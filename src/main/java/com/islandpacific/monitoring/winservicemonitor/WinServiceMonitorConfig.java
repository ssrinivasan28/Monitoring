package com.islandpacific.monitoring.winservicemonitor;

import java.util.*;

public class WinServiceMonitorConfig {

    private List<String> servers;
    private List<String> services;                          // fallback: applied to all servers
    private Map<String, List<String>> serverServices = new HashMap<>(); // per-server override
    private int monitorIntervalSeconds;
    private int metricsPort;
    private int pollThreads;
    private int alertWindowSize;
    private Map<String, Credentials> serverCredentials = new HashMap<>();

    // Email config
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

    // Log config
    private String logLevel;
    private String logFolder;
    private int logRetentionDays;
    private int logPurgeIntervalHours;

    public static class Credentials {
        public final String username;
        public final String password;

        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static WinServiceMonitorConfig fromProperties(Properties appProps, Properties emailProps) {
        WinServiceMonitorConfig cfg = new WinServiceMonitorConfig();

        String serversList = appProps.getProperty("monitor.servers", "localhost");
        cfg.servers = new ArrayList<>();
        for (String s : serversList.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) cfg.servers.add(trimmed);
        }

        String servicesList = appProps.getProperty("monitor.services", "");
        cfg.services = new ArrayList<>();
        for (String s : servicesList.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) cfg.services.add(trimmed);
        }

        cfg.monitorIntervalSeconds = Integer.parseInt(appProps.getProperty("monitor.interval.seconds", "60"));
        cfg.metricsPort = Integer.parseInt(appProps.getProperty("metrics.port", "3026"));
        cfg.pollThreads = Integer.parseInt(appProps.getProperty("monitor.poll.threads", "5"));
        cfg.alertWindowSize = Integer.parseInt(appProps.getProperty("monitor.alert.window.size", "2"));

        cfg.logLevel = appProps.getProperty("log.level", "INFO");
        cfg.logFolder = appProps.getProperty("log.folder", "logs");
        cfg.logRetentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
        cfg.logPurgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));

        // Per-server credentials and per-server service overrides
        for (String server : cfg.servers) {
            String user = appProps.getProperty("monitor.server." + server + ".username");
            String pass = appProps.getProperty("monitor.server." + server + ".password");
            if (user != null && pass != null) {
                cfg.serverCredentials.put(server, new Credentials(user.trim(), pass.trim()));
            }

            String perServerServices = appProps.getProperty("monitor.server." + server + ".services");
            if (perServerServices != null && !perServerServices.trim().isEmpty()) {
                List<String> list = new ArrayList<>();
                for (String s : perServerServices.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) list.add(trimmed);
                }
                cfg.serverServices.put(server, list);
            }
        }

        // Email
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

        // Validate required fields
        if (cfg.servers.isEmpty())
            throw new IllegalArgumentException("monitor.servers must specify at least one server");
        if (cfg.services.isEmpty() && cfg.serverServices.isEmpty())
            throw new IllegalArgumentException("monitor.services (or per-server overrides) must specify at least one service");
        for (String server : cfg.servers) {
            if (cfg.getServicesForServer(server).isEmpty())
                throw new IllegalArgumentException(
                        "No services configured for server '" + server + "'. Add monitor.services or monitor.server." + server + ".services");
        }
        if ("SMTP".equals(cfg.authMethod) && (cfg.emailHost == null || cfg.emailHost.isEmpty()))
            throw new IllegalArgumentException("mail.smtp.host is required when mail.auth.method=SMTP");
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

    public List<String> getServers() { return servers; }
    public List<String> getServices() { return services; }
    public List<String> getServicesForServer(String server) {
        return serverServices.getOrDefault(server, services);
    }
    public int getMonitorIntervalSeconds() { return monitorIntervalSeconds; }
    public int getMetricsPort() { return metricsPort; }
    public int getPollThreads() { return pollThreads; }
    public int getAlertWindowSize() { return alertWindowSize; }
    public Credentials getCredentialsForServer(String server) { return serverCredentials.get(server); }
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
