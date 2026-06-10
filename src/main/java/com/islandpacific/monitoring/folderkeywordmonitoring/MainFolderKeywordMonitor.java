package com.islandpacific.monitoring.folderkeywordmonitoring;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainFolderKeywordMonitor {

    private static final Logger LOGGER = Logger.getLogger(MainFolderKeywordMonitor.class.getName());
    private static final String DEFAULT_CONFIG_FILE = "folderlogkeywordmonitor.properties";

    public static void main(String[] args) {
        printBanner();

        String configFile = args.length > 0 ? args[0] : DEFAULT_CONFIG_FILE;

        try {
            Properties configProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                configProps.load(fis);
            }

            String emailConfigPath = configProps.getProperty("email.config.path", "email.properties");
            Properties emailProps = new Properties();
            try (FileInputStream fis = new FileInputStream(emailConfigPath)) {
                emailProps.load(fis);
            }

            String logLevel = configProps.getProperty("log.level", emailProps.getProperty("log.level", "INFO"));
            String logFolder = configProps.getProperty("log.folder", emailProps.getProperty("log.folder", "logs"));
            com.islandpacific.monitoring.common.AppLogger.setupLogger("folderkeywordmonitoring", logLevel, logFolder);

            int retentionDays = Integer.parseInt(configProps.getProperty("log.retention.days",
                    emailProps.getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(configProps.getProperty("log.purge.interval.hours",
                    emailProps.getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            FolderKeywordMonitorConfig config = new FolderKeywordMonitorConfig(configProps);
            LOGGER.info("Monitoring folder: " + config.getFolderPath());
            LOGGER.info("Keywords: " + config.getKeywords());
            LOGGER.info("Check interval: " + config.getCheckIntervalMinutes() + " minutes");

            ConcurrentHashMap<String, Long> totalFilesMatched = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Long> totalFilesScanned = new ConcurrentHashMap<>();

            FolderKeywordMonitorMetrics metrics = new FolderKeywordMonitorMetrics(
                    LOGGER, totalFilesMatched, totalFilesScanned);
            FolderKeywordMonitorServer server = new FolderKeywordMonitorServer(
                    LOGGER, config.getMetricsPort(), metrics);
            server.start();

            EmailService emailService = new EmailService(emailProps, config.getClientName(), LOGGER);
            FolderKeywordMonitorService service = new FolderKeywordMonitorService(
                    LOGGER, config, emailService, totalFilesMatched, totalFilesScanned);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    service.checkAndAlert();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during scheduled check: " + e.getMessage(), e);
                }
            }, 0, config.getCheckIntervalMinutes(), TimeUnit.MINUTES);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down FolderKeywordMonitor...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) scheduler.shutdownNow();
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                server.stop();
            }));

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
        System.out.println("       Folder Log Keyword Monitor                            ");
        System.out.println("       Monitors folders recursively for keywords             ");
        System.out.println("==============================================================");
        System.out.println();
    }
}
