package com.islandpacific.monitoring.ibmjobdurationmonitor;

import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JobMonitorConfig {

    private final Properties emailProps;
    private final Properties monitorProps;

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;
    private final List<JobSpec> jobSpecs;
    private final int monitorIntervalMs;
    private final int metricsPort;
    private final String clientName;

    public JobMonitorConfig(String emailPropsFile, String monitorPropsFile) throws IOException {
        emailProps = new Properties();
        try (InputStream in = new FileInputStream(emailPropsFile)) {
            emailProps.load(in);
        }
        monitorProps = new Properties();
        try (InputStream in = new FileInputStream(monitorPropsFile)) {
            monitorProps.load(in);
        }

        ibmiHost = require("ibmi.host");
        ibmiUser = require("ibmi.user");
        String passwordRaw = monitorProps.getProperty("ibmi.password");
        if (passwordRaw == null || passwordRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Required property 'ibmi.password' is missing or empty.");
        }
        ibmiPassword = CredentialProtector.resolve(passwordRaw);

        monitorIntervalMs = Integer.parseInt(monitorProps.getProperty("monitor.interval.ms", "120000"));
        metricsPort = Integer.parseInt(monitorProps.getProperty("metrics.port", "3022"));
        clientName = monitorProps.getProperty("client.name", emailProps.getProperty("mail.clientName", ""));

        jobSpecs = parseJobSpecs(monitorProps.getProperty("jobs.to.monitor", ""));
        if (jobSpecs.isEmpty()) {
            throw new IllegalArgumentException("Required property 'jobs.to.monitor' is missing or empty — no jobs configured.");
        }
    }

    // Format: JOB_NAME,JOB_USER,MAX_DURATION_MINUTES;JOB_NAME2,*,30
    // JOB_USER can be * to match any user
    private List<JobSpec> parseJobSpecs(String raw) {
        List<JobSpec> specs = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return specs;
        for (String entry : raw.split(";")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid jobs.to.monitor entry '" + trimmed + "'. Expected: JOB_NAME,JOB_USER,MAX_DURATION_MINUTES");
            }
            String jobName = parts[0].trim().toUpperCase();
            String jobUser = parts[1].trim().toUpperCase();
            long maxMinutes;
            try {
                maxMinutes = Long.parseLong(parts[2].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid MAX_DURATION_MINUTES in jobs.to.monitor entry '" + trimmed + "'");
            }
            if (maxMinutes <= 0) {
                throw new IllegalArgumentException("MAX_DURATION_MINUTES must be positive in entry '" + trimmed + "'");
            }
            specs.add(new JobSpec(jobName, jobUser, maxMinutes));
        }
        return specs;
    }

    private String require(String key) {
        String val = monitorProps.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Required property '" + key + "' is missing or empty.");
        }
        return val.trim();
    }

    public String getIbmiHost() { return ibmiHost; }
    public String getIbmiUser() { return ibmiUser; }
    public String getIbmiPassword() { return ibmiPassword; }
    public List<JobSpec> getJobSpecs() { return jobSpecs; }
    public int getMonitorIntervalMs() { return monitorIntervalMs; }
    public int getMetricsPort() { return metricsPort; }
    public String getClientName() { return clientName; }
    public Properties getEmailProps() { return emailProps; }
    public Properties getMonitorProps() { return monitorProps; }

    public static class JobSpec {
        public final String jobName;
        public final String jobUser;
        public final long maxDurationMinutes;

        public JobSpec(String jobName, String jobUser, long maxDurationMinutes) {
            this.jobName = jobName;
            this.jobUser = jobUser;
            this.maxDurationMinutes = maxDurationMinutes;
        }

        public String key() {
            return jobName + "/" + jobUser;
        }

        @Override
        public String toString() {
            return jobName + "/" + jobUser + "(max " + maxDurationMinutes + "m)";
        }
    }
}
