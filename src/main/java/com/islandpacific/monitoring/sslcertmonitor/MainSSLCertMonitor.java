package com.islandpacific.monitoring.sslcertmonitor;

import com.islandpacific.monitoring.common.AppLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainSSLCertMonitor {

    private static Logger logger;

    private static String propertiesFilePath = "sslcertmonitor.properties";
    private static String emailPropertiesFilePath = "email.properties";

    public static void main(String[] args) {
        if (args.length >= 1) propertiesFilePath = args[0];
        if (args.length >= 2) emailPropertiesFilePath = args[1];

        try {
            Properties appProps = loadProperties(propertiesFilePath);
            Properties emailProps = loadProperties(emailPropertiesFilePath);

            SSLCertMonitorConfig config = SSLCertMonitorConfig.fromProperties(appProps, emailProps);
            AppLogger.setupLogger("sslcertmonitor", config.getLogLevel(), config.getLogFolder());
            logger = AppLogger.getLogger();

            AppLogger.startScheduledLogPurge(config.getLogRetentionDays(), config.getLogPurgeIntervalHours());

            SSLCertMonitorService monitorService = new SSLCertMonitorService();
            EmailService emailService = new EmailService(config);
            SSLCertMetrics metrics = new SSLCertMetrics(config.getMetricsPort());
            metrics.start();

            logger.info("SSL Cert Monitor started.");
            logger.info("Monitoring " + config.getHosts().size() + " host(s): " + config.getHosts());
            logger.info("Warning thresholds: " + config.getWarningThresholds() + " days");
            logger.info("Metrics exposed on port " + config.getMetricsPort());

            // Tracks which thresholds have already fired per host — key: "host:port", value: set of threshold days alerted
            ConcurrentHashMap<String, Set<Integer>> alertedThresholds = new ConcurrentHashMap<>();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    List<SSLCertInfo> results = new ArrayList<>();
                    for (SSLCertMonitorConfig.HostEntry host : config.getHosts()) {
                        SSLCertInfo info = monitorService.checkCertificate(host);
                        results.add(info);

                        if (info.hasError()) {
                            logger.warning("Certificate check failed for " + info.getHostPort() + ": " + info.getError());
                            emailService.sendCheckError(info);
                        } else {
                            long days = info.getDaysUntilExpiry();
                            Set<Integer> fired = alertedThresholds.computeIfAbsent(info.getHostPort(), k -> new HashSet<>());

                            // Fire alert for each threshold the cert has crossed that hasn't been alerted yet
                            for (int threshold : config.getWarningThresholds()) {
                                if (days <= threshold && !fired.contains(threshold)) {
                                    fired.add(threshold);
                                    logger.warning("Certificate for " + info.getHostPort() + " expires in "
                                            + days + " days (threshold: " + threshold + ")");
                                    emailService.sendExpiryWarning(info, threshold);
                                }
                            }

                            // Reset fired thresholds if cert was renewed (days increased above all thresholds)
                            int maxThreshold = config.getWarningThresholds().get(0);
                            if (days > maxThreshold) {
                                fired.clear();
                            }

                            if (fired.isEmpty()) {
                                logger.info("Certificate for " + info.getHostPort() + " is valid — expires in " + days + " days");
                            }
                        }
                    }
                    metrics.updateMetrics(results);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unhandled error in monitoring cycle: " + e.getMessage(), e);
                }
            }, 0, config.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logger != null) logger.info("Shutting down SSL Cert Monitor...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) scheduler.shutdownNow();
                } catch (InterruptedException ie) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                metrics.stop();
                AppLogger.closeLogger();
            }));

        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Fatal startup error: " + e.getMessage(), e);
            } else {
                System.err.println("Fatal startup error: " + e.getMessage());
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static Properties loadProperties(String path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        }
        return props;
    }
}
