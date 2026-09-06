package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainSubsystemMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String emailPropertiesFilePath = "email.properties";
    private static String appPropertiesFilePath = "ibmsubsystemmonitor.properties";

    private static Properties emailProps = new Properties();
    private static Properties appProps = new Properties();

    private static SubsystemMonitorConfig monitorConfig;
    private static IbmiSubsystemService ibmiSubsystemService;
    private static EmailService emailService;
    private static SubsystemMetricsExporter metricsExporter;

    private static final Map<String, String> lastSubsystemStatus = new ConcurrentHashMap<>();
    private static final Map<String, String> lastErrorMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length >= 1) emailPropertiesFilePath = args[0];
        if (args.length >= 2) appPropertiesFilePath = args[1];

        try {
            loadProperties();

            String logLevel = emailProps.getProperty("log.level", "INFO");
            String logFolder = emailProps.getProperty("log.folder", "logs");
            com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmsubsystemmonitoring", logLevel, logFolder);

            int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            ibmiSubsystemService = new IbmiSubsystemService(
                    monitorConfig.getIbmiHost(),
                    monitorConfig.getIbmiUser(),
                    monitorConfig.getIbmiPassword());

            String logoPath = appProps.getProperty("logo.path", "");
            emailService = new EmailService(emailProps, monitorConfig.getIbmiHost(), monitorConfig.getClientName(), logoPath);

            metricsExporter = new SubsystemMetricsExporter(monitorConfig.getMetricsPort());
            metricsExporter.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            logger.info("Starting IBM i Subsystem monitoring service. Checking every " +
                    monitorConfig.getMonitorIntervalMs() / 1000 + " seconds.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    checkSubsystemsAndAlert();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected monitor error: " + e.getMessage(), e);
                }
            }, 0, monitorConfig.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down subsystem monitor...");
                scheduler.shutdown();
                metricsExporter.stop();
                com.islandpacific.monitoring.common.AppLogger.closeLogger();
            }));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Startup failure: " + e.getMessage(), e);
            System.exit(1);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void loadProperties() throws IOException {
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailProps.load(in);
        }
        try (InputStream in = new FileInputStream(appPropertiesFilePath)) {
            appProps.load(in);
        }
        monitorConfig = SubsystemMonitorConfig.fromProperties(appProps, emailProps);
    }

    private static void checkSubsystemsAndAlert() throws Exception {
        List<SubsystemInfo> retrievedSubsystems = new ArrayList<>();
        String globalLibrary = monitorConfig.getIbmiSystemContextLibrary();

        for (String description : monitorConfig.getCriticalSubsystemNames()) {
            String key = globalLibrary + "/" + description;

            try {
                SubsystemInfo info = ibmiSubsystemService.getSubsystemInfo(description, globalLibrary);

                if (info != null) {
                    retrievedSubsystems.add(info);
                    String currentStatus = info.getStatus();
                    String previousStatus = lastSubsystemStatus.get(key);

                    if (previousStatus == null || !previousStatus.equalsIgnoreCase(currentStatus)) {
                        logger.warning("Subsystem " + key + " changed: " + previousStatus + " → " + currentStatus);
                        if (!"ACTIVE".equalsIgnoreCase(currentStatus)) {
                            emailService.sendSubsystemStatusAlert(info);
                        }
                        lastSubsystemStatus.put(key, currentStatus);
                        lastErrorMap.remove(key);
                    } else {
                        logger.info("Suppressed duplicate status alert for " + key + " (still " + currentStatus + ")");
                    }
                } else {
                    String previous = lastSubsystemStatus.get(key);
                    if (previous == null || !"NOT FOUND".equalsIgnoreCase(previous)) {
                        logger.warning("Subsystem " + key + " NOT FOUND.");
                        lastSubsystemStatus.put(key, "NOT FOUND");
                    } else {
                        logger.info("Suppressed duplicate NOT FOUND alert for " + key);
                    }
                }

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                String lastError = lastErrorMap.get(key);
                if (lastError == null || !lastError.equalsIgnoreCase(errorMessage)) {
                    logger.severe("NEW error for " + key + ": " + errorMessage);
                    lastErrorMap.put(key, errorMessage);
                } else {
                    logger.info("Suppressed duplicate error for " + key);
                }
            }
        }

        metricsExporter.updateMetrics(retrievedSubsystems);
    }
}
