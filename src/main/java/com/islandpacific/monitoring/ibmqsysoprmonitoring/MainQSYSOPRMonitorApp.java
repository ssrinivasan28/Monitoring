package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import com.islandpacific.monitoring.common.AppLogger;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger; // Import the JUL Logger


public class MainQSYSOPRMonitorApp {

    private static final Logger LOGGER = AppLogger.getLogger(); // Get logger instance

    public static void main(String[] args) {
        // 1. Load Configurations (needed for logger setup)
        QSYSOPRMonitorConfig config = new QSYSOPRMonitorConfig();
        if (!config.loadConfigurations()) {
            // Error logged by QSYSOPRMonitorConfig already, just exit.
            System.err.println("Failed to load configurations. Application will exit."); // Fallback print
            return;
        }

        // 2. Initialize Logger (now that config is loaded)
        AppLogger.setupLogger("ibmqsysoprmonitoring", config.getLogLevel(), config.getLogFolder());
        
        // Start automatic log purge
        Properties mailProps = config.getMailProperties();
        int retentionDays = Integer.parseInt(mailProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(mailProps.getProperty("log.purge.interval.hours", "24"));
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        
        LOGGER.info("Starting QSYSOPR Monitor Application...");
        LOGGER.info("Configurations loaded successfully.");

        // 3. Initialize Metrics (must be called early to set initial status)
        QSYSOPRMonitorMetrics.initializeMetrics();

        // 4. Initialize Services
        EmailService emailService = new EmailService(config.getMailProperties());
        QSYSOPRMonitorService monitorService = new QSYSOPRMonitorService(config, emailService);

        // 5. Start Metrics Server (for Prometheus scraping)
        QSYSOPRMetricsServer metricsServer = new QSYSOPRMetricsServer(config.getMetricsPort());
        metricsServer.startServer();
        LOGGER.info("Prometheus Metrics Server started on port " + config.getMetricsPort());

        // 6. Load Last Checked Timestamp
        String lastCheckedTimestamp = monitorService.loadLastCheckedTimestamp();
        if (lastCheckedTimestamp != null) {
            LOGGER.info("Resuming from last checked timestamp: " + lastCheckedTimestamp);
        } else {
            LOGGER.info("No previous state found. Will process all current messages and save state.");
        }

        // 7. Start the Monitoring Loop
        try {
            while (true) {
                // Scan and Alert, updating the last checked timestamp
                String newLastCheckedTimestamp = monitorService.scanAndAlert(lastCheckedTimestamp);

                if (newLastCheckedTimestamp != null) {
                    lastCheckedTimestamp = newLastCheckedTimestamp;
                    // Save the updated timestamp after each scan
                    monitorService.saveLastCheckedTimestamp(lastCheckedTimestamp);
                }

                LOGGER.info("Next scan in " + (config.getMonitorIntervalMillis() / 1000) + " seconds...");
                Thread.sleep(config.getMonitorIntervalMillis());
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.INFO, "Monitoring stopped by user.", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during monitoring loop: " + e.getMessage(), e);
        } finally {
            // Clean up resources
            if (metricsServer != null) {
                metricsServer.stopServer();
                LOGGER.info("Prometheus Metrics Server stopped.");
            }
            QSYSOPRMonitorMetrics.setMonitorStopped(); // Indicate graceful shutdown or error
            LOGGER.info("QSYSOPR Monitor Application stopped.");
            AppLogger.closeLogger(); // Close the logger file handler
        }
    }
}
