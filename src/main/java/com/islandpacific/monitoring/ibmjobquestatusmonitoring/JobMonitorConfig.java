package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobMonitorConfig {

    private static final Logger logger = Logger.getLogger(JobMonitorConfig.class.getName());

    private final Properties jobListProps = new Properties();
    private final Properties emailProps = new Properties();

    // IBM i Connection Details
    private String ibmiHost;
    private String ibmiUser;
    private String ibmiPassword;

    // Job Monitoring Details
    private List<JobDefinition> jobsToMonitor = new ArrayList<>();
    private long pollingIntervalSeconds;
    private int prometheusPort;

    // Email Configuration (mail.* keys)
    private String smtpHost;
    private String smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String fromEmail;
    private String toEmails;
    private String bccEmails;
    private String emailImportance;
    private boolean smtpAuth;
    private boolean smtpStartTls;
    private boolean sendEmailAlerts;

    public JobMonitorConfig(String jobListConfigFile, String emailConfigFile) throws IOException {
        loadPropertiesFromFileSystem(jobListProps, jobListConfigFile);
        loadPropertiesFromFileSystem(emailProps, emailConfigFile);
        parseConfiguration();
    }

    private void loadPropertiesFromFileSystem(Properties props, String fileName) throws IOException {
        try (InputStream input = new FileInputStream(fileName)) {
            props.load(input);
            logger.info("Successfully loaded " + fileName + " from file system.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load " + fileName + ": " + e.getMessage(), e);
            throw new IOException("Configuration file not found: " + fileName, e);
        }
    }

    private void parseConfiguration() {

        // IBM i Connection
        ibmiHost = jobListProps.getProperty("ibmi.host");
        ibmiUser = jobListProps.getProperty("ibmi.user");
        ibmiPassword = jobListProps.getProperty("ibmi.password");

        // Job Monitoring
        String jobListString = jobListProps.getProperty("jobs.list");
        if (jobListString != null && !jobListString.trim().isEmpty()) {
            Arrays.stream(jobListString.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(entry -> {
                        String[] parts = entry.split(",");
                        if (parts.length >= 2) {
                            String jobName = parts[0].trim();
                            String jobUser = parts[1].trim();
                            String expectedSubsystem = (parts.length == 3) ? parts[2].trim() : null;
                            jobsToMonitor.add(new JobDefinition(jobName, jobUser, expectedSubsystem));
                        } else {
                            logger.log(Level.WARNING, "Invalid job entry in joblist.properties: " + entry);
                        }
                    });
        }

        pollingIntervalSeconds = Long.parseLong(jobListProps.getProperty("monitor.pollingIntervalSeconds", "60"));
        prometheusPort = Integer.parseInt(jobListProps.getProperty("prometheus.port", "3013"));

        // Email (mail.* keys)
        smtpHost = emailProps.getProperty("mail.smtp.host");
        smtpPort = emailProps.getProperty("mail.smtp.port");
        smtpUsername = emailProps.getProperty("mail.username");
        smtpPassword = emailProps.getProperty("mail.password");

        fromEmail = emailProps.getProperty("mail.from");
        toEmails = emailProps.getProperty("mail.to");
        bccEmails = emailProps.getProperty("mail.bcc", "");

        emailImportance = emailProps.getProperty("mail.importance", "Normal");
        smtpAuth = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        smtpStartTls = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));

        // Always allow sending alerts unless explicitly disabled
        sendEmailAlerts = true;
    }

    // --- Getters ---
    public String getIbmiHost() {
        return ibmiHost;
    }

    public String getIbmiUser() {
        return ibmiUser;
    }

    public String getIbmiPassword() {
        return ibmiPassword;
    }

    public List<JobDefinition> getJobsToMonitor() {
        return jobsToMonitor;
    }

    public long getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public int getPrometheusPort() {
        return prometheusPort;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public boolean isStartTls() {
        return smtpStartTls;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getToEmails() {
        return toEmails;
    }

    public String getBccEmails() {
        return bccEmails;
    }

    public String getEmailImportance() {
        return emailImportance;
    }

    public boolean isSendEmailAlerts() {
        return sendEmailAlerts;
    }

    public static class JobDefinition {
        private final String jobName;
        private final String jobUser;
        private final String expectedSubsystem;

        public JobDefinition(String jobName, String jobUser, String expectedSubsystem) {
            this.jobName = jobName;
            this.jobUser = jobUser;
            this.expectedSubsystem = expectedSubsystem;
        }

        public String getJobName() {
            return jobName;
        }

        public String getJobUser() {
            return jobUser;
        }

        public String getExpectedSubsystem() {
            return expectedSubsystem;
        }

        @Override
        public String toString() {
            return jobName + "/" + jobUser + ((expectedSubsystem != null) ? " in " + expectedSubsystem : "");
        }
    }
}
