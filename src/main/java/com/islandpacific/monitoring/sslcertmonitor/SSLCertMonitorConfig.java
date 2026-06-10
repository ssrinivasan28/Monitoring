package com.islandpacific.monitoring.sslcertmonitor;

import java.util.*;

public class SSLCertMonitorConfig {

    private List<HostEntry> hosts;
    private List<Integer> warningThresholds;
    private long monitorIntervalMs;
    private int metricsPort;
    private String clientName;

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

    private String logLevel;
    private String logFolder;
    private int logRetentionDays;
    private int logPurgeIntervalHours;

    public static class HostEntry {
        public final String host;
        public final int port;

        public HostEntry(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    public static SSLCertMonitorConfig fromProperties(Properties appProps, Properties emailProps) {
        SSLCertMonitorConfig cfg = new SSLCertMonitorConfig();

        cfg.hosts = new ArrayList<>();
        String hostsRaw = appProps.getProperty("ssl.hosts", "");
        for (String entry : hostsRaw.split(",")) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;
            if (entry.contains(":")) {
                String[] parts = entry.split(":", 2);
                cfg.hosts.add(new HostEntry(parts[0].trim(), Integer.parseInt(parts[1].trim())));
            } else {
                cfg.hosts.add(new HostEntry(entry, 443));
            }
        }

        cfg.warningThresholds = new ArrayList<>();
        String thresholdsRaw = appProps.getProperty("ssl.warning.days", "30,15,7,3");
        for (String t : thresholdsRaw.split(",")) {
            t = t.trim();
            if (!t.isEmpty()) cfg.warningThresholds.add(Integer.parseInt(t));
        }
        cfg.warningThresholds.sort(java.util.Collections.reverseOrder());
        cfg.clientName = appProps.getProperty("client.name", "");
        cfg.monitorIntervalMs = Long.parseLong(appProps.getProperty("monitor.interval.ms", "86400000"));
        cfg.metricsPort = Integer.parseInt(appProps.getProperty("metrics.port", "3027"));

        cfg.logLevel = appProps.getProperty("log.level", "INFO");
        cfg.logFolder = appProps.getProperty("log.folder", "logs");
        cfg.logRetentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
        cfg.logPurgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));

        cfg.authMethod = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
        cfg.emailHost = emailProps.getProperty("mail.smtp.host");
        cfg.emailPort = emailProps.getProperty("mail.smtp.port", "25");
        cfg.emailFrom = emailProps.getProperty("mail.from");
        cfg.emailTo = emailProps.getProperty("mail.to");
        cfg.emailBcc = emailProps.getProperty("mail.bcc", "");
        cfg.emailUsername = emailProps.getProperty("mail.smtp.username", "");
        cfg.emailPassword = emailProps.getProperty("mail.smtp.password", "");
        cfg.emailAuthEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));
        cfg.emailStartTlsEnabled = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.starttls.enable", "false"));
        cfg.emailImportance = emailProps.getProperty("mail.importance", "High");
        cfg.oauth2TenantId = emailProps.getProperty("mail.oauth2.tenant.id", "");
        cfg.oauth2ClientId = emailProps.getProperty("mail.oauth2.client.id", "");
        cfg.oauth2ClientSecret = emailProps.getProperty("mail.oauth2.client.secret", "");
        cfg.oauth2TokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");
        cfg.graphMailUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");

        if (cfg.hosts.isEmpty())
            throw new IllegalArgumentException("ssl.hosts must specify at least one host");
        if (cfg.emailFrom == null || cfg.emailFrom.isEmpty())
            throw new IllegalArgumentException("mail.from is required");
        if (cfg.emailTo == null || cfg.emailTo.isEmpty())
            throw new IllegalArgumentException("mail.to is required");
        if ("OAUTH2".equals(cfg.authMethod)) {
            if (cfg.oauth2TenantId.isEmpty()) throw new IllegalArgumentException("mail.oauth2.tenant.id is required when mail.auth.method=OAUTH2");
            if (cfg.oauth2ClientId.isEmpty()) throw new IllegalArgumentException("mail.oauth2.client.id is required when mail.auth.method=OAUTH2");
            if (cfg.oauth2ClientSecret.isEmpty()) throw new IllegalArgumentException("mail.oauth2.client.secret is required when mail.auth.method=OAUTH2");
        }

        return cfg;
    }

    public List<HostEntry> getHosts() { return hosts; }
    public List<Integer> getWarningThresholds() { return warningThresholds; }
    public String getClientName() { return clientName; }
    public long getMonitorIntervalMs() { return monitorIntervalMs; }
    public int getMetricsPort() { return metricsPort; }
    public String getAuthMethod() { return authMethod; }
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
    public String getOauth2TenantId() { return oauth2TenantId; }
    public String getOauth2ClientId() { return oauth2ClientId; }
    public String getOauth2ClientSecret() { return oauth2ClientSecret; }
    public String getOauth2TokenUrl() { return oauth2TokenUrl; }
    public String getGraphMailUrl() { return graphMailUrl; }
    public String getLogLevel() { return logLevel; }
    public String getLogFolder() { return logFolder; }
    public int getLogRetentionDays() { return logRetentionDays; }
    public int getLogPurgeIntervalHours() { return logPurgeIntervalHours; }
}
