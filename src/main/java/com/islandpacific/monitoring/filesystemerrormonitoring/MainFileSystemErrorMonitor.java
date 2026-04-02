package com.islandpacific.monitoring.filesystemerrormonitoring;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main entry point for Windows File System Error Monitor.
 * Monitors configured Windows folders for new error files and sends email notifications.
 */
public class MainFileSystemErrorMonitor {
    
    private static final Logger LOGGER = Logger.getLogger(MainFileSystemErrorMonitor.class.getName());
    private static final int DEFAULT_METRICS_PORT = 8085;
    private static final String DEFAULT_CONFIG_FILE = "fserrormonitor.properties";
    
    private final Properties configProps;
    private final Properties emailProps;
    private final FileSystemErrorConfig monitorConfig;
    private final String clientName;
    private final int checkIntervalMinutes;
    private final int metricsPort;
    
    private ScheduledExecutorService scheduler;
    private FileSystemErrorMonitorServer metricsServer;
    
    public MainFileSystemErrorMonitor(String configFilePath) throws IOException {
        // Load main configuration
        configProps = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            configProps.load(fis);
        }
        
        // Load email configuration from separate file
        String emailConfigPath = configProps.getProperty("email.config.path", "email.properties");
        emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream(emailConfigPath)) {
            emailProps.load(fis);
        }
        
        // Parse configuration
        monitorConfig = new FileSystemErrorConfig(configProps);
        
        // Get general settings
        clientName = configProps.getProperty("client.name", "Windows FS Error Monitor");
        
        // Support both check.interval.minutes and monitor.interval.ms for backward compatibility
        String intervalMinutes = configProps.getProperty("check.interval.minutes");
        if (intervalMinutes != null) {
            checkIntervalMinutes = Integer.parseInt(intervalMinutes);
        } else {
            // Fallback to monitor.interval.ms (convert to minutes)
            int intervalMs = Integer.parseInt(configProps.getProperty("monitor.interval.ms", "300000"));
            checkIntervalMinutes = Math.max(1, intervalMs / 60000);
        }
        metricsPort = Integer.parseInt(configProps.getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));
        
        // Setup logging
        setupLogging();
    }
    
    private void setupLogging() {
        try {
            String logDir = configProps.getProperty("log.directory", "logs");
            Files.createDirectories(Paths.get(logDir));
            
            String logFileName = String.format("%s/fserrormonitor_%s.log",
                logDir,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            FileHandler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.INFO);
            
            LOGGER.info("Logging initialized. Log file: " + logFileName);
        } catch (IOException e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }
    
    /**
     * Starts the monitor service.
     */
    public void start() {
        LOGGER.info("Starting Windows File System Error Monitor...");
        LOGGER.info("Client: " + clientName);
        LOGGER.info("Check interval: " + checkIntervalMinutes + " minutes");
        LOGGER.info("Metrics port: " + metricsPort);
        
        List<FileSystemErrorConfig.MonitoringConfig> locations = monitorConfig.getMonitoringConfigs();
        LOGGER.info("Monitoring " + locations.size() + " location(s):");
        for (FileSystemErrorConfig.MonitoringConfig loc : locations) {
            LOGGER.info("  - " + loc.getName() + ": " + loc.getPathString());
        }
        
        // Create shared data structures for metrics
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts = new ConcurrentHashMap<>();
        
        // Start metrics server
        FileSystemErrorMetrics metrics = new FileSystemErrorMetrics(LOGGER, totalFileCounts, newFileCounts);
        try {
            metricsServer = new FileSystemErrorMonitorServer(LOGGER, metricsPort, metrics);
            metricsServer.start();
            LOGGER.info("Metrics server started on port " + metricsPort);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start metrics server: " + e.getMessage(), e);
        }
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(configProps.getProperty("log.retention.days",
            emailProps.getProperty("log.retention.days", "30")));
        int purgeIntervalHours = Integer.parseInt(configProps.getProperty("log.purge.interval.hours",
            emailProps.getProperty("log.purge.interval.hours", "24")));
        com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        LOGGER.info("Log purge scheduled: retaining " + retentionDays + " days, checking every " + purgeIntervalHours + " hours");
        
        // Create email service
        EmailService emailService = new EmailService(emailProps, clientName, LOGGER);
        FileSystemErrorService monitorService = new FileSystemErrorService(
            LOGGER, 
            monitorConfig.getMonitoringConfigs(), 
            emailService, 
            totalFileCounts, 
            newFileCounts, 
            metrics
        );
        
        // Schedule monitoring task
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOGGER.fine("Running scheduled error file check...");
                monitorService.checkNewFilesAndSendEmail();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during scheduled check: " + e.getMessage(), e);
            }
        }, 0, checkIntervalMinutes, TimeUnit.MINUTES);
        
        LOGGER.info("Monitor service started. Running initial check...");
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Windows File System Error Monitor...");
            stop();
        }));
    }
    
    /**
     * Stops the monitor service.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Scheduler stopped.");
        }
        
        if (metricsServer != null) {
            metricsServer.stop();
            LOGGER.info("Metrics server stopped.");
        }
    }
    
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        printBanner();
        
        String configFile = DEFAULT_CONFIG_FILE;
        if (args.length > 0) {
            configFile = args[0];
        }
        
        try {
            // Load properties first to get logger configuration
            Properties tempProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                tempProps.load(fis);
            }
            
            String emailConfigPath = tempProps.getProperty("email.config.path", "email.properties");
            Properties emailProps = new Properties();
            try (FileInputStream fis = new FileInputStream(emailConfigPath)) {
                emailProps.load(fis);
            }
            
            // Initialize AppLogger before any other operations
            String logLevel = tempProps.getProperty("log.level", emailProps.getProperty("log.level", "INFO"));
            String logFolder = tempProps.getProperty("log.folder", emailProps.getProperty("log.folder", "logs"));
            com.islandpacific.monitoring.common.AppLogger.setupLogger("filesystemerrormonitoring", logLevel, logFolder);
            
            LOGGER.info("Using configuration file: " + configFile);
            
            MainFileSystemErrorMonitor monitor = new MainFileSystemErrorMonitor(configFile);
            monitor.start();
            
            // Keep main thread alive
            Thread.currentThread().join();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start monitor: " + e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            LOGGER.info("Monitor interrupted.");
            Thread.currentThread().interrupt();
        }
    }
    
    private static void printBanner() {
        System.out.println("==============================================================");
        System.out.println("       Windows File System Error Monitor                      ");
        System.out.println("       Monitoring for Error Files                             ");
        System.out.println("==============================================================");
        System.out.println();
    }
}
