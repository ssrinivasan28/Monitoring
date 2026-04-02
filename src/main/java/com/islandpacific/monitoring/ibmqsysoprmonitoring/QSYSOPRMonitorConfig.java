package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Manages all application configurations, loading them from properties files.
 */
public class QSYSOPRMonitorConfig {

    private static final String MAIL_PROPERTIES_FILE = "email.properties";
    private static final String JOB_FAILURE_PROPERTIES_FILE = "job_failure.properties";

    // IBM i Connection Details
    private String ibmiHost;
    private String ibmiUser;
    private String ibmiPassword;
    private String dbUrl; // Derived from ibmi.host

    // Email Configuration
    private Properties mailProperties; // Keep original properties for EmailService

    // Message Filtering Criteria
    private Set<String> jobFailureMessageIds;
    private Set<String> jobFailureKeywords;

    // Monitor Interval (in milliseconds)
    private long monitorIntervalMillis;

    // Metrics Port
    private int metricsPort;

    // State file name
    private String stateFileName;

    // Logging Configuration
    private String logFolder;
    private String logLevel;

    // New: Client Name
    private String clientName; // Added field for client name

    /**
     * Loads all configurations from mail.properties and job_failure.properties.
     *
     * @return true if configurations are loaded successfully, false otherwise.
     */
    public boolean loadConfigurations() {
        try {
            // Load Mail Properties
            mailProperties = new Properties();
            mailProperties.load(new FileInputStream(MAIL_PROPERTIES_FILE));

            // Load Job Failure Properties
            Properties jobFailureProps = new Properties();
            jobFailureProps.load(new FileInputStream(JOB_FAILURE_PROPERTIES_FILE));

            // IBM i Connection Details
            this.ibmiHost = jobFailureProps.getProperty("ibmi.host");
            this.ibmiUser = jobFailureProps.getProperty("ibmi.user");
            this.ibmiPassword = jobFailureProps.getProperty("ibmi.password");
            // Construct the JDBC URL using the ibmi.host
            this.dbUrl = "jdbc:as400://" + ibmiHost + "/QSYS;naming=system";

            // Monitor Interval
            this.monitorIntervalMillis = Long.parseLong(jobFailureProps.getProperty("monitor.interval.ms", "60000")); // Default: 60000ms (1 minute)

            // Metrics Port
            this.metricsPort = Integer.parseInt(jobFailureProps.getProperty("metrics.port", "3019")); // Default Prometheus port

            // State file name
            this.stateFileName = jobFailureProps.getProperty("state.file.name", "last_checked_timestamp.txt");

            // Logging Configuration
            this.logFolder = jobFailureProps.getProperty("log.folder", "logs");
            this.logLevel = jobFailureProps.getProperty("log.level", "INFO");

            // New: Load Client Name
            this.clientName = jobFailureProps.getProperty("client.name", "UnknownClient"); // Load client name with a default

            // Handle comma-separated lists for message IDs and keywords
            this.jobFailureMessageIds = new HashSet<>(Arrays.asList(
                jobFailureProps.getProperty("job.failure.message.ids", "").split(",")
            ));
            this.jobFailureKeywords = new HashSet<>(Arrays.asList(
                jobFailureProps.getProperty("job.failure.keywords", "").split(",")
            ));

            // Remove any empty strings resulting from split (e.g., if a property is empty)
            this.jobFailureMessageIds.removeIf(String::isEmpty);
            this.jobFailureKeywords.removeIf(String::isEmpty);

            // Basic validation
            if (ibmiHost == null || ibmiUser == null || ibmiPassword == null ||
                mailProperties.getProperty("mail.from") == null || mailProperties.getProperty("mail.to") == null ||
                (jobFailureMessageIds.isEmpty() && jobFailureKeywords.isEmpty())) {
                System.err.println("Missing or incomplete required configuration properties. Please check your properties files.");
                return false;
            }

            return true;
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading configuration files: " + e.getMessage());
            return false;
        }
    }

    // --- Getters for all configuration properties ---
    public String getIbmiHost() {
        return ibmiHost;
    }

    public String getIbmiUser() {
        return ibmiUser;
    }

    public String getIbmiPassword() {
        return ibmiPassword;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return ibmiUser;
    }

    public String getDbPassword() {
        return ibmiPassword;
    }

    public Properties getMailProperties() {
        return mailProperties;
    }

    public Set<String> getJobFailureMessageIds() {
        return jobFailureMessageIds;
    }

    public Set<String> getJobFailureKeywords() {
        return jobFailureKeywords;
    }

    public long getMonitorIntervalMillis() {
        return monitorIntervalMillis;
    }

    public int getMetricsPort() {
        return metricsPort;
    }

    public String getStateFileName() {
        return stateFileName;
    }

    public String getLogFolder() {
        return logFolder;
    }

    public String getLogLevel() {
        return logLevel;
    }

    // New: Getter for client name
    public String getClientName() {
        return clientName;
    }
}