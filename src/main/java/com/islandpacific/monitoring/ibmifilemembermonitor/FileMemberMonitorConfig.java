package com.islandpacific.monitoring.ibmifilemembermonitor;

import com.islandpacific.monitoring.common.CredentialProtector;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMemberMonitorConfig {

    private static final Logger logger = Logger.getLogger(FileMemberMonitorConfig.class.getName());

    private Properties fileMemberMonitorProps;
    private Properties emailProps; // Store email properties as a separate object

    private Map<String, Long> memberSpecificBreachThresholds;
    private Set<String> allUniqueMonitoredMembers;

    public FileMemberMonitorConfig(String fileMemberMonitorConfigFile, String emailConfigFile) throws IOException {
        fileMemberMonitorProps = new Properties();
        emailProps = new Properties();

        try (FileInputStream fis = new FileInputStream(fileMemberMonitorConfigFile)) {
            fileMemberMonitorProps.load(fis);
            logger.info("Loaded ibmfilemembermonitor.properties: " + fileMemberMonitorConfigFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load ibmfilemembermonitor.properties: " + fileMemberMonitorConfigFile, e);
            throw e;
        }

        try (FileInputStream fis = new FileInputStream(emailConfigFile)) {
            emailProps.load(fis);
            logger.info("Loaded email.properties: " + emailConfigFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load email.properties: " + emailConfigFile, e);
            throw e;
        }

        this.memberSpecificBreachThresholds = parseMonitoredFileMembersWithThresholds();
        this.allUniqueMonitoredMembers = new HashSet<>(memberSpecificBreachThresholds.keySet());

        validateConfiguration();
    }

    private Map<String, Long> parseMonitoredFileMembersWithThresholds() {
        Map<String, Long> thresholds = new HashMap<>();
        String membersConfigString = fileMemberMonitorProps.getProperty("files.members.to.monitor");

        if (membersConfigString == null || membersConfigString.trim().isEmpty()) {
            return thresholds;
        }

        for (String entry : membersConfigString.split(";")) {
            String trimmedEntry = entry.trim();
            String[] parts = trimmedEntry.split(",");

            if (parts.length == 4) {
                String library = parts[0].trim();
                String fileName = parts[1].trim();
                String memberName = parts[2].trim();
                String fullMemberPath = library + "/" + fileName + "/" + memberName;

                try {
                    long threshold = Long.parseLong(parts[3].trim());
                    if (threshold <= 0) {
                        logger.warning("Configured threshold for '" + fullMemberPath + "' is not positive: " + parts[3] + ". Skipping.");
                    } else {
                        thresholds.put(fullMemberPath, threshold);
                    }
                } catch (NumberFormatException e) {
                    logger.warning("Invalid threshold value for member '" + fullMemberPath + "': " + parts[3] + ". Skipping. Error: " + e.getMessage());
                }
            } else {
                logger.warning("Invalid format for 'files.members.to.monitor' entry: '" + trimmedEntry + "'. Expected format: LIBRARY,FILE,MEMBER,THRESHOLD. Skipping.");
            }
        }
        return thresholds;
    }

    private void validateConfiguration() {
        String ibmiHost = getIbmiHost();
        if (ibmiHost == null || ibmiHost.trim().isEmpty()) {
            logger.warning("Configuration 'ibmi.host' is missing or empty.");
        }

        try {
            long ms = getMonitorIntervalMs();
            if (ms <= 0) {
                logger.warning("Configuration 'monitor.interval.ms' must be a positive value. Using default.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Configuration 'monitor.interval.ms' is not a valid number. Using default.");
        }

        if (emailProps.getProperty("mail.to", "").isEmpty() && emailProps.getProperty("mail.bcc", "").isEmpty()) {
            logger.warning("No recipient emails configured (mail.to or mail.bcc). Email alerts will not be sent.");
        }
        String authMethod = emailProps.getProperty("mail.auth.method", "SMTP");
        if ("SMTP".equalsIgnoreCase(authMethod) &&
                (emailProps.getProperty("mail.smtp.host") == null || emailProps.getProperty("mail.smtp.host").trim().isEmpty())) {
            logger.warning("SMTP host (mail.smtp.host) is not configured. Email sending may fail.");
        }

        if (allUniqueMonitoredMembers.isEmpty()) {
            logger.warning("No file members configured via 'files.members.to.monitor'. Monitor will start but skip checks until configured.");
        } else {
            logger.info("Found " + allUniqueMonitoredMembers.size() + " unique file members configured for monitoring.");
        }
    }

    // IBM i Connection Properties
    public String getIbmiHost() { return fileMemberMonitorProps.getProperty("ibmi.host"); }
    public String getIbmiUser() { return fileMemberMonitorProps.getProperty("ibmi.user"); }
    public String getIbmiPassword() {
        String raw = fileMemberMonitorProps.getProperty("ibmi.password");
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Required property 'ibmi.password' is missing or empty.");
        }
        return CredentialProtector.resolve(raw);
    }

    // File Member Monitor Properties
    public boolean isFileMemberMonitorEnabled() { return Boolean.parseBoolean(fileMemberMonitorProps.getProperty("filemembermonitor.enabled", "true")); }
    public long getMonitorIntervalMs() { return Long.parseLong(fileMemberMonitorProps.getProperty("monitor.interval.ms", "300000")); }

    public List<String> getMonitoredFileMembers() { return List.copyOf(allUniqueMonitoredMembers); }
    public Map<String, Long> getMemberSpecificBreachThresholds() { return this.memberSpecificBreachThresholds; }
    public Set<String> getAllUniqueMonitoredMembers() { return this.allUniqueMonitoredMembers; }

    public long getEffectiveBreachThreshold(String fullMemberPath) {
        return memberSpecificBreachThresholds.getOrDefault(fullMemberPath, Long.MAX_VALUE);
    }

    public long getDefaultThreshold() {
        return Long.parseLong(fileMemberMonitorProps.getProperty("default.threshold", "1000000"));
    }

    // Prometheus Metrics Properties
    public int getPrometheusFileMemberPort() { return Integer.parseInt(fileMemberMonitorProps.getProperty("metrics.port", "8080")); }

    // NEW: Method to return the email properties object
    public Properties getEmailProperties() {
        return this.emailProps;
    }

    public Properties getFileMemberProps() {
        return this.fileMemberMonitorProps;
    }

    // These methods are now redundant as EmailService will get properties directly from the Properties object
    // Keeping them for backward compatibility if other parts of the code still use them.
    public String getSmtpHost() { return emailProps.getProperty("mail.smtp.host"); }
    public int getSmtpPort() { return Integer.parseInt(emailProps.getProperty("mail.smtp.port", "587")); }
    public String getSmtpUsername() { return emailProps.getProperty("mail.smtp.user"); }
    public String getSmtpPassword() { return CredentialProtector.resolve(emailProps.getProperty("mail.smtp.password")); }
    public String getFromEmail() { return emailProps.getProperty("mail.from"); }
    public List<String> getToEmails() { String to = emailProps.getProperty("mail.to"); return to == null || to.trim().isEmpty() ? List.of() : Arrays.asList(to.split(",")); }
    public List<String> getBccEmails() { String bcc = emailProps.getProperty("mail.bcc"); return bcc == null || bcc.trim().isEmpty() ? List.of() : Arrays.asList(bcc.split(",")); }
    public String getEmailImportance() { return emailProps.getProperty("mail.importance", "normal"); }
    public boolean isSmtpAuthEnabled() { return Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "true")); }
    public boolean isSmtpStartTlsEnabled() { return Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "true")); }

    public String getClientName() { return fileMemberMonitorProps.getProperty("client.name", emailProps.getProperty("mail.clientName", "")); }
}
