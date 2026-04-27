package com.islandpacific.monitoring.serveruptime; // New package for this app

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainServerUpTimeMonitorApp {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    // Prometheus Gauge for server status (1 = up, 0 = down)
    private static final Gauge SERVER_STATUS = Gauge.build()
            .name("server_status")
            .help("Server status (1 for up, 0 for down).")
            .labelNames("server")
            .register();

    private static Properties serverInfoProperties = new Properties(); // For serverinfo.properties
    private static Properties emailProperties = new Properties(); // For email.properties

    private static List<String> serversToMonitor;
    private static int exporterPort;
    private static int pingIntervalSeconds;

    // Map to store the current status of each server (true = up, false = down)
    private static final Map<String, Boolean> currentServerStatus = new java.util.concurrent.ConcurrentHashMap<>();

    private static ScheduledExecutorService scheduler;

    private static EmailService emailService; // Instance of the EmailService
    private static ServerUptimeService uptimeService;
    private static HTTPServer prometheusServer;

    public static void main(String[] args) {
        try {
            loadConfiguration(args); // Load configuration first to read log.level
            setupLogger(); // Initialize logger
            
            // Start automatic log purge (optional - configure via properties)
            int retentionDays = Integer.parseInt(emailProperties.getProperty("log.retention.days", 
                serverInfoProperties.getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(emailProperties.getProperty("log.purge.interval.hours",
                serverInfoProperties.getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
            
            initializeMetricsAndInitialStatus(); // NEW method for initial ping and alerts
            startPrometheusExporter();
            schedulePingMonitoring();

            logger.info("Server Downtime Monitor App started. Metrics exposed on port " + exporterPort + "/metrics");
            logger.info("Monitoring servers: " + serversToMonitor);

            // Add a shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Server Downtime Monitor App gracefully...");
                if (scheduler != null) {
                    scheduler.shutdown();
                    try {
                        if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) scheduler.shutdownNow();
                    } catch (InterruptedException ie) {
                        scheduler.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
                if (prometheusServer != null) prometheusServer.close();
                logger.info("Server Downtime Monitor App shutdown complete.");
            }));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error starting application due to I/O: " + e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Application configuration error: " + e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred during application startup: " + e.getMessage(), e);
            e.printStackTrace(); // Print stack trace for unexpected errors
            System.exit(1);
        }
    }

    private static void setupLogger() throws IOException {
        String logLevel = serverInfoProperties.getProperty("log.level",
                emailProperties.getProperty("log.level", "INFO"));
        String logFolder = serverInfoProperties.getProperty("log.folder",
                emailProperties.getProperty("log.folder", "logs"));
        com.islandpacific.monitoring.common.AppLogger.setupLogger("serveruptime", logLevel, logFolder);
    }

    private static void loadConfiguration(String[] args) throws IOException {
        String serverInfoFilePath = "serverinfo.properties";
        String emailFilePath = "email.properties";

        if (args.length >= 1) {
            serverInfoFilePath = args[0];
        }
        if (args.length >= 2) {
            emailFilePath = args[1];
        }

        // Load serverinfo.properties
        try (FileInputStream fis = new FileInputStream(serverInfoFilePath)) {
            serverInfoProperties.load(fis);
            logger.info("Loaded server information properties from: " + serverInfoFilePath);
        } catch (FileNotFoundException e) {
            logger.severe("serverinfo.properties not found. Ensure '" + serverInfoFilePath + "' exists. Error: " + e.getMessage());
            throw new IllegalArgumentException("Missing server information properties file: " + e.getMessage());
        }

        // Load email.properties
        try (FileInputStream fis = new FileInputStream(emailFilePath)) {
            emailProperties.load(fis);
            logger.info("Loaded email properties from: " + emailFilePath);
        } catch (FileNotFoundException e) {
            logger.severe("email.properties not found. Ensure '" + emailFilePath + "' exists. Error: " + e.getMessage());
            throw new IllegalArgumentException("Missing email properties file: " + e.getMessage());
        }

        exporterPort = Integer.parseInt(serverInfoProperties.getProperty("exporter.port", "9091"));
        pingIntervalSeconds = Integer.parseInt(serverInfoProperties.getProperty("ping.interval.seconds", "10"));
        
        String serversListString = serverInfoProperties.getProperty("servers.list");
        if (serversListString == null || serversListString.trim().isEmpty()) {
             throw new IllegalArgumentException("Required property 'servers.list' is missing or empty in serverinfo.properties.");
        }
        serversToMonitor = Arrays.asList(serversListString.split(","));
        
        if (serversToMonitor.isEmpty() || serversToMonitor.get(0).trim().isEmpty()) {
            throw new IllegalArgumentException("No valid servers specified in 'servers.list' in serverinfo.properties. It might be empty after splitting.");
        }

        // Get monitor host name for email template (NEW)
        String monitorHostName = serverInfoProperties.getProperty("monitor.host.name", "Unknown Host");

        // Determine authentication method
        String authMethod = emailProperties.getProperty("mail.auth.method", "SMTP").toUpperCase();
        
        // Initialize EmailService
        String mailHost = "SMTP".equals(authMethod) ? getRequiredProperty(emailProperties, "mail.smtp.host")
                                                     : emailProperties.getProperty("mail.smtp.host", "");
        String mailPort = emailProperties.getProperty("mail.smtp.port", "25");
        String mailFrom = getRequiredProperty(emailProperties, "mail.from");
        String mailTo = getRequiredProperty(emailProperties, "mail.to");
        String mailBcc = emailProperties.getProperty("mail.bcc", "");
        String mailUsername = emailProperties.getProperty("mail.smtp.username", "");
        String mailPassword = emailProperties.getProperty("mail.smtp.password", "");
        boolean mailAuthEnabled = Boolean.parseBoolean(emailProperties.getProperty("mail.smtp.auth", "false"));
        boolean mailStartTlsEnabled = Boolean.parseBoolean(emailProperties.getProperty("mail.smtp.starttls.enable", "false"));
        String mailImportance = emailProperties.getProperty("mail.importance", "Normal");
        
        // OAuth2 configuration
        OAuth2TokenProvider oauth2Provider = null;
        String graphMailUrl = null;
        String fromUser = null;
        
        if ("OAUTH2".equals(authMethod)) {
            String tenantId = getRequiredProperty(emailProperties, "mail.oauth2.tenant.id");
            String clientId = getRequiredProperty(emailProperties, "mail.oauth2.client.id");
            String clientSecret = getRequiredProperty(emailProperties, "mail.oauth2.client.secret");
            String scope = emailProperties.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProperties.getProperty("mail.oauth2.token.url", "");
            
            oauth2Provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
            fromUser = emailProperties.getProperty("mail.oauth2.from.user", mailFrom.replaceAll(".*<([^>]+)>.*", "$1").trim());
            
            // Build Graph API URL from fromUser if not explicitly provided, or use provided URL
            String providedGraphUrl = emailProperties.getProperty("mail.oauth2.graph.mail.url", "");
            if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                graphMailUrl = providedGraphUrl.trim();
            } else {
                // Build URL from fromUser email address
                graphMailUrl = "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
                logger.info("Built Graph API URL from fromUser: " + graphMailUrl);
            }
            
            logger.info("OAuth2 authentication configured for email service.");
        }

        emailService = new EmailService(mailHost, mailPort, mailFrom, mailTo, mailBcc,
                                        mailUsername, mailPassword, mailAuthEnabled, mailStartTlsEnabled, mailImportance,
                                        monitorHostName, authMethod, oauth2Provider, graphMailUrl, fromUser);
        uptimeService = new ServerUptimeService(logger, serversToMonitor, emailService,
                                                SERVER_STATUS, currentServerStatus);
    }

    private static String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required property '" + key + "' is missing or empty.");
        }
        return value.trim();
    }


    private static void initializeMetricsAndInitialStatus() {
        uptimeService.initializeStatus();
    }

    private static void startPrometheusExporter() throws IOException {
        prometheusServer = new HTTPServer(exporterPort);
    }

    private static void schedulePingMonitoring() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(uptimeService::checkAndAlert, 0, pingIntervalSeconds, TimeUnit.SECONDS);
    }
}