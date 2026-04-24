package com.islandpacific.monitoring.windowsmonitoring;

import java.util.*;

public class WindowsMonitorConfig {
    private List<String> hosts;
    private double cpuAlertThreshold;
    private double memoryAlertThreshold;
    private double diskAlertThreshold;
    private int topNProcesses;
    private int monitorIntervalMs;
    private int metricsPort;
    private int pollThreads;
    private int alertWindowSize;
    private List<String> servicesToMonitor;
    private Map<String, Credentials> hostCredentials = new HashMap<>();

    public static class Credentials {
        public final String username;
        public final String password;

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    // Email Config
    private String authMethod;
    private String emailHost;
    private String emailPort;
    private String emailFrom;
    private String emailTo;
    private String emailBcc;
    private String emailUsername;
    private String emailPassword;
    private boolean emailAuthEnabled;
    private boolean emailStartTlsEnabled;
    private String emailImportance;
    private String oauth2TenantId;
    private String oauth2ClientId;
    private String oauth2ClientSecret;
    private String oauth2TokenUrl;
    private String graphMailUrl;

    public static WindowsMonitorConfig fromProperties(Properties appProps, Properties emailProps) {
        WindowsMonitorConfig config = new WindowsMonitorConfig();

        String hostsList = appProps.getProperty("windows.servers.list", "localhost");
        config.hosts = Arrays.asList(hostsList.split(","));

        config.cpuAlertThreshold = Double.parseDouble(appProps.getProperty("windows.alert.threshold.cpu", "90"));
        config.memoryAlertThreshold = Double.parseDouble(appProps.getProperty("windows.alert.threshold.memory", "90"));
        config.diskAlertThreshold = Double.parseDouble(appProps.getProperty("windows.alert.threshold.disk", "90"));
        config.topNProcesses = Integer.parseInt(appProps.getProperty("windows.top.n.processes", "5"));

        int intervalSeconds = Integer.parseInt(appProps.getProperty("windows.monitor.interval.seconds", "300"));
        config.monitorIntervalMs = intervalSeconds * 1000;

        config.metricsPort = Integer.parseInt(appProps.getProperty("metrics.exporter.port", "3017"));

        config.pollThreads = Integer.parseInt(appProps.getProperty("windows.poll.threads", "5"));
        config.alertWindowSize = Integer.parseInt(appProps.getProperty("windows.alert.window.size", "3"));
        String servicesList = appProps.getProperty("windows.services.to.monitor", "");
        config.servicesToMonitor = servicesList.isEmpty() ? new ArrayList<>() : Arrays.asList(servicesList.split(","));

        // Email properties
        config.authMethod = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
        config.emailHost = emailProps.getProperty("mail.smtp.host");
        config.emailPort = emailProps.getProperty("mail.smtp.port", "25");
        config.emailFrom = emailProps.getProperty("mail.from");
        config.emailTo = emailProps.getProperty("mail.to");
        config.emailBcc = emailProps.getProperty("mail.bcc", "");
        config.emailUsername = emailProps.getProperty("mail.smtp.username", "");
        config.emailPassword = emailProps.getProperty("mail.smtp.password", "");
        config.emailAuthEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        config.emailStartTlsEnabled = Boolean
                .parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));
        config.emailImportance = emailProps.getProperty("mail.importance", "Normal");
        config.oauth2TenantId = emailProps.getProperty("mail.oauth2.tenant.id", "");
        config.oauth2ClientId = emailProps.getProperty("mail.oauth2.client.id", "");
        config.oauth2ClientSecret = emailProps.getProperty("mail.oauth2.client.secret", "");
        config.oauth2TokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");
        config.graphMailUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");

        // Parse per-host credentials
        for (String host : config.hosts) {
            String user = appProps.getProperty("windows.server." + host + ".username");
            String pass = appProps.getProperty("windows.server." + host + ".password");
            if (user != null && pass != null) {
                config.hostCredentials.put(host, new Credentials(user, pass));
            }
        }

        return config;
    }

    // Getters
    public List<String> getHosts() {
        return hosts;
    }

    public double getCpuAlertThreshold() {
        return cpuAlertThreshold;
    }

    public double getMemoryAlertThreshold() {
        return memoryAlertThreshold;
    }

    public double getDiskAlertThreshold() {
        return diskAlertThreshold;
    }

    public int getTopNProcesses() {
        return topNProcesses;
    }

    public int getMonitorIntervalMs() {
        return monitorIntervalMs;
    }

    public int getMetricsPort() {
        return metricsPort;
    }

    public int getPollThreads() {
        return pollThreads;
    }

    public int getAlertWindowSize() {
        return alertWindowSize;
    }

    public List<String> getServicesToMonitor() {
        return servicesToMonitor;
    }

    public String getAuthMethod() { return authMethod; }
    public String getOauth2TenantId() { return oauth2TenantId; }
    public String getOauth2ClientId() { return oauth2ClientId; }
    public String getOauth2ClientSecret() { return oauth2ClientSecret; }
    public String getOauth2TokenUrl() { return oauth2TokenUrl; }
    public String getGraphMailUrl() { return graphMailUrl; }

    public String getEmailHost() {
        return emailHost;
    }

    public String getEmailPort() {
        return emailPort;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getEmailBcc() {
        return emailBcc;
    }

    public String getEmailUsername() {
        return emailUsername;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public boolean isEmailAuthEnabled() {
        return emailAuthEnabled;
    }

    public boolean isEmailStartTlsEnabled() {
        return emailStartTlsEnabled;
    }

    public String getEmailImportance() {
        return emailImportance;
    }

    public Credentials getCredentialsForHost(String host) {
        return hostCredentials.get(host);
    }
}
