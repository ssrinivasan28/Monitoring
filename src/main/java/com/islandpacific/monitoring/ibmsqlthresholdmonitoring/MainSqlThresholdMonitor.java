package com.islandpacific.monitoring.ibmsqlthresholdmonitoring;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for IBM SQL Threshold Monitor.
 * Runs configured SQL row-count checks against IBM i DB2 via JDBC and sends email alerts on threshold breach.
 */
public class MainSqlThresholdMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String emailPropertiesFilePath = "email.properties";
    private static String monitorPropertiesFilePath = "sqlthresholdmonitor.properties";

    private static final int DEFAULT_METRICS_PORT = 3025;
    private static final int DEFAULT_MONITOR_INTERVAL_MINUTES = 5;

    public static void main(String[] args) {
        if (args.length >= 1) {
            emailPropertiesFilePath = args[0];
            if (args.length >= 2) {
                monitorPropertiesFilePath = args[1];
            }
        }

        try {
            setupLogger();

            SqlThresholdConfig config = new SqlThresholdConfig(emailPropertiesFilePath, monitorPropertiesFilePath, logger);
            int monitorIntervalMinutes = Integer.parseInt(config.getMonitorProps().getProperty("monitor.interval.minutes", String.valueOf(DEFAULT_MONITOR_INTERVAL_MINUTES)));
            int metricsPort = Integer.parseInt(config.getMonitorProps().getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));

            int retentionDays = Integer.parseInt(config.getMonitorProps().getProperty("log.retention.days",
                    config.getEmailProps().getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(config.getMonitorProps().getProperty("log.purge.interval.hours",
                    config.getEmailProps().getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            String logoPath = config.getMonitorProps().getProperty("logo.path", "");
            EmailService emailService = new EmailService(config.getEmailProps(), config.getClientName(), logoPath);

            SqlThresholdService monitorService = new SqlThresholdService(
                    logger, emailService, config.getIbmiHost(), config.getIbmiUser(), config.getIbmiPassword(), config.getClientName()
            );

            SqlThresholdAppServer metricsServer = new SqlThresholdAppServer(logger, metricsPort);
            metricsServer.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            logger.info("Starting IBM SQL Threshold monitoring service. Checking every " + monitorIntervalMinutes + " minutes.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    logger.info("Starting new SQL threshold scan...");
                    SqlThresholdMetrics.updateUptime();

                    for (SqlThresholdConfig.SqlCheckConfig check : config.getSqlChecks()) {
                        monitorService.runCheck(check);
                    }
                    SqlThresholdMetrics.setLastScanTimestamp(System.currentTimeMillis() / 1000);
                    logger.info("IBM SQL Threshold scan completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during SQL threshold monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, monitorIntervalMinutes, TimeUnit.MINUTES);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down IBM SQL Threshold monitor gracefully...");
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
                    logger.info("IBM SQL Threshold monitor shutdown complete.");
                }
            }));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Application failed to start due to I/O error: " + e.getMessage(), e);
            System.err.println("Application failed to start due to I/O error: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Application configuration error: " + e.getMessage(), e);
            System.err.println("Application configuration error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred during application startup: " + e.getMessage(), e);
            System.err.println("An unexpected error occurred during application startup: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void setupLogger() throws IOException {
        String logLevel = "INFO";
        String logFolder = "logs";
        try (java.io.InputStream in = new java.io.FileInputStream(monitorPropertiesFilePath)) {
            Properties p = new Properties();
            p.load(in);
            logLevel = p.getProperty("log.level", "INFO");
            logFolder = p.getProperty("log.folder", "logs");
        } catch (Exception e) {
            // Use defaults if properties file not readable yet
        }
        com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmsqlthresholdmonitoring", logLevel, logFolder);
    }
}
