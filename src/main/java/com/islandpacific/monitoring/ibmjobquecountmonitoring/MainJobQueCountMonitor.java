package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainJobQueCountMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    // File paths for configuration properties
    private static String emailPropertiesFilePath = "email.properties";
    private static String jobQueueMonitorPropertiesFilePath = "ibmjobqueuemonitor.properties";

    // Properties objects for different config files
    private static Properties emailAndGeneralProps = new Properties(); // For email settings
    private static Properties ibmiAndJobQueueProps = new Properties(); // For IBM i, job queue, and monitor settings

    // Services
    private static MonitoringConfig monitorConfig;
    private static IbmiJobQueueService ibmiJobQueueService;
    private static EmailService emailService;
    private static MetricsServer metricsServer;

    public static void main(String[] args) {
        // Allow command-line arguments to override default property file paths
        if (args.length >= 1) {
            emailPropertiesFilePath = args[0];
        }
        if (args.length >= 2) {
            jobQueueMonitorPropertiesFilePath = args[1];
        }

        try {
            loadProperties();
            setupLogger();

            int retentionDays = Integer.parseInt(emailAndGeneralProps.getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(emailAndGeneralProps.getProperty("log.purge.interval.hours", "24"));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            ibmiJobQueueService = new IbmiJobQueueService(
                monitorConfig.getIbmiHost(),
                monitorConfig.getIbmiUser(),
                monitorConfig.getIbmiPassword()
            );

            emailService = new EmailService(
                emailAndGeneralProps,
                monitorConfig.getIbmiHost(),
                monitorConfig.getClientMonitorName(),
                monitorConfig.getEmailImportance(),
                monitorConfig.getLogoPath()
            );
            metricsServer = new MetricsServer(monitorConfig.getMetricsPort(), monitorConfig.getJobQueuesToMonitor());
            metricsServer.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            logger.info("Starting Job Queue monitoring service. Checking every " + monitorConfig.getMonitorIntervalMs() / 1000 + " seconds.");

            // Schedule the periodic job queue checks
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    long currentScanTimestamp = Instant.now().getEpochSecond(); // Get timestamp at start of scan cycle
                    
                    for (JobQueueInfo jqInfo : monitorConfig.getJobQueuesToMonitor()) {
                        logger.info("Starting new scan for job queue: " + jqInfo.getName() + "/" + jqInfo.getLibrary() + " (ID: " + jqInfo.getId() + ")");
                        checkSingleJobQueue(jqInfo);
                    }
                    metricsServer.setLastOverallScanTimestamp(currentScanTimestamp); // Update global timestamp for metrics
                    logger.info("All configured job queue scans completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during overall job queue monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, monitorConfig.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            // Add a shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Job Queue monitor gracefully...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate in time, forcing shutdown.");
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.warning("Shutdown interrupted.");
                    scheduler.shutdownNow();
                } finally {
                    metricsServer.stop();
                    logger.info("Job Queue monitor shutdown complete.");
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

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private static void setupLogger() throws IOException {
        // Use standardized AppLogger
        String logLevel = emailAndGeneralProps.getProperty("log.level", ibmiAndJobQueueProps.getProperty("log.level", "INFO"));
        String logFolder = emailAndGeneralProps.getProperty("log.folder", ibmiAndJobQueueProps.getProperty("log.folder", "logs"));
        com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmjobquecountmonitoring", logLevel, logFolder);
    }

    /**
     * Loads application properties from the specified configuration files.
     * Initializes the MonitoringConfig object by parsing properties from both files.
     *
     * @throws IOException If there's an issue reading the properties file.
     * @throws IllegalArgumentException If required properties are missing or invalid.
     */
    private static void loadProperties() throws IOException, IllegalArgumentException {
        // Load email and general application properties
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailAndGeneralProps.load(in);
            logger.info("Loaded email and general application properties from: " + emailPropertiesFilePath);
        } catch (FileNotFoundException e) {
            logger.severe(String.format("Email properties file not found. Ensure '%s' exists. Error: %s",
                                 emailPropertiesFilePath, e.getMessage()));
            throw new IllegalArgumentException("Missing email properties file: " + e.getMessage());
        }

        // Load IBM i, job queue, and monitor specific properties
        try (InputStream in = new FileInputStream(jobQueueMonitorPropertiesFilePath)) {
            ibmiAndJobQueueProps.load(in);
            logger.info("Loaded IBM i, job queue, and monitor specific properties from: " + jobQueueMonitorPropertiesFilePath);
        } catch (FileNotFoundException e) {
            logger.severe(String.format("Job queue monitor properties file not found. Ensure '%s' exists. Error: %s",
                                 jobQueueMonitorPropertiesFilePath, e.getMessage()));
            throw new IllegalArgumentException("Missing job queue monitor properties file: " + e.getMessage());
        }

        try {
            // Build the MonitoringConfig object from loaded properties
            monitorConfig = MonitoringConfig.fromProperties(emailAndGeneralProps, ibmiAndJobQueueProps);

            logger.info("Properties loaded successfully and configuration initialized.");
            logger.info("IBM i Host: " + monitorConfig.getIbmiHost());
            logger.info("Total Job Queues to Monitor: " + monitorConfig.getJobQueuesToMonitor().size());
            logger.info("Monitor Interval: " + monitorConfig.getMonitorIntervalMs() + " ms");
            logger.info("Metrics Port: " + monitorConfig.getMetricsPort());
            logger.info("Client Monitor Name: " + monitorConfig.getClientMonitorName()); // Log the new property

        } catch (NumberFormatException e) {
            logger.severe("Invalid number format in properties: " + e.getMessage());
            throw new IllegalArgumentException("Invalid number format in properties: " + e.getMessage());
        }
    }

    /**
     * Checks a single IBM i Job Queue for waiting jobs and sends an email alert
     * if the number of waiting jobs exceeds the configured threshold for that queue.
     * This method now delegates to IbmiJobQueueService and EmailService.
     *
     * @param jqInfo The configuration information for the specific job queue to check.
     */
    private static void checkSingleJobQueue(JobQueueInfo jqInfo) {
        try {
            int waitingJobsCount = ibmiJobQueueService.getNumberOfWaitingJobs(jqInfo.getName(), jqInfo.getLibrary());
            
            // Update metrics for this specific job queue
            metricsServer.updateJobCountMetric(jqInfo.getId(), waitingJobsCount);

            logger.info(String.format("Current waiting jobs in %s/%s (ID: %s): %d (Threshold: %d)",
                                         jqInfo.getLibrary(), jqInfo.getName(), jqInfo.getId(),
                                         waitingJobsCount, jqInfo.getThreshold()));

            if (waitingJobsCount > jqInfo.getThreshold()) {
                logger.warning(String.format("Threshold exceeded for %s/%s! Sending email alert. Waiting jobs: %d, Threshold: %d",
                                             jqInfo.getLibrary(), jqInfo.getName(),
                                             waitingJobsCount, jqInfo.getThreshold()));
                // Individual job details are not available with getNumberOfJobs(), pass empty list for now
                emailService.sendAlert(jqInfo, waitingJobsCount, new ArrayList<>());
            } else {
                logger.info("Number of waiting jobs is within the acceptable threshold for " + jqInfo.getName() + "/" + jqInfo.getLibrary() + ". No email sent.");
            }

        } catch (AS400SecurityException e) {
            logger.log(Level.SEVERE, "IBM i Security Error for " + jqInfo.getName() + "/" + jqInfo.getLibrary() + ": Check user ID and password. " + e.getMessage(), e);
        } catch (AS400Exception e) {
            logger.log(Level.SEVERE, "IBM i System Error for " + jqInfo.getName() + "/" + jqInfo.getLibrary() + ": " + e.getLocalizedMessage() + " (Error details: " + e.getMessage() + ")", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while checking job queue " + jqInfo.getName() + "/" + jqInfo.getLibrary() + ": " + e.getMessage(), e);
        }
    }
}