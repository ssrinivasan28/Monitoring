package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.islandpacific.monitoring.common.CredentialProtector;

public class QSYSOPRMonitorConfig {

    private String emailPropertiesFile = "email.properties";
    private String jobFailurePropertiesFile = "job_failure.properties";

    // IBM i Connection Details
    private String ibmiHost;
    private String ibmiUser;
    private String ibmiPassword;
    private String dbUrl;

    private Properties mailProperties;
    private Properties monitorProperties;

    private Set<String> jobFailureMessageIds;
    private Set<String> jobFailureKeywords;

    private long monitorIntervalMillis;
    private int metricsPort;
    private String stateFileName;
    private String clientName;

    public QSYSOPRMonitorConfig() {}

    public QSYSOPRMonitorConfig(String emailPropertiesFile, String jobFailurePropertiesFile) {
        this.emailPropertiesFile = emailPropertiesFile;
        this.jobFailurePropertiesFile = jobFailurePropertiesFile;
    }

    public boolean loadConfigurations() {
        try {
            mailProperties = new Properties();
            mailProperties.load(new FileInputStream(emailPropertiesFile));

            Properties jobFailureProps = new Properties();
            jobFailureProps.load(new FileInputStream(jobFailurePropertiesFile));
            this.monitorProperties = jobFailureProps;

            this.ibmiHost = jobFailureProps.getProperty("ibmi.host");
            this.ibmiUser = jobFailureProps.getProperty("ibmi.user");
            String ibmiPasswordRaw = jobFailureProps.getProperty("ibmi.password");
            if (ibmiPasswordRaw == null || ibmiPasswordRaw.trim().isEmpty()) {
                System.err.println("Required property 'ibmi.password' is missing or empty.");
                return false;
            }
            this.ibmiPassword = CredentialProtector.resolve(ibmiPasswordRaw);
            this.dbUrl = "jdbc:as400://" + ibmiHost + "/QSYS;naming=system";

            this.monitorIntervalMillis = Long.parseLong(jobFailureProps.getProperty("monitor.interval.ms", "60000"));
            this.metricsPort = Integer.parseInt(jobFailureProps.getProperty("metrics.port", "3019"));
            this.stateFileName = jobFailureProps.getProperty("state.file.name", "last_checked_timestamp.txt");
            this.clientName = jobFailureProps.getProperty("client.name", mailProperties.getProperty("mail.clientName", ""));

            this.jobFailureMessageIds = new HashSet<>(Arrays.asList(
                jobFailureProps.getProperty("job.failure.message.ids", "").split(",")));
            this.jobFailureKeywords = new HashSet<>(Arrays.asList(
                jobFailureProps.getProperty("job.failure.keywords", "").split(",")));
            this.jobFailureMessageIds.removeIf(String::isEmpty);
            this.jobFailureKeywords.removeIf(String::isEmpty);

            if (ibmiHost == null || ibmiHost.trim().isEmpty() ||
                ibmiUser == null || ibmiUser.trim().isEmpty()) {
                System.err.println("Missing required IBM i connection properties (ibmi.host, ibmi.user).");
                return false;
            }
            if (mailProperties.getProperty("mail.from") == null || mailProperties.getProperty("mail.to") == null) {
                System.err.println("Missing required email properties (mail.from, mail.to).");
                return false;
            }
            if (jobFailureMessageIds.isEmpty() && jobFailureKeywords.isEmpty()) {
                System.err.println("At least one of job.failure.message.ids or job.failure.keywords must be configured.");
                return false;
            }

            return true;
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading configuration files: " + e.getMessage());
            return false;
        }
    }

    public String getIbmiHost() { return ibmiHost; }
    public String getIbmiUser() { return ibmiUser; }
    public String getIbmiPassword() { return ibmiPassword; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUsername() { return ibmiUser; }
    public String getDbPassword() { return ibmiPassword; }
    public Properties getMailProperties() { return mailProperties; }
    public Properties getMonitorProperties() { return monitorProperties; }
    public Set<String> getJobFailureMessageIds() { return jobFailureMessageIds; }
    public Set<String> getJobFailureKeywords() { return jobFailureKeywords; }
    public long getMonitorIntervalMillis() { return monitorIntervalMillis; }
    public int getMetricsPort() { return metricsPort; }
    public String getStateFileName() { return stateFileName; }
    public String getClientName() { return clientName; }
}
