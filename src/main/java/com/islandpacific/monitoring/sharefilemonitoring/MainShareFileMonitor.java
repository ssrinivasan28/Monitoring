package com.islandpacific.monitoring.sharefilemonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainShareFileMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String emailPropsPath = "email.properties";
    private static String monitorPropsPath = "sharefilemonitor.properties";

    private static final int DEFAULT_PORT = 3026;
    private static final int DEFAULT_INTERVAL_MINUTES = 5;

    public static void main(String[] args) {
        if (args.length >= 1) emailPropsPath = args[0];
        if (args.length >= 2) monitorPropsPath = args[1];

        try {
            setupLogger();

            ShareFileMonitorConfig config = new ShareFileMonitorConfig(emailPropsPath, monitorPropsPath, logger);

            int port = Integer.parseInt(config.getMonitorProps().getProperty("metrics.port", String.valueOf(DEFAULT_PORT)));
            int intervalMinutes = Integer.parseInt(config.getMonitorProps().getProperty("monitor.interval.minutes", String.valueOf(DEFAULT_INTERVAL_MINUTES)));

            int retentionDays = Integer.parseInt(config.getMonitorProps().getProperty("log.retention.days",
                    config.getEmailProps().getProperty("log.retention.days", "30")));
            int purgeHours = Integer.parseInt(config.getMonitorProps().getProperty("log.purge.interval.hours",
                    config.getEmailProps().getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeHours);

            String ftpHost = config.getMonitorProps().getProperty("ftp.host", "islandpacific.sharefileftp.com");
            String ftpUser = config.getMonitorProps().getProperty("ftp.username");
            String ftpPass = config.getMonitorProps().getProperty("ftp.password");

            if (ftpUser == null || ftpPass == null) {
                throw new IllegalArgumentException("ftp.username and ftp.password are required in " + monitorPropsPath);
            }

            int alertWindowSize = Integer.parseInt(config.getMonitorProps().getProperty("alert.window.size", "3"));

            EmailService emailService = new EmailService(config.getEmailProps(), config.getClientName());
            ShareFileMonitorService monitorService = new ShareFileMonitorService(logger, emailService, ftpHost, ftpUser, ftpPass, alertWindowSize);
            ShareFileAppServer appServer = new ShareFileAppServer(logger, port);
            appServer.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sharefile-monitor");
                t.setDaemon(true);
                return t;
            });

            logger.info("ShareFile monitor starting — interval=" + intervalMinutes + "m, port=" + port);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    ShareFileMetrics.updateUptime();
                    for (ShareFileMonitorConfig.FolderConfig fc : config.getFolderConfigs()) {
                        monitorService.monitorFolder(fc);
                    }
                    ShareFileMetrics.setLastScanTimestamp(System.currentTimeMillis() / 1000);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during ShareFile monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, intervalMinutes, TimeUnit.MINUTES);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down ShareFile monitor...");
                scheduler.shutdown();
                appServer.stop();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) scheduler.shutdownNow();
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                }
                logger.info("ShareFile monitor shutdown complete.");
            }));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Startup I/O error: " + e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Configuration error: " + e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected startup error: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setupLogger() throws IOException {
        String logLevel = "INFO";
        String logFolder = "logs";
        try (FileInputStream in = new FileInputStream(monitorPropsPath)) {
            Properties p = new Properties();
            p.load(in);
            logLevel = p.getProperty("log.level", "INFO");
            logFolder = p.getProperty("log.folder", "logs");
        } catch (Exception ignored) {}
        com.islandpacific.monitoring.common.AppLogger.setupLogger("sharefilemonitoring", logLevel, logFolder);
    }
}
