package com.islandpacific.monitoring.ibmssystemmatrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainIbmiSystemMonitorApp {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String systemMonitorPropertiesFilePath = "systemmonitor.properties";
    private static String emailPropertiesFilePath = "email.properties";

    private static Properties appProps = new Properties();
    private static Properties emailProps = new Properties();

    private static IbmiSystemMonitorConfig monitorConfig;
    private static IbmiSystemMonitorMetricsService ibmiSystemMonitorMetricsService;
    private static EmailService ibmiSystemMonitorEmailService;
    private static IbmiSystemMonitorMetricsExporter ibmiSystemMonitorMetricsExporter;

    private static final Map<String, IbmiSystemMonitorInfo> previousSystemUtilization = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length >= 1) systemMonitorPropertiesFilePath = args[0];
        if (args.length >= 2) emailPropertiesFilePath = args[1];

        try {
            loadProperties(); // Load properties first to read log.level
            setupLogger();
            
            // Start automatic log purge
            int retentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", 
                emailProps.getProperty("log.retention.days", "30")));
            int purgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours",
                emailProps.getProperty("log.purge.interval.hours", "24")));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            ibmiSystemMonitorMetricsService = new IbmiSystemMonitorMetricsService();
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

            ibmiSystemMonitorEmailService = new EmailService(
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
                    emailProps.getProperty("mail.clientName", ""),
                    authMethod,
                    oauth2Provider,
                    graphMailUrl,
                    fromUser
            );

            ibmiSystemMonitorMetricsExporter = new IbmiSystemMonitorMetricsExporter(monitorConfig.getMetricsPort());
            ibmiSystemMonitorMetricsExporter.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            logger.info("Starting IBM i Performance monitoring service. Checking every " +
                    monitorConfig.getMonitorIntervalMs() / 1000 + " seconds.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    logger.info("Starting new scan for IBM i system performance metrics across all configured hosts.");
                    checkAllSystemMetrics();
                    logger.info("IBM i system performance metrics scan completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during monitoring cycle: " + e.getMessage(), e);
                    ibmiSystemMonitorEmailService.sendErrorAlert("Overall Monitoring Error",
                            "An error occurred: " + e.getMessage());
                }
            }, 5, monitorConfig.getMonitorIntervalMs(), TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down IBM i Performance monitor gracefully...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate in time, forcing shutdown.");
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.warning("Shutdown interrupted.");
                    scheduler.shutdownNow();
                } finally {
                    ibmiSystemMonitorMetricsExporter.stop();
                    com.islandpacific.monitoring.common.AppLogger.closeLogger();
                    logger.info("Shutdown complete.");
                }
            }));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal startup error: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void setupLogger() throws IOException {
        // Use standardized AppLogger
        String logLevel = appProps.getProperty("log.level", "INFO");
        String logFolder = appProps.getProperty("log.folder", "logs");
        com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmssystemmatrix", logLevel, logFolder);
    }

    private static void loadProperties() throws IOException {
        try (InputStream in = new FileInputStream(systemMonitorPropertiesFilePath)) {
            appProps.load(in);
        }
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailProps.load(in);
        }
        monitorConfig = IbmiSystemMonitorConfig.fromProperties(appProps, emailProps);
    }

    private static void checkAllSystemMetrics() {
        List<IbmiSystemMonitorInfo> currentSystemInfos = new ArrayList<>();

        for (String ibmiHost : monitorConfig.getIbmiHosts()) {
            try {
                IbmiSystemMonitorInfo currentInfo =
                        ibmiSystemMonitorMetricsService.getSystemUtilization(ibmiHost,
                                monitorConfig.getIbmiUser(), monitorConfig.getIbmiPassword());
                currentSystemInfos.add(currentInfo);

                boolean sendAlert = (currentInfo.getCpuUtilization() > monitorConfig.getCpuAlertThreshold()
                        || currentInfo.getAspUtilization() > monitorConfig.getAspAlertThreshold()
                        || currentInfo.getSharedPoolUtilization() > monitorConfig.getSharedProcessorPoolAlertThreshold()
                        || currentInfo.getTotalJobs() > monitorConfig.getTotalJobsAlertThreshold()
                        || currentInfo.getActiveJobs() > monitorConfig.getActiveJobsAlertThreshold());

                if (sendAlert) {
                    ibmiSystemMonitorEmailService.sendSystemAlert(currentInfo,
                            monitorConfig.getCpuAlertThreshold(),
                            monitorConfig.getAspAlertThreshold(),
                            monitorConfig.getSharedProcessorPoolAlertThreshold(),
                            monitorConfig.getTotalJobsAlertThreshold(),
                            monitorConfig.getActiveJobsAlertThreshold());
                }

                previousSystemUtilization.put(ibmiHost, currentInfo);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error fetching metrics for " + ibmiHost, e);
                ibmiSystemMonitorEmailService.sendErrorAlert("Metrics Fetch Error",
                        "Failed to fetch metrics for " + ibmiHost + ": " + e.getMessage());
            }
        }

        ibmiSystemMonitorMetricsExporter.updateMetrics(currentSystemInfos);
    }
}
