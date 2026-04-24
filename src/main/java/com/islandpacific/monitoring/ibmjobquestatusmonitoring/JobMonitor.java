package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobMonitor {

    private static final Logger logger = Logger.getLogger(JobMonitor.class.getName());

    private final IbmiJobService ibmiJobService;
    private final JobMetricsExporter metricsExporter;
    private final EmailService emailService;
    private final JobMonitorConfig config;

    private ScheduledExecutorService scheduler;

    // Track job active/inactive transitions
    private final Map<String, Boolean> lastJobActiveStatus = new HashMap<>();

    public JobMonitor(JobMonitorConfig config,
            IbmiJobService ibmiJobService,
            JobMetricsExporter metricsExporter,
            EmailService emailService) {
        this.config = config;
        this.ibmiJobService = ibmiJobService;
        this.metricsExporter = metricsExporter;
        this.emailService = emailService;
    }

    public void start() {
        logger.info("Starting IBM i Job Monitor...");

        metricsExporter.startMetricsServer(config.getPrometheusPort());

        // We no longer assume active status at startup.
        // The first polling cycle will determine the baseline status for each job.

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::monitorJobs,
                0,
                config.getPollingIntervalSeconds(),
                TimeUnit.SECONDS);

        logger.info("IBM i Job Monitor started. Polling every " + config.getPollingIntervalSeconds() + " seconds.");
    }

    public void stop() {
        logger.info("Stopping IBM i Job Monitor...");
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        metricsExporter.stopMetricsServer();
        logger.info("IBM i Job Monitor stopped.");
    }

    // Track which jobs have completed their first polling cycle to establish a
    // baseline
    private final java.util.Set<String> initializedJobs = new java.util.HashSet<>();

    private void monitorJobs() {
        logger.fine("Executing job monitoring cycle...");

        for (JobMonitorConfig.JobDefinition jobDef : config.getJobsToMonitor()) {
            String jobName = jobDef.getJobName();
            String jobUser = jobDef.getJobUser();
            String expectedSubsystem = jobDef.getExpectedSubsystem();
            String jobIdentifier = getJobIdentifier(jobDef);

            try {
                // Try fetching job info
                JobInfo jobInfo = ibmiJobService.getJobInfo(jobName, jobUser, expectedSubsystem);

                boolean currentStatusActive = (jobInfo != null && jobInfo.isActive());

                metricsExporter.updateMetrics(jobName, jobUser, expectedSubsystem, jobInfo);

                // Check if this is the very first time we are polling this job since startup
                if (!initializedJobs.contains(jobIdentifier)) {
                    // Record baseline status without triggering any alerts
                    lastJobActiveStatus.put(jobIdentifier, currentStatusActive);
                    initializedJobs.add(jobIdentifier);
                    logger.info("Initialized baseline status for job " + jobIdentifier + " to "
                            + (currentStatusActive ? "ACTIVE" : "INACTIVE"));
                    continue; // Skip the rest of the transition logic for the very first poll
                }

                boolean lastStatusActive = lastJobActiveStatus.getOrDefault(jobIdentifier, currentStatusActive);

                // Handle INACTIVE transition
                if (!currentStatusActive && lastStatusActive) {
                    String currentStatusStr = (jobInfo != null) ? jobInfo.getJobStatus() : "NOT FOUND";
                    String sbsName = (jobInfo != null && jobInfo.getSubsystemName() != null)
                            ? jobInfo.getSubsystemName()
                            : expectedSubsystem;

                    logger.warning("Job " + jobIdentifier + " changed to INACTIVE/NOT FOUND.");

                    if (config.isSendEmailAlerts()) {
                        JobInfo alertJobInfo = (jobInfo != null)
                                ? jobInfo
                                : new JobInfo(jobName, jobUser, "N/A",
                                        "NOT FOUND", "N/A", sbsName, "N/A", "N/A", 0);

                        emailService.sendJobStatusChangeAlert(alertJobInfo,
                                "JOB_INACTIVE_ALERT", currentStatusStr, sbsName);
                    }

                    lastJobActiveStatus.put(jobIdentifier, false);

                }
                // Handle RECOVERY transition
                else if (currentStatusActive && !lastStatusActive) {
                    String currentStatusStr = jobInfo.getJobStatus();
                    String sbsName = jobInfo.getSubsystemName() != null ? jobInfo.getSubsystemName()
                            : expectedSubsystem;

                    logger.info("Job " + jobIdentifier + " recovered to ACTIVE.");

                    if (config.isSendEmailAlerts()) {
                        emailService.sendJobStatusChangeAlert(jobInfo,
                                "JOB_RECOVERY_ALERT", currentStatusStr, sbsName);
                    }

                    lastJobActiveStatus.put(jobIdentifier, true);
                }

            } catch (Exception e) {
                // IBM i is down or unreachable
                logger.log(Level.SEVERE,
                        "IBM i connectivity error while monitoring job " + jobIdentifier +
                                ": " + e.getMessage(),
                        e);

                // DO NOT SEND EMAIL ALERTS FOR CONNECTIVITY ISSUES
                // DO NOT mark the job inactive, no false transitions

                // Only update metrics with null jobInfo
                metricsExporter.updateMetrics(jobName, jobUser, expectedSubsystem, null);

                // Keep last status unchanged
            }
        }

        logger.fine("Job monitoring cycle completed.");
    }

    private String getJobIdentifier(JobMonitorConfig.JobDefinition jobDef) {
        return jobDef.getJobName() + "/" + jobDef.getJobUser() + "/" +
                (jobDef.getExpectedSubsystem() != null ? jobDef.getExpectedSubsystem() : "any");
    }
}
