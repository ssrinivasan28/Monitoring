package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MainSubsystemMonitor {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    // File paths
    private static String appPropertiesFilePath = "subsystem.properties";
    private static String emailPropertiesFilePath = "email.properties";

    // Properties
    private static Properties appProps = new Properties();
    private static Properties emailProps = new Properties();

    // Services
    private static SubsystemMonitorConfig monitorConfig;
    private static IbmiSubsystemService ibmiSubsystemService;
    private static EmailService emailService;
    private static SubsystemMetricsExporter metricsExporter;

    // Suppression maps
    private static final Map<String, String> lastSubsystemStatus = new ConcurrentHashMap<>();
    private static final Map<String, String> lastErrorMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        if (args.length >= 1)
            appPropertiesFilePath = args[0];
        if (args.length >= 2)
            emailPropertiesFilePath = args[1];

        try {
            loadProperties(); // Load properties first to read log.level
            setupLogger();
            
            // Start automatic log purge (optional - configure via properties)
            int retentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            ibmiSubsystemService = new IbmiSubsystemService(
                    monitorConfig.getIbmiHost(),
                    monitorConfig.getIbmiUser(),
                    monitorConfig.getIbmiPassword());

            // Determine authentication method
            String authMethod = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();
            
            // Initialize OAuth2 if needed
            OAuth2TokenProvider oauth2Provider = null;
            String graphMailUrl = null;
            String fromUser = null;
            
            if ("OAUTH2".equals(authMethod)) {
                String tenantId = emailProps.getProperty("mail.oauth2.tenant.id");
                String clientId = emailProps.getProperty("mail.oauth2.client.id");
                String clientSecret = emailProps.getProperty("mail.oauth2.client.secret");
                String scope = emailProps.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
                String tokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");
                
                if (tenantId != null && clientId != null && clientSecret != null) {
                    oauth2Provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                    fromUser = emailProps.getProperty("mail.oauth2.from.user", monitorConfig.getEmailFrom().replaceAll(".*<([^>]+)>.*", "$1").trim());
                    String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
                    if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                        graphMailUrl = providedGraphUrl.trim();
                    } else {
                        graphMailUrl = "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
                    }
                    logger.info("OAuth2 authentication configured for email service.");
                }
            }

            emailService = new EmailService(
                    monitorConfig.getEmailHost(),
                    monitorConfig.getEmailPort(),
                    monitorConfig.getEmailFrom(),
                    monitorConfig.getEmailTo(),
                    monitorConfig.getEmailBcc(),
                    monitorConfig.getEmailUsername(),
                    monitorConfig.getEmailPassword(),
                    monitorConfig.isEmailAuthEnabled(),
                    monitorConfig.isEmailStartTlsEnabled(),
                    monitorConfig.getEmailImportance(),
                    monitorConfig.getIbmiHost(),
                    monitorConfig.getClientName(),
                    authMethod,
                    oauth2Provider,
                    graphMailUrl,
                    fromUser);

            metricsExporter = new SubsystemMetricsExporter(monitorConfig.getMetricsPort());
            metricsExporter.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            logger.info("Starting IBM i Subsystem monitoring service. Checking every " +
                    monitorConfig.getMonitorIntervalMs() / 1000 + " seconds.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    checkSubsystemsAndAlert();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected monitor error: " + e.getMessage(), e);
                    // emailService.sendErrorAlert(
                    // "Unexpected Monitoring Error",
                    // "A fatal error occurred in the IBM i Subsystem Monitor:\n\n" +
                    // e.getMessage());
                }
            }, 0, monitorConfig.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down subsystem monitor...");
                scheduler.shutdown();
                metricsExporter.stop();
                com.islandpacific.monitoring.common.AppLogger.closeLogger();
            }));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Startup failure: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setupLogger() throws IOException {
        // Use standardized AppLogger
        String logLevel = appProps.getProperty("log.level", "INFO");
        String logFolder = appProps.getProperty("log.folder", "logs");
        com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmsubsystemmonitoring", logLevel, logFolder);
    }

    private static void loadProperties() throws IOException {

        try (InputStream in = new FileInputStream(appPropertiesFilePath)) {
            appProps.load(in);
        }
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailProps.load(in);
        }

        monitorConfig = SubsystemMonitorConfig.fromProperties(appProps, emailProps);
    }

    // =========================================================
    // CORE MONITOR LOGIC WITH SPAM SUPPRESSION
    // =========================================================
    private static void checkSubsystemsAndAlert() throws Exception {

        List<SubsystemInfo> retrievedSubsystems = new ArrayList<>();
        String globalLibrary = monitorConfig.getIbmiSystemContextLibrary();

        for (String description : monitorConfig.getCriticalSubsystemNames()) {

            String key = globalLibrary + "/" + description;

            try {
                SubsystemInfo info = ibmiSubsystemService.getSubsystemInfo(description, globalLibrary);

                if (info != null) {
                    retrievedSubsystems.add(info);
                    String currentStatus = info.getStatus();
                    String previousStatus = lastSubsystemStatus.get(key);

                    // ✨ STATUS CHANGE SUPPRESSION
                    if (previousStatus == null ||
                            !previousStatus.equalsIgnoreCase(currentStatus)) {

                        logger.warning("Subsystem " + key + " changed: " +
                                previousStatus + " → " + currentStatus);

                        if (!"ACTIVE".equalsIgnoreCase(currentStatus)) {
                            emailService.sendSubsystemStatusAlert(info);
                        }

                        lastSubsystemStatus.put(key, currentStatus);
                        lastErrorMap.remove(key); // Reset error suppression

                    } else {
                        logger.info("Suppressed duplicate status alert for " + key +
                                " (still " + currentStatus + ")");
                    }

                } else {
                    // Subsystem NOT FOUND
                    String previous = lastSubsystemStatus.get(key);

                    if (previous == null || !"NOT FOUND".equalsIgnoreCase(previous)) {
                        logger.warning("Subsystem " + key + " NOT FOUND.");
                        // emailService.sendSubsystemStatusAlert(
                        // new SubsystemInfo(description, description, "NOT FOUND", globalLibrary));
                        lastSubsystemStatus.put(key, "NOT FOUND");
                    } else {
                        logger.info("Suppressed duplicate NOT FOUND alert for " + key);
                    }
                }

            } catch (Exception e) {

                String errorMessage = e.getMessage();
                String lastError = lastErrorMap.get(key);

                // ✨ ERROR SUPPRESSION
                if (lastError == null || !lastError.equalsIgnoreCase(errorMessage)) {

                    logger.severe("Sending NEW error alert for " + key + ": " + errorMessage);

                    // emailService.sendErrorAlert(
                    // "Subsystem Check Error: " + key,
                    // "An error occurred while checking subsystem " +
                    // key + ":\n\n" + errorMessage);

                    lastErrorMap.put(key, errorMessage);
                } else {
                    logger.info("Suppressed duplicate error email for " + key);
                }
            }
        }

        metricsExporter.updateMetrics(retrievedSubsystems);
    }
}
