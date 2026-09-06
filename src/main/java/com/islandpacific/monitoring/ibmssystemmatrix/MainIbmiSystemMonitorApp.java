package com.islandpacific.monitoring.ibmssystemmatrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainIbmiSystemMonitorApp {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private static String emailPropertiesFilePath = "email.properties";
    private static String systemMonitorPropertiesFilePath = "ibmmatrixmonitor.properties";

    private static Properties appProps = new Properties();
    private static Properties emailProps = new Properties();

    private static IbmiSystemMonitorConfig monitorConfig;
    private static IbmiSystemMonitorMetricsService ibmiSystemMonitorMetricsService;
    private static EmailService ibmiSystemMonitorEmailService;
    private static IbmiSystemMonitorMetricsExporter ibmiSystemMonitorMetricsExporter;

    private static final Map<String, Boolean> previousAlertState = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> lastAlertTimestamp = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> lastErrorTimestamp = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 60 * 60 * 1000;

    private static DailyMetricStore metricStore;
    private static DailyReportService reportService;

    public static void main(String[] args) {
        if (args.length >= 1) emailPropertiesFilePath = args[0];
        if (args.length >= 2) systemMonitorPropertiesFilePath = args[1];

        try {
            loadProperties();

            String logLevel = emailProps.getProperty("log.level", "INFO");
            String logFolder = emailProps.getProperty("log.folder", "logs");
            com.islandpacific.monitoring.common.AppLogger.setupLogger("ibmssystemmatrix", logLevel, logFolder);

            int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));
            com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            ibmiSystemMonitorMetricsService = new IbmiSystemMonitorMetricsService();
            ibmiSystemMonitorEmailService = new EmailService(emailProps, monitorConfig.getIbmiHosts().get(0), monitorConfig.getClientName(), monitorConfig.getReportLogoPath());

            System.err.println("DEBUG: Starting metrics exporter on port " + monitorConfig.getMetricsPort());
            ibmiSystemMonitorMetricsExporter = new IbmiSystemMonitorMetricsExporter(monitorConfig.getMetricsPort());
            ibmiSystemMonitorMetricsExporter.start();
            System.err.println("DEBUG: Metrics exporter started");

            metricStore = new DailyMetricStore(monitorConfig.getReportDataFolder());
            reportService = new DailyReportService(metricStore, monitorConfig.getClientName(), monitorConfig.getReportLogoPath());

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
            ScheduledExecutorService reportScheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "report-scheduler"); t.setDaemon(true); return t; });
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
            }, 5, monitorConfig.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            if (monitorConfig.isReportEnabled()) {
                scheduleDaily(reportScheduler, monitorConfig.getReportSendTime());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down IBM i Performance monitor gracefully...");
                scheduler.shutdown();
                reportScheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warning("Scheduler did not terminate in time, forcing shutdown.");
                        scheduler.shutdownNow();
                    }
                    reportScheduler.shutdownNow();
                } catch (InterruptedException e) {
                    logger.warning("Shutdown interrupted.");
                    scheduler.shutdownNow();
                    reportScheduler.shutdownNow();
                } finally {
                    ibmiSystemMonitorMetricsExporter.stop();
                    com.islandpacific.monitoring.common.AppLogger.closeLogger();
                    logger.info("Shutdown complete.");
                }
            }));

        } catch (Exception e) {
            System.err.println("FATAL STARTUP ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            logger.log(Level.SEVERE, "Fatal startup error: " + e.getMessage(), e);
            System.exit(1);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void scheduleDaily(ScheduledExecutorService scheduler, String sendTime) {
        LocalTime target;
        try {
            target = LocalTime.parse(sendTime, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            logger.warning("Invalid report.send.time '" + sendTime + "', defaulting to 08:00");
            target = LocalTime.of(8, 0);
        }
        long nowSec = LocalTime.now().toSecondOfDay();
        long targetSec = target.toSecondOfDay();
        long initialDelaySec = targetSec > nowSec ? targetSec - nowSec : 86400 - (nowSec - targetSec);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[REPORT] Generating daily IBM i performance report...");
                logger.info("Generating daily IBM i performance report...");
                metricStore.pruneOlderThan(monitorConfig.getReportRetentionDays());
                byte[] pdf = reportService.generatePdfReport();
                System.out.println("[REPORT] PDF generated, size=" + pdf.length + " bytes");
                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                ibmiSystemMonitorEmailService.sendDailyReport(pdf, date);
                System.out.println("[REPORT] Email sent for " + date);
                logger.info("Daily report sent for " + date);
            } catch (Exception e) {
                System.err.println("[REPORT] FAILED: " + e.getMessage());
                e.printStackTrace(System.err);
                logger.log(Level.SEVERE, "Failed to generate/send daily report: " + e.getMessage(), e);
            }
        }, initialDelaySec, 86400, TimeUnit.SECONDS);

        System.out.println("[REPORT] Scheduled at " + target + " — first run in " + initialDelaySec + "s");
        logger.info("Daily report scheduled at " + target + " (first run in " + initialDelaySec + "s)");
    }

    private static void loadProperties() throws IOException {
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailProps.load(in);
        }
        try (InputStream in = new FileInputStream(systemMonitorPropertiesFilePath)) {
            appProps.load(in);
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

                if (monitorConfig.isReportEnabled()) {
                    metricStore.append(new MetricRecord(
                            java.time.Instant.now(), ibmiHost,
                            currentInfo.getCpuUtilization(),
                            currentInfo.getAspUtilization(),
                            currentInfo.getSharedPoolUtilization(),
                            currentInfo.getTotalJobs(),
                            currentInfo.getActiveJobs()));
                }

                boolean breaching = (currentInfo.getCpuUtilization() > monitorConfig.getCpuAlertThreshold()
                        || currentInfo.getAspUtilization() > monitorConfig.getAspAlertThreshold()
                        || currentInfo.getSharedPoolUtilization() > monitorConfig.getSharedProcessorPoolAlertThreshold()
                        || currentInfo.getTotalJobs() > monitorConfig.getTotalJobsAlertThreshold()
                        || currentInfo.getActiveJobs() > monitorConfig.getActiveJobsAlertThreshold());

                boolean wasBreaching = previousAlertState.getOrDefault(ibmiHost, false);
                long now = System.currentTimeMillis();
                long lastAlert = lastAlertTimestamp.computeIfAbsent(ibmiHost, k -> new AtomicLong(0)).get();
                boolean cooldownExpired = (now - lastAlert) >= ALERT_COOLDOWN_MS;

                if (breaching && (!wasBreaching || cooldownExpired)) {
                    ibmiSystemMonitorEmailService.sendSystemAlert(currentInfo,
                            monitorConfig.getCpuAlertThreshold(),
                            monitorConfig.getAspAlertThreshold(),
                            monitorConfig.getSharedProcessorPoolAlertThreshold(),
                            monitorConfig.getTotalJobsAlertThreshold(),
                            monitorConfig.getActiveJobsAlertThreshold());
                    lastAlertTimestamp.get(ibmiHost).set(now);
                }
                previousAlertState.put(ibmiHost, breaching);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error fetching metrics for " + ibmiHost, e);
                long now = System.currentTimeMillis();
                long lastErr = lastErrorTimestamp.computeIfAbsent(ibmiHost, k -> new AtomicLong(0)).get();
                if ((now - lastErr) >= ALERT_COOLDOWN_MS) {
                    ibmiSystemMonitorEmailService.sendErrorAlert("Metrics Fetch Error",
                            "Failed to fetch metrics for " + ibmiHost + ": " + e.getMessage());
                    lastErrorTimestamp.get(ibmiHost).set(now);
                }
            }
        }

        ibmiSystemMonitorMetricsExporter.updateMetrics(currentSystemInfos);
    }
}
