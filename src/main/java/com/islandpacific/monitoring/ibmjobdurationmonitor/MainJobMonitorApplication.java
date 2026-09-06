package com.islandpacific.monitoring.ibmjobdurationmonitor;

import com.islandpacific.monitoring.common.AppLogger;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainJobMonitorApplication {

    private static final Logger logger = Logger.getLogger(MainJobMonitorApplication.class.getName());

    public static void main(String[] args) {
        String emailPropsFile = args.length >= 1 ? args[0] : "email.properties";
        String monitorPropsFile = args.length >= 2 ? args[1] : "ibmjobdurationmonitor.properties";

        JobMonitorConfig config;
        try {
            config = new JobMonitorConfig(emailPropsFile, monitorPropsFile);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.exit(1);
            return;
        }

        String logLevel = config.getEmailProps().getProperty("log.level", "INFO");
        String logFolder = config.getEmailProps().getProperty("log.folder", "logs");
        int retentionDays = Integer.parseInt(config.getEmailProps().getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(config.getEmailProps().getProperty("log.purge.interval.hours", "24"));

        AppLogger.setupLogger("ibmjobdurationmonitor", logLevel, logFolder);
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        logger.info("Starting IBM i Job Duration Monitor with " + config.getJobSpecs().size() + " job(s) configured.");

        IbmiJobService jobService = new IbmiJobService(config.getIbmiHost(), config.getIbmiUser(), config.getIbmiPassword());
        String logoPath = config.getMonitorProps().getProperty("logo.path", "");
        EmailService emailService = new EmailService(config.getEmailProps(), config.getIbmiHost(), config.getClientName(), logoPath);
        JobDurationMonitor monitor = new JobDurationMonitor(config, jobService, emailService);

        startMetricsServer(config.getMetricsPort());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "job-duration-monitor");
            t.setDaemon(true);
            return t;
        });

        long intervalMs = config.getMonitorIntervalMs();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitor.runCheck();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unhandled error in monitor loop: " + e.getMessage(), e);
                try {
                    emailService.sendErrorAlert("Monitor Loop Error", e.getMessage() != null ? e.getMessage() : e.toString());
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Failed to send error alert: " + ex.getMessage(), ex);
                }
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down IBM i Job Duration Monitor...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        logger.info("IBM i Job Duration Monitor started. Metrics on port " + config.getMetricsPort()
                + ". Interval: " + intervalMs + "ms.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startMetricsServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", httpExchange -> {
                httpExchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
                httpExchange.sendResponseHeaders(200, 0);
                try (OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody())) {
                    TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
                }
            });
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "metrics-server");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            logger.info("Metrics server started on port " + port);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start metrics server on port " + port + ": " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
