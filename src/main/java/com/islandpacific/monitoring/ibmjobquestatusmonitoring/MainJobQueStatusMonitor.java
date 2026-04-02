package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import com.islandpacific.monitoring.common.AppLogger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainJobQueStatusMonitor {

    private static final Logger logger = AppLogger.getLogger();

    public static void main(String[] args) {
        JobMonitorConfig config;
        try {
            config = new JobMonitorConfig("joblist.properties", "email.properties");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load configuration: " + e.getMessage(), e);
            return;
        }

        // Initialize logger with configurable settings
        Properties emailProps = new Properties();
        try (InputStream input = new FileInputStream("email.properties")) {
            emailProps.load(input);
        } catch (IOException e) {
            System.err.println("Could not load email.properties: " + e.getMessage());
        }
        
        // Setup logger after loading properties
        String logLevel = emailProps.getProperty("log.level", "INFO");
        String logFolder = emailProps.getProperty("log.folder", "logs");
        AppLogger.setupLogger("ibmjobquestatusmonitoring", logLevel, logFolder);
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Job Queue Status Monitor...");
            AppLogger.closeLogger();
        }));
        
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
                fromUser = emailProps.getProperty("mail.oauth2.from.user", config.getFromEmail().replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedGraphUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "");
                if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                    graphMailUrl = providedGraphUrl.trim();
                } else {
                    graphMailUrl = "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
                }
                logger.info("OAuth2 authentication configured for email service.");
            }
        }

        EmailService emailService = new EmailService(
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.getFromEmail(),
                config.getToEmails(),
                config.getBccEmails(),
                config.getSmtpUsername(),
                config.getSmtpPassword(),
                config.isSmtpAuth(),
                config.isStartTls(),
                config.getEmailImportance(),
                config.getIbmiHost(),
                authMethod,
                oauth2Provider,
                graphMailUrl,
                fromUser);

        IbmiJobService ibmiJobService = new IbmiJobService(
                config.getIbmiHost(), config.getIbmiUser(), config.getIbmiPassword());

        JobMetricsExporter metricsExporter = new JobMetricsExporter();
        JobMonitor monitor = new JobMonitor(config, ibmiJobService, metricsExporter, emailService);

        Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));

        monitor.start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
