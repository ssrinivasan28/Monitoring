package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class MonitoringConfig {
    // IBM i Connection
    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;

    // List of Job Queues to Monitor
    private final List<JobQueueInfo> jobQueuesToMonitor;

    // Email Details
    private final String emailHost;
    private final String emailPort;
    private final String emailFrom;
    private final String emailTo;
    private final String emailBcc;
    private final String emailUsername;
    private final String emailPassword;
    private final boolean emailAuthEnabled;
    private final boolean emailStartTlsEnabled;
    private final String emailImportance;

    // Monitor Settings
    private final int monitorIntervalMs;
    private final int metricsPort;
    private final String clientMonitorName; 

 
    private MonitoringConfig(String ibmiHost, String ibmiUser, String ibmiPassword,
                             List<JobQueueInfo> jobQueuesToMonitor,
                             String emailHost, String emailPort, String emailFrom, String emailTo, String emailBcc,
                             String emailUsername, String emailPassword, boolean emailAuthEnabled, boolean emailStartTlsEnabled, String emailImportance,
                             int monitorIntervalMs, int metricsPort, String clientMonitorName) { // Add clientMonitorName here
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
        // Defensive copy and make unmodifiable
        this.jobQueuesToMonitor = Collections.unmodifiableList(new ArrayList<>(jobQueuesToMonitor));
        this.emailHost = emailHost;
        this.emailPort = emailPort;
        this.emailFrom = emailFrom;
        this.emailTo = emailTo;
        this.emailBcc = emailBcc;
        this.emailUsername = emailUsername;
        this.emailPassword = emailPassword;
        this.emailAuthEnabled = emailAuthEnabled;
        this.emailStartTlsEnabled = emailStartTlsEnabled;
        this.emailImportance = emailImportance;
        this.monitorIntervalMs = monitorIntervalMs;
        this.metricsPort = metricsPort;
        this.clientMonitorName = clientMonitorName; // Initialize new field
    }

   
    public static MonitoringConfig fromProperties(Properties emailProps, Properties ibmiJobQueueProps)
            throws IllegalArgumentException, NumberFormatException {

        // Helper for required properties
        java.util.function.BiFunction<Properties, String, String> getRequiredProperty = (props, key) -> {
            String value = props.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Required property '" + key + "' is missing or empty.");
            }
            return value.trim();
        };

        // IBM i connection properties (global for all queues)
        String ibmiHost = getRequiredProperty.apply(ibmiJobQueueProps, "ibmi.host");
        String ibmiUser = getRequiredProperty.apply(ibmiJobQueueProps, "ibmi.user");
        String ibmiPassword = ibmiJobQueueProps.getProperty("ibmi.password", "");

        // Parse multiple job queue configurations
        List<JobQueueInfo> jobQueuesToMonitor = new ArrayList<>();
        String jobQueueIdsString = getRequiredProperty.apply(ibmiJobQueueProps, "jobqueue.monitor.ids");
        String[] jobQueueIds = jobQueueIdsString.split(",");

        if (jobQueueIds.length == 0 || (jobQueueIds.length == 1 && jobQueueIds[0].trim().isEmpty())) {
            throw new IllegalArgumentException("No job queue IDs specified in 'jobqueue.monitor.ids'.");
        }

        for (String id : jobQueueIds) {
            String trimmedId = id.trim();
            if (trimmedId.isEmpty()) continue; // Skip empty IDs if any from extra commas

            String jqName = getRequiredProperty.apply(ibmiJobQueueProps, "jobqueue." + trimmedId + ".name");
            String jqLibrary = getRequiredProperty.apply(ibmiJobQueueProps, "jobqueue." + trimmedId + ".library");
            int jqThreshold = Integer.parseInt(getRequiredProperty.apply(ibmiJobQueueProps, "jobqueue." + trimmedId + ".threshold"));
            jobQueuesToMonitor.add(new JobQueueInfo(trimmedId, jqName, jqLibrary, jqThreshold));
        }

        // Email properties
        String emailHost = getRequiredProperty.apply(emailProps, "mail.smtp.host");
        String emailPort = emailProps.getProperty("mail.smtp.port", "25");
        String emailFrom = getRequiredProperty.apply(emailProps, "mail.from");
        String emailTo = getRequiredProperty.apply(emailProps, "mail.to");
        String emailBcc = emailProps.getProperty("mail.bcc", "");
        String emailUsername = emailProps.getProperty("mail.smtp.username", "");
        String emailPassword = emailProps.getProperty("mail.smtp.password", "");
        boolean emailAuth = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        boolean emailStartTlsEnable = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));
        String emailImportance = emailProps.getProperty("mail.importance", "Normal");

        // Monitor interval and metrics port
        int monitorInterval = Integer.parseInt(ibmiJobQueueProps.getProperty("monitor.interval.ms", String.valueOf(60000)));
        int metricsPort = Integer.parseInt(ibmiJobQueueProps.getProperty("metrics.port", String.valueOf(8081)));

        // NEW: Read client.monitor property
        String clientMonitorName = ibmiJobQueueProps.getProperty("client.monitor", ""); // Default to empty string if not found

        return new MonitoringConfig(
            ibmiHost, ibmiUser, ibmiPassword,
            jobQueuesToMonitor,
            emailHost, emailPort, emailFrom, emailTo, emailBcc,
            emailUsername, emailPassword, emailAuth, emailStartTlsEnable, emailImportance,
            monitorInterval, metricsPort, clientMonitorName // Pass new parameter to constructor
        );
    }

    // Getters for all fields
    public String getIbmiHost() { return ibmiHost; }
    public String getIbmiUser() { return ibmiUser; }
    public String getIbmiPassword() { return ibmiPassword; }
    public List<JobQueueInfo> getJobQueuesToMonitor() { return jobQueuesToMonitor; }
    public String getEmailHost() { return emailHost; }
    public String getEmailPort() { return emailPort; }
    public String getEmailFrom() { return emailFrom; }
    public String getEmailTo() { return emailTo; }
    public String getEmailBcc() { return emailBcc; }
    public String getEmailUsername() { return emailUsername; }
    public String getEmailPassword() { return emailPassword; }
    public boolean isEmailAuthEnabled() { return emailAuthEnabled; }
    public boolean isEmailStartTlsEnabled() { return emailStartTlsEnabled; }
    public String getEmailImportance() { return emailImportance; }
    public int getMonitorIntervalMs() { return monitorIntervalMs; }
    public int getMetricsPort() { return metricsPort; }
    public String getClientMonitorName() { return clientMonitorName; } // NEW GETTER
}