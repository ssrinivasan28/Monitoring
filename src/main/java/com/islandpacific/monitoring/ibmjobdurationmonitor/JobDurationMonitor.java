package com.islandpacific.monitoring.ibmjobdurationmonitor;

import com.islandpacific.monitoring.ibmjobdurationmonitor.IbmiJobService.ActiveJobInfo;
import com.islandpacific.monitoring.ibmjobdurationmonitor.JobMonitorConfig.JobSpec;
import io.prometheus.client.Gauge;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobDurationMonitor {

    private static final Logger logger = Logger.getLogger(JobDurationMonitor.class.getName());

    private static final long ALERT_COOLDOWN_MS = 60L * 60 * 1000; // 1 hour

    private static final Gauge JOB_ELAPSED_MINUTES = Gauge.build()
            .name("ibmi_job_elapsed_minutes")
            .help("Elapsed run time in minutes for monitored IBM i job")
            .labelNames("job_name", "job_user", "job_number")
            .register();

    private static final Gauge JOB_OVER_THRESHOLD = Gauge.build()
            .name("ibmi_job_over_threshold")
            .help("1 if job duration exceeds configured threshold, 0 otherwise")
            .labelNames("job_name", "job_user")
            .register();

    private static final Gauge JOB_NOT_FOUND = Gauge.build()
            .name("ibmi_job_not_found")
            .help("1 if expected job was not found in active job list, 0 otherwise")
            .labelNames("job_name", "job_user")
            .register();

    private final JobMonitorConfig config;
    private final IbmiJobService jobService;
    private final EmailService emailService;

    private final Map<String, Long> lastOverThresholdAlert = new HashMap<>();
    private final Map<String, Long> lastNotFoundAlert = new HashMap<>();

    public JobDurationMonitor(JobMonitorConfig config, IbmiJobService jobService, EmailService emailService) {
        this.config = config;
        this.jobService = jobService;
        this.emailService = emailService;
    }

    public void runCheck() {
        for (JobSpec spec : config.getJobSpecs()) {
            checkJob(spec);
        }
    }

    private void checkJob(JobSpec spec) {
        try {
            List<ActiveJobInfo> activeJobs = jobService.getActiveJobs(spec.jobName, spec.jobUser);

            if (activeJobs.isEmpty()) {
                logger.warning("Job not found in active list: " + spec.key());
                JOB_NOT_FOUND.labels(spec.jobName, spec.jobUser).set(1.0);
                JOB_OVER_THRESHOLD.labels(spec.jobName, spec.jobUser).set(0.0);

                if (shouldAlert(lastNotFoundAlert, spec.key())) {
                    emailService.sendJobNotFoundAlert(spec.jobName, spec.jobUser);
                    lastNotFoundAlert.put(spec.key(), System.currentTimeMillis());
                }
                return;
            }

            JOB_NOT_FOUND.labels(spec.jobName, spec.jobUser).set(0.0);

            boolean anyOverThreshold = false;
            for (ActiveJobInfo job : activeJobs) {
                long elapsedMinutes = job.elapsedSeconds / 60;
                JOB_ELAPSED_MINUTES.labels(job.jobName, job.jobUser, job.jobNumber).set(elapsedMinutes);

                if (elapsedMinutes > spec.maxDurationMinutes) {
                    anyOverThreshold = true;
                    logger.warning("Job " + spec.key() + " (" + job.jobNumber + ") over threshold: "
                            + elapsedMinutes + "m > " + spec.maxDurationMinutes + "m");

                    if (shouldAlert(lastOverThresholdAlert, spec.key())) {
                        emailService.sendJobOverThresholdAlert(
                                job.jobName, job.jobUser, job.jobNumber,
                                elapsedMinutes, spec.maxDurationMinutes);
                        lastOverThresholdAlert.put(spec.key(), System.currentTimeMillis());
                    }
                }
            }
            JOB_OVER_THRESHOLD.labels(spec.jobName, spec.jobUser).set(anyOverThreshold ? 1.0 : 0.0);

            if (!anyOverThreshold) {
                lastOverThresholdAlert.remove(spec.key());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking job " + spec.key() + ": " + e.getMessage(), e);
        }
    }

    private boolean shouldAlert(Map<String, Long> cooldownMap, String key) {
        Long lastAlert = cooldownMap.get(key);
        return lastAlert == null || (System.currentTimeMillis() - lastAlert) >= ALERT_COOLDOWN_MS;
    }
}
