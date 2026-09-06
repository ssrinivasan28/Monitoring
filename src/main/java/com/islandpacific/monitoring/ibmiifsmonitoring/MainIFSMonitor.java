package com.islandpacific.monitoring.ibmiifsmonitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainIFSMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    // Default paths for configuration files
    private static String emailPropertiesFilePath = "email.properties";
    private static String monitorPropertiesFilePath = "ibmifsmonitor.properties";

    // Constants
    private static final String LAST_RUN_LOG = "logs/last_run_timestamp.txt";
    private static final int DEFAULT_METRICS_PORT = 8080;
    private static final int DEFAULT_MONITOR_INTERVAL_MS = 300000;

    // Application settings loaded from properties
    private static int monitorIntervalMs;
    private static int metricsPort;


    public static void main(String[] args) {
        // Allow command-line arguments to override default property file paths
        if (args.length >= 1) {
            emailPropertiesFilePath = args[0];
            if (args.length >= 2) {
                monitorPropertiesFilePath = args[1];
            }
        }

        try {
            // Initialize configuration first so log settings come from properties
            IFSMonitorConfig config = new IFSMonitorConfig(emailPropertiesFilePath, monitorPropertiesFilePath, logger);
            String logLevel = config.getEmailProps().getProperty("log.level", "INFO");
            String logFolder = config.getEmailProps().getProperty("log.folder", "logs");
            com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmiifsmonitoring", logLevel, logFolder);

            monitorIntervalMs = Integer.parseInt(config.getMonitorProps().getProperty("monitor.interval.ms", String.valueOf(DEFAULT_MONITOR_INTERVAL_MS)));
            metricsPort = Integer.parseInt(config.getMonitorProps().getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));

            // Start automatic log purge
            int retentionDays = Integer.parseInt(config.getEmailProps().getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(config.getEmailProps().getProperty("log.purge.interval.hours", "24"));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            // Initialize core services
            String logoPath = config.getMonitorProps().getProperty("logo.path", "");
            EmailService emailService = new EmailService(config.getEmailProps(), logoPath);
            
            // These maps are shared between MonitorService and MetricsService
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts = new ConcurrentHashMap<>();

            IFSMonitorMetrics metricsService = new IFSMonitorMetrics(logger, totalFileCounts, newFileCounts);
            IFSMonitorService monitorService = new IFSMonitorService( 
                logger, emailService, totalFileCounts, newFileCounts, config.getGlobalSmbCredentials()
            );
            IFSMonitorAppServer metricsServer = new IFSMonitorAppServer(logger, metricsPort);

            metricsServer.start(); // Start the metrics HTTP server

            // Load last run timestamp for initial metric update
            Path lastRunLogPath = Paths.get(LAST_RUN_LOG);
            if (Files.exists(lastRunLogPath)) {
                try {
                    String timestampStr = Files.readString(lastRunLogPath).trim();
                    if (!timestampStr.isEmpty()) {
                        long lastRun = Long.parseLong(timestampStr);
                        logger.info("Previous scan completed at: " + Instant.ofEpochSecond(lastRun));
                        metricsService.setLastScanTimestamp(lastRun); // Set initial timestamp for metrics
                    }
                } catch (IOException | NumberFormatException e) {
                    logger.warning("Could not read or parse last run timestamp from " + LAST_RUN_LOG + ": " + e.getMessage());
                }
            } else {
                logger.info("No previous run timestamp found (first run or " + LAST_RUN_LOG + " missing).");
            }

            // Schedule the periodic folder monitoring task
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            logger.info("Starting IFS folder monitoring service. Checking every " + monitorIntervalMs + " ms.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    logger.info("Starting new scan for IFS folders...");
                    IFSMonitorMetrics.updateUptime(); // Update uptime metric at the start of each cycle

                    // Perform the file count check for each configured IFS location
                    for (IFSMonitorConfig.MonitoringConfig monitorConfig : config.getMonitorConfigs()) { // Iterate through configured locations
                        monitorService.monitorFolder(monitorConfig); // Call the method on the ifsMonitorService instance
                    }
                    IFSMonitorMetrics.updateCountMetrics(totalFileCounts, newFileCounts);
                    logger.info("IFS folder scan completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during IFS folder monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, monitorIntervalMs, TimeUnit.MILLISECONDS);

            // Add a shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down IFS folder monitor gracefully...");
                scheduler.shutdown(); // Initiate shutdown of the scheduler
                metricsServer.stop(); // Stop the HTTP server
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate in time, forcing shutdown.");
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                        logger.warning("Shutdown interrupted.");
                        scheduler.shutdownNow();
                } finally {
                    logger.info("IFS folder monitor shutdown complete.");
                }
            }));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Application failed to start due to I/O error: " + e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Application configuration error: " + e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred during application startup: " + e.getMessage(), e);
            System.exit(1);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
