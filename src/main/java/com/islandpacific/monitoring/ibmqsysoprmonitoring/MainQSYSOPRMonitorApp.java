package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import com.islandpacific.monitoring.common.AppLogger;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainQSYSOPRMonitorApp {

    private static final Logger LOGGER = AppLogger.getLogger();

    public static void main(String[] args) {
        String emailPropertiesFile = args.length >= 1 ? args[0] : "email.properties";
        String jobFailurePropertiesFile = args.length >= 2 ? args[1] : "ibmqsysoprmonitor.properties";

        QSYSOPRMonitorConfig config = new QSYSOPRMonitorConfig(emailPropertiesFile, jobFailurePropertiesFile);
        if (!config.loadConfigurations()) {
            System.err.println("Failed to load configurations. Application will exit.");
            return;
        }

        Properties mailProps = config.getMailProperties();
        String logLevel = mailProps.getProperty("log.level", "INFO");
        String logFolder = mailProps.getProperty("log.folder", "logs");
        AppLogger.setupLogger("ibmqsysoprmonitoring", logLevel, logFolder);

        int retentionDays = Integer.parseInt(mailProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(mailProps.getProperty("log.purge.interval.hours", "24"));
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        LOGGER.info("Starting QSYSOPR Monitor Application...");

        QSYSOPRMonitorMetrics.initializeMetrics();

        String logoPath = config.getMonitorProperties().getProperty("logo.path", "");
        EmailService emailService = new EmailService(mailProps, logoPath);
        QSYSOPRMonitorService monitorService = new QSYSOPRMonitorService(config, emailService);

        QSYSOPRMetricsServer metricsServer = new QSYSOPRMetricsServer(config.getMetricsPort());
        metricsServer.startServer();
        LOGGER.info("Prometheus Metrics Server started on port " + config.getMetricsPort());

        final String[] lastCheckedTimestamp = { monitorService.loadLastCheckedTimestamp() };
        if (lastCheckedTimestamp[0] != null) {
            LOGGER.info("Resuming from last checked timestamp: " + lastCheckedTimestamp[0]);
        } else {
            LOGGER.info("No previous state found. Will process all current messages and save state.");
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                String newTimestamp = monitorService.scanAndAlert(lastCheckedTimestamp[0]);
                if (newTimestamp != null) {
                    lastCheckedTimestamp[0] = newTimestamp;
                    monitorService.saveLastCheckedTimestamp(lastCheckedTimestamp[0]);
                }
                LOGGER.info("Next scan in " + (config.getMonitorIntervalMillis() / 1000) + " seconds...");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during monitoring cycle: " + e.getMessage(), e);
            }
        }, 0, config.getMonitorIntervalMillis(), TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down QSYSOPR Monitor...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            } finally {
                metricsServer.stopServer();
                QSYSOPRMonitorMetrics.setMonitorStopped();
                AppLogger.closeLogger();
                LOGGER.info("QSYSOPR Monitor shutdown complete.");
            }
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
