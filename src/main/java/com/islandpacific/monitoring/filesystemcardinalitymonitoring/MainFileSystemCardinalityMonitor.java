package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for Windows File System Cardinality Monitor.
 * Monitors Windows folders for file count thresholds and sends email alerts.
 */
public class MainFileSystemCardinalityMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    // Default paths for configuration files
    private static String emailPropertiesFilePath = "email.properties";
    private static String monitorPropertiesFilePath = "fscardinalitymonitor.properties";

    // Constants
    private static final String LAST_RUN_LOG = "logs/fs_cardinality_last_run_timestamp.txt";
    private static final int DEFAULT_METRICS_PORT = 8084;
    private static final int DEFAULT_MONITOR_INTERVAL_MINUTES = 5;

    // Application settings loaded from properties
    private static int monitorIntervalMinutes;
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
            setupLogger();

            // Initialize configuration (uses separate email.properties and monitor properties files)
            FileSystemCardinalityConfig config = new FileSystemCardinalityConfig(emailPropertiesFilePath, monitorPropertiesFilePath, logger);
            monitorIntervalMinutes = Integer.parseInt(config.getMonitorProps().getProperty("monitor.interval.minutes", String.valueOf(DEFAULT_MONITOR_INTERVAL_MINUTES)));
            metricsPort = Integer.parseInt(config.getMonitorProps().getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));
            
            // Start automatic log purge
            int retentionDays = Integer.parseInt(config.getMonitorProps().getProperty("log.retention.days",
                config.getEmailProps().getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(config.getMonitorProps().getProperty("log.purge.interval.hours",
                config.getEmailProps().getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            // Initialize core services
            EmailService emailService = new EmailService(config.getEmailProps(), config.getClientName());
            
            // These maps are shared between MonitorService and MetricsService
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts = new ConcurrentHashMap<>();

            FileSystemCardinalityMetrics metricsService = new FileSystemCardinalityMetrics(logger, totalFileCounts, newFileCounts);
            FileSystemCardinalityService monitorService = new FileSystemCardinalityService(
                logger, emailService, totalFileCounts, newFileCounts
            );
            FileSystemCardinalityAppServer metricsServer = new FileSystemCardinalityAppServer(logger, metricsPort);

            metricsServer.start();

            // Load last run timestamp for initial metric update
            Path lastRunLogPath = Paths.get(LAST_RUN_LOG);
            if (Files.exists(lastRunLogPath)) {
                try {
                    String timestampStr = Files.readString(lastRunLogPath).trim();
                    if (!timestampStr.isEmpty()) {
                        long lastRun = Long.parseLong(timestampStr);
                        logger.info("Previous scan completed at: " + Instant.ofEpochSecond(lastRun));
                        metricsService.setLastScanTimestamp(lastRun);
                    }
                } catch (IOException | NumberFormatException e) {
                    logger.warning("Could not read or parse last run timestamp from " + LAST_RUN_LOG + ": " + e.getMessage());
                }
            } else {
                logger.info("No previous run timestamp found (first run or " + LAST_RUN_LOG + " missing).");
            }

            // Schedule the periodic folder monitoring task
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            logger.info("Starting Windows File System Cardinality monitoring service. Checking every " + monitorIntervalMinutes + " minutes.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    logger.info("Starting new scan for Windows folders...");
                    FileSystemCardinalityMetrics.updateUptime();

                    // Perform the file count check for each configured location
                    for (FileSystemCardinalityConfig.MonitoringConfig monitorConfig : config.getMonitorConfigs()) {
                        monitorService.monitorFolder(monitorConfig);
                    }
                    logger.info("Windows folder scan completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during Windows folder monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, monitorIntervalMinutes, TimeUnit.MINUTES);

            // Add a shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Windows File System Cardinality monitor gracefully...");
                scheduler.shutdown();
                metricsServer.stop();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate in time, forcing shutdown.");
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.warning("Shutdown interrupted.");
                    scheduler.shutdownNow();
                } finally {
                    for (Logger locLogger : config.getLocationLoggers().values()) {
                        for (Handler handler : locLogger.getHandlers()) {
                            if (handler instanceof FileHandler) {
                                handler.close();
                            }
                        }
                    }
                    logger.info("Windows File System Cardinality monitor shutdown complete.");
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
    }

    /**
     * Sets up the main application logger.
     */
    private static void setupLogger() throws IOException {
        String logLevel = "INFO";
        String logFolder = "logs";
        try {
            FileSystemCardinalityConfig tempConfig = new FileSystemCardinalityConfig(emailPropertiesFilePath, monitorPropertiesFilePath, logger);
            logLevel = tempConfig.getMonitorProps().getProperty("log.level", "INFO");
            logFolder = tempConfig.getMonitorProps().getProperty("log.folder", "logs");
        } catch (Exception e) {
            // Use defaults if config not available yet
        }
        com.islandpacific.monitoring.common.AppLogger.setupLogger("filesystemcardinalitymonitoring", logLevel, logFolder);
    }
}
