package com.islandpacific.monitoring.logkeywordmonitoring;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for Log Keyword Monitor.
 * Monitors configured log files for specific keywords and sends email
 * notifications.
 */
public class MainLogKeywordMonitor {

    private static final Logger LOGGER = Logger.getLogger(MainLogKeywordMonitor.class.getName());
    private static final int DEFAULT_METRICS_PORT = 3023;
    private static final String DEFAULT_CONFIG_FILE = "logkeywordmonitor.properties";

    private final Properties configProps;
    private final Properties emailProps;
    private final LogKeywordMonitorConfig monitorConfig;
    private final String clientName;
    private final int checkIntervalMinutes;
    private final int metricsPort;

    private ScheduledExecutorService scheduler;
    private LogKeywordMonitorServer metricsServer;

    public MainLogKeywordMonitor(String configFilePath) throws IOException {
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
        monitorConfig = new LogKeywordMonitorConfig(configProps);

        // Get general settings
        clientName = configProps.getProperty("client.name", "Log Keyword Monitor");

        // Support both check.interval.minutes and monitor.interval.ms for backward
        // compatibility
        String intervalMinutes = configProps.getProperty("check.interval.minutes");
        if (intervalMinutes != null) {
            checkIntervalMinutes = Integer.parseInt(intervalMinutes);
        } else {
            // Fallback to monitor.interval.ms (convert to minutes)
            int intervalMs = Integer.parseInt(configProps.getProperty("monitor.interval.ms", "60000"));
            checkIntervalMinutes = Math.max(1, intervalMs / 60000);
        }
        metricsPort = Integer.parseInt(configProps.getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));

        // Setup logging
        setupLogging();
    }

    private void setupLogging() {
        // Logging is already configured by AppLogger.setupLogger() in main() before
        // this constructor runs — nothing to do here.
        LOGGER.info("Logging initialized via AppLogger.");
    }

    /**
     * Starts the monitor service.
     */
    public void start() {
        LOGGER.info("Starting Log Keyword Monitor...");
        LOGGER.info("Client: " + clientName);
        LOGGER.info("Check interval: " + checkIntervalMinutes + " minutes");
        LOGGER.info("Metrics port: " + metricsPort);

        List<LogKeywordMonitorConfig.LogFileConfig> logFiles = monitorConfig.getLogFileConfigs();
        LOGGER.info("Monitoring " + logFiles.size() + " log file(s):");
        for (LogKeywordMonitorConfig.LogFileConfig logFile : logFiles) {
            LOGGER.info("  - " + logFile.getPathPattern() + " (Keywords: " + String.join(", ", logFile.getKeywords())
                    + ")");
        }

        // Create shared data structures for metrics
        ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Long> linesScanned = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Long> readErrors = new ConcurrentHashMap<>();

        // Start metrics server
        LogKeywordMonitorMetrics metrics = new LogKeywordMonitorMetrics(
                LOGGER, totalMatchCounts, newMatchCounts, linesScanned, readErrors);
        try {
            metricsServer = new LogKeywordMonitorServer(LOGGER, metricsPort, metrics);
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
        LOGGER.info("Log purge scheduled: retaining " + retentionDays + " days, checking every " + purgeIntervalHours
                + " hours");

        // Create email service
        EmailService emailService = new EmailService(emailProps, clientName, LOGGER);
        LogKeywordMonitorService monitorService = new LogKeywordMonitorService(
                LOGGER,
                monitorConfig.getLogFileConfigs(),
                emailService,
                totalMatchCounts,
                newMatchCounts,
                linesScanned,
                readErrors,
                metrics);

        // Schedule monitoring task
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOGGER.fine("Running scheduled log keyword check...");
                monitorService.checkLogsAndSendAlerts();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during scheduled check: " + e.getMessage(), e);
            }
        }, 0, checkIntervalMinutes, TimeUnit.MINUTES);

        LOGGER.info("Monitor service started. Running initial check...");

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Log Keyword Monitor...");
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
            com.islandpacific.monitoring.common.AppLogger.setupLogger("logkeywordmonitoring", logLevel, logFolder);

            LOGGER.info("Using configuration file: " + configFile);

            MainLogKeywordMonitor monitor = new MainLogKeywordMonitor(configFile);
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
        System.out.println("       Log Keyword Monitor                                   ");
        System.out.println("       Monitoring Log Files for Keywords                     ");
        System.out.println("==============================================================");
        System.out.println();
    }
}
