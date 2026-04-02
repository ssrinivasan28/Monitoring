package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.logging.Level;


public class SubsystemMonitorConfig {

    private static final Logger logger = Logger.getLogger(SubsystemMonitorConfig.class.getName());

    // IBM i Connection
    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;
    private final String clientName; // Added clientName

    // Critical Subsystem Names (descriptions only)
    private final List<String> criticalSubsystemNames;

    // Global IBM i System Context Library
    private final String ibmiSystemContextLibrary;

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


    private SubsystemMonitorConfig(String ibmiHost, String ibmiUser, String ibmiPassword,
                                   String clientName, // Added to constructor
                                   List<String> criticalSubsystemNames, String ibmiSystemContextLibrary,
                                   String emailHost, String emailPort, String emailFrom, String emailTo, String emailBcc,
                                   String emailUsername, String emailPassword, boolean emailAuthEnabled, boolean emailStartTlsEnabled, String emailImportance,
                                   int monitorIntervalMs, int metricsPort) {
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
        this.clientName = clientName; // Assign clientName
        this.criticalSubsystemNames = Collections.unmodifiableList(criticalSubsystemNames);
        this.ibmiSystemContextLibrary = ibmiSystemContextLibrary;
        this.emailHost = emailHost;
        this.emailPort = emailPort;
        this.emailFrom = emailFrom; // Corrected assignment
        this.emailTo = emailTo;      // Corrected assignment
        this.emailBcc = emailBcc;    // Corrected assignment
        this.emailUsername = emailUsername;
        this.emailPassword = emailPassword;
        this.emailAuthEnabled = emailAuthEnabled;      // Corrected assignment
        this.emailStartTlsEnabled = emailStartTlsEnabled; // Corrected assignment
        this.emailImportance = emailImportance;        // Corrected assignment
        this.monitorIntervalMs = monitorIntervalMs;
        this.metricsPort = metricsPort;
    }


    public static SubsystemMonitorConfig fromProperties(Properties appProps, Properties emailProps)
            throws IllegalArgumentException, NumberFormatException {

        // Helper for required properties
        BiFunction<Properties, String, String> getRequiredProperty = (properties, key) -> {
            String value = properties.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Required property '" + key + "' is missing or empty.");
            }
            return value.trim();
        };

        // IBM i connection properties (from appProps)
        String ibmiHost = getRequiredProperty.apply(appProps, "ibmi.host");
        String ibmiUser = getRequiredProperty.apply(appProps, "ibmi.user");
        String ibmiPassword = appProps.getProperty("ibmi.password", ""); // Optional password
        String clientName = appProps.getProperty("client.name", ""); // Get client.name

        // Critical Subsystem Names (descriptions only)
        String criticalNamesString = appProps.getProperty("subsystem.critical.names", "");
        List<String> criticalSubsystemNames = new ArrayList<>();
        if (!criticalNamesString.isEmpty()) {
            criticalSubsystemNames = Arrays.asList(criticalNamesString.split(",")).stream()
                                           .map(String::trim)
                                           .filter(s -> !s.isEmpty())
                                           .toList();
        }
        logger.log(Level.INFO, "Configured critical subsystem names: " + criticalSubsystemNames);

        // Global IBM i System Context Library
        String ibmiSystemContextLibrary = getRequiredProperty.apply(appProps, "ibmi.system.context.library");
        logger.log(Level.INFO, "Configured IBM i System Context Library: " + ibmiSystemContextLibrary);


        // Email properties (from emailProps)
        String emailHost = getRequiredProperty.apply(emailProps, "mail.smtp.host");
        String emailPort = emailProps.getProperty("mail.smtp.port", "25");
        String emailFrom = getRequiredProperty.apply(emailProps, "mail.from");
        String emailTo = getRequiredProperty.apply(emailProps, "mail.to"); // This is required
        String emailBcc = emailProps.getProperty("mail.bcc", "");
        String emailUsername = emailProps.getProperty("mail.smtp.username", "");
        String emailPassword = emailProps.getProperty("mail.smtp.password", "");
        boolean emailAuth = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        boolean emailStartTlsEnable = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));
        String emailImportance = emailProps.getProperty("mail.importance", "Normal");

        // Monitor interval and metrics port (from appProps)
        int monitorInterval = Integer.parseInt(appProps.getProperty("monitor.interval.ms", "60000"));
        int metricsPort = Integer.parseInt(appProps.getProperty("metrics.port", "9400")); // Default to 9400 as requested

        return new SubsystemMonitorConfig(
            ibmiHost, ibmiUser, ibmiPassword,
            clientName, // Pass clientName
            criticalSubsystemNames, ibmiSystemContextLibrary, // Pass both
            emailHost, emailPort, emailFrom, emailTo, emailBcc,
            emailUsername, emailPassword, emailAuth, emailStartTlsEnable, emailImportance,
            monitorInterval, metricsPort
        );
    }

    // Getters for all fields
    public String getIbmiHost() { return ibmiHost; }
    public String getIbmiUser() { return ibmiUser; }
    public String getIbmiPassword() { return ibmiPassword; }
    public String getClientName() { return clientName; } // New getter
    public List<String> getCriticalSubsystemNames() { return criticalSubsystemNames; } // Getter for names
    public String getIbmiSystemContextLibrary() { return ibmiSystemContextLibrary; } // Getter for global library
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
}