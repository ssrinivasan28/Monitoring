package com.islandpacific.monitoring.ibmjobquestatusmonitoring; // Corrected package name

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobMetricsExporter {

    private static final Logger logger = Logger.getLogger(JobMetricsExporter.class.getName());

    // Metric for Job Active Status (1 for Active, 0 for Not Found/Inactive)
    private static final Gauge JOB_ACTIVE_STATUS = Gauge.build()
            .name("ibmi_job_active_status")
            .help("Status of IBM i Job (1=Active, 0=Not Found/Inactive).")
            .labelNames("job_name", "job_user", "subsystem_name")
            .register();

    // Metric for Job CPU Usage
    private static final Gauge JOB_CPU_USED_MILLISECONDS = Gauge.build()
            .name("ibmi_job_cpu_used_milliseconds")
            .help("CPU time used by IBM i Job in milliseconds.")
            .labelNames("job_name", "job_user", "subsystem_name")
            .register();

    // Metric for Job Status String (useful for Grafana labels/filters, though not
    // directly numeric)
    // Prometheus best practice is to use numeric values, but this can be helpful
    // for debugging
    // or displaying the raw status string. For alerting, rely on JOB_ACTIVE_STATUS.
    private static final Gauge JOB_CURRENT_STATUS_STRING = Gauge.build()
            .name("ibmi_job_current_status_string")
            .help("Current status string of the IBM i Job (e.g., RUN, MSGW). Value is 1 if job is found, 0 otherwise.")
            .labelNames("job_name", "job_user", "subsystem_name", "status_string")
            .register();

    // Track the last reported status string to clean up old metrics from Prometheus
    private final Map<String, String> lastStatusMap = new ConcurrentHashMap<>();

    private HTTPServer httpServer;

    public void startMetricsServer(int port) {
        try {
            httpServer = new HTTPServer(port);
            logger.info("Prometheus metrics server for Jobs started on port " + port);
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Failed to start Prometheus metrics server for Jobs on port " + port + ": " + e.getMessage(), e);
            throw new RuntimeException("Could not start Prometheus metrics server.", e);
        }
    }

    public void stopMetricsServer() {
        if (httpServer != null) {
            httpServer.stop();
            logger.info("Prometheus metrics server for Jobs stopped.");
        }
    }

    public void updateMetrics(String jobName, String jobUser, String expectedSubsystem, JobInfo jobInfo) {
        String sbsLabel = (expectedSubsystem != null && !expectedSubsystem.trim().isEmpty()) ? expectedSubsystem
                : "any";
        String mapKey = jobName + "_" + jobUser + "_" + sbsLabel;
        String newStatusString;

        if (jobInfo != null && jobInfo.isActive()) {
            JOB_ACTIVE_STATUS.labels(jobName, jobUser, sbsLabel).set(1);
            JOB_CPU_USED_MILLISECONDS.labels(jobName, jobUser, sbsLabel).set(jobInfo.getCpuUsed());

            newStatusString = jobInfo.getJobStatus();
        } else {
            // Job not found or not active. Set active status to 0.
            JOB_ACTIVE_STATUS.labels(jobName, jobUser, sbsLabel).set(0);
            JOB_CPU_USED_MILLISECONDS.labels(jobName, jobUser, sbsLabel).set(0); // Reset CPU if not active

            newStatusString = "NOT_ACTIVE_OR_FOUND";
        }

        // Clean up old status string metric if it has changed
        String lastStatusString = lastStatusMap.get(mapKey);
        if (lastStatusString != null && !lastStatusString.equals(newStatusString)) {
            JOB_CURRENT_STATUS_STRING.remove(jobName, jobUser, sbsLabel, lastStatusString);
            logger.fine(String.format("Removed old status metric for job %s/%s in %s: %s", jobName, jobUser, sbsLabel,
                    lastStatusString));
        }

        // Set the new status string metric
        JOB_CURRENT_STATUS_STRING.labels(jobName, jobUser, sbsLabel, newStatusString).set(1);
        lastStatusMap.put(mapKey, newStatusString);

        if (jobInfo != null && jobInfo.isActive()) {
            logger.fine(String.format("Metrics updated for active job %s/%s in %s. Status: %s", jobName, jobUser,
                    sbsLabel, newStatusString));
        } else {
            logger.fine(String.format("Metrics updated for inactive/not found job %s/%s in %s.", jobName, jobUser,
                    sbsLabel));
        }
    }
}
