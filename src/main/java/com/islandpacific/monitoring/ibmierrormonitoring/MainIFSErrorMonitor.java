package com.islandpacific.monitoring.ibmierrormonitoring;

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


public class MainIFSErrorMonitor {

    private static final Logger logger = Logger.getLogger(MainIFSErrorMonitor.class.getName());

    private static String emailPropertiesFilePath = "email.properties";
    private static String monitorPropertiesFilePath = "monitor.properties";
    private static final String LAST_RUN_LOG = "logs/last_run_timestamp.txt";

    private static int monitorInterval;
    private static int metricsPort;

    public static void main(String[] args) {
        if (args.length >= 1) {
            emailPropertiesFilePath = args[0];
            if (args.length >= 2) {
                monitorPropertiesFilePath = args[1];
            }
        }

        try {
            setupLogger();

            // Initialize configuration
            IFSErrorMonitorConfig config = new IFSErrorMonitorConfig(emailPropertiesFilePath, monitorPropertiesFilePath, logger);
            monitorInterval = Integer.parseInt(config.getMonitorProps().getProperty("monitor.interval.ms", "30000"));
            metricsPort = Integer.parseInt(config.getMonitorProps().getProperty("metrics.port", "8080"));
            
            // Start automatic log purge
            int retentionDays = Integer.parseInt(config.getMonitorProps().getProperty("log.retention.days",
                config.getEmailProps().getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(config.getMonitorProps().getProperty("log.purge.interval.hours",
                config.getEmailProps().getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            // Initialize services
            // Pass the client name to EmailService
            EmailService emailService = new EmailService(config.getEmailProps(), config.getClientName(), logger); // Modified line
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts = new ConcurrentHashMap<>();
            IFSErrorMonitorMetrics metricsService = new IFSErrorMonitorMetrics(logger, totalFileCounts, newFileCounts);
            IFSErrorMonitorService monitorService = new IFSErrorMonitorService(
                logger, config.getMonitoringConfigs(), config.getGlobalSmbCredentials(),
                emailService, totalFileCounts, newFileCounts, metricsService
            );
            IFSErrorMonitorsServer metricsServer = new IFSErrorMonitorsServer(logger, metricsPort, metricsService);

            metricsServer.start();

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

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    logger.info("Starting new scan for files across all configured monitoring locations...");
                    monitorService.checkNewFilesAndSendEmail();
                    logger.info("Scan completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during file monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, monitorInterval, TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down file monitor gracefully...");
                scheduler.shutdown();
                metricsServer.stop();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
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
                    logger.info("File monitor shutdown complete.");
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

    private static String logLevel = "INFO"; // Default
    private static String logFolder = "logs"; // Default
    
    private static void setupLogger() throws IOException {
        // Use standardized AppLogger with values set in main()
        com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmierrormonitoring", logLevel, logFolder);
    }
}