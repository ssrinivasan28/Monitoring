package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import com.islandpacific.monitoring.common.AppLogger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainJobStatusMonitor {

    private static final Logger logger = Logger.getLogger(MainJobStatusMonitor.class.getName());

    public static void main(String[] args) {
        String emailPropsFile  = args.length >= 1 ? args[0] : "email.properties";
        String monitorPropsFile = args.length >= 2 ? args[1] : "ibmjobstatusmonitor.properties";

        JobStatusMonitorConfig config;
        try {
            config = new JobStatusMonitorConfig(emailPropsFile, monitorPropsFile);
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.exit(1);
            return;
        }

        String logLevel  = config.getEmailProps().getProperty("log.level", "INFO");
        String logFolder = config.getEmailProps().getProperty("log.folder", "logs");
        int retentionDays      = Integer.parseInt(config.getEmailProps().getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(config.getEmailProps().getProperty("log.purge.interval.hours", "24"));
        AppLogger.setupLogger("ibmjobstatusmonitoring", logLevel, logFolder);
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        JobStatusService jobService = new JobStatusService(
                config.getIbmiHost(), config.getIbmiUser(), config.getIbmiPassword());
        String logoPath = config.getMonitorProps().getProperty("logo.path", "");
        EmailService emailService = new EmailService(
                config.getEmailProps(), config.getIbmiHost(), config.getClientName(), logoPath);

        JobStatusMetrics metrics = new JobStatusMetrics(config.getMetricsPort());
        try {
            metrics.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start metrics server: " + e.getMessage(), e);
            System.exit(1);
        }

        Set<String> alertedJobs = new HashSet<>();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "job-status-monitor");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<JobStatusInfo> msgwJobs = jobService.getMsgwJobs();
                metrics.update(msgwJobs);

                List<JobStatusInfo> newAlerts = msgwJobs.stream()
                        .filter(j -> j.getElapsedSeconds() >= config.getMsgwThresholdSeconds())
                        .filter(j -> !alertedJobs.contains(j.getFullJobId()))
                        .collect(Collectors.toList());

                if (!newAlerts.isEmpty()) {
                    logger.warning(newAlerts.size() + " new MSGW job(s) exceeded threshold. Sending alert.");
                    emailService.sendMsgwAlert(newAlerts);
                    newAlerts.forEach(j -> alertedJobs.add(j.getFullJobId()));
                }

                Set<String> currentIds = msgwJobs.stream()
                        .map(JobStatusInfo::getFullJobId)
                        .collect(Collectors.toSet());
                alertedJobs.retainAll(currentIds);

                if (msgwJobs.isEmpty()) {
                    logger.info("No jobs in MSGW status.");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during job status check: " + e.getMessage(), e);
            }
        }, 0, config.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down IBM i Job Status Monitor...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) scheduler.shutdownNow();
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            metrics.stop();
        }));

        logger.info("IBM i Job Status Monitor started. Port: " + config.getMetricsPort()
                + ", interval: " + config.getMonitorIntervalMs() + "ms"
                + ", MSGW threshold: " + config.getMsgwThresholdSeconds() + "s.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
