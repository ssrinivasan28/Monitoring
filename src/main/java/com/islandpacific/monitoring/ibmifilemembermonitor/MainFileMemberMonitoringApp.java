package com.islandpacific.monitoring.ibmifilemembermonitor;

import com.islandpacific.monitoring.common.AppLogger;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainFileMemberMonitoringApp {

    private static final Logger logger = AppLogger.getLogger();

    private static FileMemberMonitor fileMemberMonitor;
    private static IbmiFileMemberService fileMemberService;
    private static EmailService emailService;
    private static HTTPServer prometheusServer;

    public static void main(String[] args) {
        String fileMemberMonitorConfigFile = "filemembermonitor.properties";
        String emailConfigFile = "email.properties";

        try {
            // Load configuration first to read log.level
            FileMemberMonitorConfig tempConfig = new FileMemberMonitorConfig(fileMemberMonitorConfigFile, emailConfigFile);
            String logLevel = tempConfig.getEmailProperties().getProperty("log.level", "INFO");
            String logFolder = tempConfig.getEmailProperties().getProperty("log.folder", "logs");
            // Re-initialize logger with config values
            AppLogger.setupLogger("ibmifilemembermonitor", logLevel, logFolder);
            
            // Start automatic log purge
            int retentionDays = Integer.parseInt(tempConfig.getEmailProperties().getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(tempConfig.getEmailProperties().getProperty("log.purge.interval.hours", "24"));
            AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
            
            logger.info("Loading application configuration...");
            FileMemberMonitorConfig config = tempConfig; // Use the already loaded config
            logger.info("Configuration loaded successfully.");

            logger.info("Initializing IBM i File Member Service...");
            fileMemberService = new IbmiFileMemberService(
                config.getIbmiHost(),
                config.getIbmiUser(),
                config.getIbmiPassword()
            );
            logger.info("IBM i File Member Service initialized.");

            logger.info("Initializing Email Service...");
            // FIX: Pass the entire email properties object to EmailService constructor
            emailService = new EmailService(config.getEmailProperties());
            logger.info("Email Service initialized.");

            if (config.isFileMemberMonitorEnabled()) {
                logger.info("Initializing IBM i File Member Monitor...");
                fileMemberMonitor = new FileMemberMonitor(fileMemberService, emailService, config, config.getClientName());
                fileMemberMonitor.start();
                logger.info("IBM i File Member Monitor started successfully.");
            } else {
                logger.info("IBM i File Member Monitor is disabled in configuration. Not starting monitor.");
            }

            logger.info("Starting Prometheus HTTP server on port " + config.getPrometheusFileMemberPort() + "...");
            prometheusServer = new HTTPServer(config.getPrometheusFileMemberPort());
            logger.info("Prometheus HTTP server started.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook initiated. Stopping services...");
                if (fileMemberMonitor != null) {
                    fileMemberMonitor.stop();
                }
                if (fileMemberService != null) {
                    try {
                        fileMemberService.disconnect();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error during IBM i service disconnect on shutdown: " + e.getMessage(), e);
                    }
                }
                if (prometheusServer != null) {
                    prometheusServer.close();
                    logger.info("Prometheus HTTP server stopped.");
                }
                AppLogger.closeLogger();
                logger.info("Services stopped. Application exiting.");
            }));

            Thread.currentThread().join(); // Keep the main thread alive

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration files: " + e.getMessage(), e);
            logger.log(Level.SEVERE, "Application will exit. Please ensure 'filemembermonitor.properties' and 'email.properties' are in the working directory.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unhandled error occurred during application startup or runtime: " + e.getMessage(), e);
            logger.log(Level.SEVERE, "Application will exit.", e);
        }
    }
}
