package com.islandpacific.monitoring.apiurlmonitoring;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApiUrlMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String emailPropertiesFilePath = "email.properties";
    private static String monitorPropertiesFilePath = "apiurlmonitor.properties";

    private static final int DEFAULT_METRICS_PORT = 3026;
    private static final int DEFAULT_INTERVAL_SECONDS = 60;

    public static void main(String[] args) {
        if (args.length >= 1) emailPropertiesFilePath = args[0];
        if (args.length >= 2) monitorPropertiesFilePath = args[1];

        try {
            setupLogger();

            ApiUrlMonitorConfig config = new ApiUrlMonitorConfig(emailPropertiesFilePath, monitorPropertiesFilePath);

            int metricsPort = Integer.parseInt(
                    config.getMonitorProps().getProperty("metrics.port", String.valueOf(DEFAULT_METRICS_PORT)));
            int intervalSeconds = Integer.parseInt(
                    config.getMonitorProps().getProperty("monitor.interval.seconds", String.valueOf(DEFAULT_INTERVAL_SECONDS)));

            int retentionDays = Integer.parseInt(config.getMonitorProps().getProperty("log.retention.days",
                    config.getEmailProps().getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(config.getMonitorProps().getProperty("log.purge.interval.hours",
                    config.getEmailProps().getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            String logoPath = config.getMonitorProps().getProperty("logo.path",
                    config.getEmailProps().getProperty("logo.path", ""));
            EmailService emailService = new EmailService(config.getEmailProps(), config.getClientName(), logoPath);
            ApiUrlMonitorMetrics metrics = new ApiUrlMonitorMetrics();
            ApiUrlMonitorService monitorService = new ApiUrlMonitorService(emailService, metrics);

            ApiUrlMonitorAppServer appServer = new ApiUrlMonitorAppServer(metricsPort);
            appServer.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            });

            logger.info("API URL Monitor starting. Checking " + config.getUrlConfigs().size()
                    + " URL(s) every " + intervalSeconds + " seconds.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    metrics.updateUptime();
                    monitorService.checkAll(config);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during API URL check cycle: " + e.getMessage(), e);
                }
            }, 0, intervalSeconds, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down API URL Monitor...");
                scheduler.shutdown();
                appServer.stop();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                }
                logger.info("API URL Monitor stopped.");
            }));

        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            e.printStackTrace();
            logger.log(Level.SEVERE, "Configuration error: " + e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Failed to start API URL Monitor: " + e.getMessage());
            e.printStackTrace();
            logger.log(Level.SEVERE, "Failed to start API URL Monitor: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setupLogger() {
        String logLevel = "INFO";
        String logFolder = "logs";
        try (FileInputStream in = new FileInputStream(monitorPropertiesFilePath)) {
            Properties p = new Properties();
            p.load(in);
            logLevel = p.getProperty("log.level", "INFO");
            logFolder = p.getProperty("log.folder", "logs");
        } catch (Exception ignored) {
        }
        com.islandpacific.monitoring.common.AppLogger.setupLogger("apiurlmonitoring", logLevel, logFolder);
    }
}
