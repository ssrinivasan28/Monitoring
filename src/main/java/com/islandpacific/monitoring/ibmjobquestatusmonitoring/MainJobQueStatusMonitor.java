package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import com.islandpacific.monitoring.common.AppLogger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainJobQueStatusMonitor {

    private static final Logger logger = AppLogger.getLogger();

    public static void main(String[] args) {
        String jobListFile = args.length >= 1 ? args[0] : "ibmjobquestatusmonitor.properties";
        String emailFile = args.length >= 2 ? args[1] : "email.properties";

        Properties emailProps = new Properties();
        try (InputStream input = new FileInputStream(emailFile)) {
            emailProps.load(input);
        } catch (IOException e) {
            System.err.println("Could not load " + emailFile + ": " + e.getMessage());
        }

        String logLevel = emailProps.getProperty("log.level", "INFO");
        String logFolder = emailProps.getProperty("log.folder", "logs");
        AppLogger.setupLogger("ibmjobquestatusmonitoring", logLevel, logFolder);

        int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        JobMonitorConfig config;
        try {
            config = new JobMonitorConfig(jobListFile, emailFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration: " + e.getMessage(), e);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Job Queue Status Monitor...");
            AppLogger.closeLogger();
        }));

        Properties jobProps = new Properties();
        try (InputStream input = new FileInputStream(jobListFile)) { jobProps.load(input); } catch (IOException e) { /* use empty */ }
        String logoPath = jobProps.getProperty("logo.path", "");
        EmailService emailService = new EmailService(emailProps, config.getIbmiHost(), logoPath);
        IbmiJobService ibmiJobService = new IbmiJobService(
                config.getIbmiHost(), config.getIbmiUser(), config.getIbmiPassword());
        JobMetricsExporter metricsExporter = new JobMetricsExporter();
        JobMonitor monitor = new JobMonitor(config, ibmiJobService, metricsExporter, emailService);

        Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
        monitor.start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
