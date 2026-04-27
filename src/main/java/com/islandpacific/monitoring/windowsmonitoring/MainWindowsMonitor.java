package com.islandpacific.monitoring.windowsmonitoring;

import com.islandpacific.monitoring.common.AppLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWindowsMonitor {
    private static Logger logger;

    private static String propertiesFilePath = "windowsmonitor.properties";
    private static String emailPropertiesFilePath = "email.properties";

    private static Properties appProps = new Properties();
    private static Properties emailProps = new Properties();

    private static WindowsMonitorConfig config;
    private static WindowsMonitorMetricsService metricsService;
    private static EmailService emailService;
    private static WindowsMonitorMetricsExporter metricsExporter;

    // Alert Windowing: Map<Host, Map<MetricType, ConsecutiveBreachCount>>
    private static final Map<String, Map<String, Integer>> alertCounters = new ConcurrentHashMap<>();

    // Disk Growth Tracking: Map<Host, Map<Drive, PreviousUsagePercent>>
    private static final Map<String, Map<String, Double>> lastDiskUsage = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length >= 1)
            propertiesFilePath = args[0];
        if (args.length >= 2)
            emailPropertiesFilePath = args[1];

        try {
            loadProperties();
            setupLogger();
            logger = AppLogger.getLogger();

            // Start automatic log purge
            int retentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
            int purgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));
            AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

            metricsService = new WindowsMonitorMetricsService();
            emailService = new EmailService(config);
            metricsExporter = new WindowsMonitorMetricsExporter(config.getMetricsPort());
            metricsExporter.start();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            ExecutorService pollPool = Executors.newFixedThreadPool(config.getPollThreads());

            logger.info("Starting Windows Monitoring service.");
            logger.info("Monitoring " + config.getHosts().size() + " servers with " + config.getPollThreads()
                    + " threads.");
            logger.info("Polling every " + config.getMonitorIntervalMs() / 1000 + " seconds.");
            logger.info("Alert window size: " + config.getAlertWindowSize());

            // scheduleWithFixedDelay ensures the next cycle starts only after the previous one fully finishes,
            // preventing overlapping cycles if a poll runs long. Initial delay gives services time to warm up.
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    logger.info("Starting parallel Windows metrics collection cycle...");
                    checkAllServerMetrics(pollPool);
                    logger.info("Windows metrics collection cycle completed.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during monitoring cycle: " + e.getMessage(), e);
                    emailService.sendErrorAlert("Cycle Error",
                            "An error occurred during the monitoring cycle: " + e.getMessage());
                }
            }, 5000, config.getMonitorIntervalMs(), TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logger != null)
                    logger.info("Shutting down Windows Monitor gracefully...");
                scheduler.shutdown();
                pollPool.shutdown();
                if (metricsExporter != null)
                    metricsExporter.stop();
                AppLogger.closeLogger();
                if (logger != null)
                    logger.info("Shutdown complete.");
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

    private static void setupLogger() throws IOException {
        String logLevel = appProps.getProperty("log.level", "INFO");
        String logFolder = appProps.getProperty("log.folder", "logs");
        AppLogger.setupLogger("windowsmonitoring", logLevel, logFolder);
    }

    private static void loadProperties() throws IOException {
        try (InputStream in = new FileInputStream(propertiesFilePath)) {
            appProps.load(in);
        }
        try (InputStream in = new FileInputStream(emailPropertiesFilePath)) {
            emailProps.load(in);
        }
        config = WindowsMonitorConfig.fromProperties(appProps, emailProps);
    }

    private static void checkAllServerMetrics(ExecutorService pollPool) {
        List<Future<WindowsMonitorInfo>> futures = new ArrayList<>();

        for (String host : config.getHosts()) {
            final String targetHost = host.trim();
            if (targetHost.isEmpty())
                continue;

            futures.add(pollPool.submit(() -> {
                try {
                    WindowsMonitorConfig.Credentials creds = config.getCredentialsForHost(targetHost);
                    return metricsService.getMetrics(targetHost, config.getTopNProcesses(),
                            config.getServicesToMonitor(), creds);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Thread error fetching metrics for " + targetHost, e);
                    return null;
                }
            }));
        }

        List<WindowsMonitorInfo> currentInfos = new ArrayList<>();
        for (Future<WindowsMonitorInfo> future : futures) {
            try {
                WindowsMonitorInfo info = future.get(30, TimeUnit.SECONDS);
                if (info != null) {
                    currentInfos.add(info);
                    processAlerts(info);
                }
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warning("Poll timed out for a host after 30s — skipping this cycle's result");
                future.cancel(true);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error retrieving task result: " + e.getMessage(), e);
            }
        }

        metricsExporter.updateMetrics(currentInfos);
    }

    private static void processAlerts(WindowsMonitorInfo info) {
        String host = info.getHostName();
        Map<String, Integer> hostCounters = alertCounters.computeIfAbsent(host, k -> new ConcurrentHashMap<>());

        // CPU Alert
        checkMetric(host, "CPU", info.getCpuUtilization(), config.getCpuAlertThreshold(), hostCounters);

        // Memory Alert
        checkMetric(host, "Memory", info.getMemoryUtilization(), config.getMemoryAlertThreshold(), hostCounters);

        // Disk Growth & Alert
        if (info.getDisks() != null) {
            Map<String, Double> prevDisks = lastDiskUsage.computeIfAbsent(host, k -> new ConcurrentHashMap<>());
            for (Map.Entry<String, WindowsMonitorInfo.DiskInfo> entry : info.getDisks().entrySet()) {
                String drive = entry.getKey();
                double current = entry.getValue().getUsagePercent();
                double limit = config.getDiskAlertThreshold();

                checkMetric(host, "DISK_" + drive, current, limit, hostCounters);

                // Growth alert is windowed like other metrics to prevent email flooding.
                if (prevDisks.containsKey(drive)) {
                    double diff = current - prevDisks.get(drive);
                    if (diff > 5.0) {
                        logger.warning(
                                String.format("[%s] Rapid disk growth detected on %s: +%.2f%%", host, drive, diff));
                        String growthKey = "DISK_GROWTH_" + drive;
                        int growthCount = hostCounters.getOrDefault(growthKey, 0) + 1;
                        hostCounters.put(growthKey, growthCount);
                        if (growthCount == config.getAlertWindowSize()) {
                            emailService.sendErrorAlert("Rapid Disk Growth: " + drive + " on " + host,
                                    "Disk " + drive + " usage increased by " + String.format("%.2f%%", diff)
                                            + " since last check (Current: " + String.format("%.2f%%", current) + ")");
                        }
                    } else {
                        hostCounters.put("DISK_GROWTH_" + drive, 0);
                    }
                }
                prevDisks.put(drive, current);
            }
        }

        // Service Alerts (windowed to avoid alert storms on transient blips)
        Map<String, String> serviceStatuses = info.getServiceStatuses();
        if (serviceStatuses == null) serviceStatuses = Collections.emptyMap();
        for (Map.Entry<String, String> entry : serviceStatuses.entrySet()) {
            if (!"Running".equalsIgnoreCase(entry.getValue())) {
                String serviceKey = "SERVICE_" + entry.getKey();
                int count = hostCounters.getOrDefault(serviceKey, 0) + 1;
                hostCounters.put(serviceKey, count);

                if (count == config.getAlertWindowSize()) { // Alert once per window breach
                    emailService.sendErrorAlert("Service Stopped: " + entry.getKey() + " on " + host,
                            "Service " + entry.getKey() + " is in status: " + entry.getValue());
                }
            } else {
                hostCounters.remove("SERVICE_" + entry.getKey());
            }
        }

        // Consolidated system alert: fires once per breach window, resets when BOTH CPU and Memory recover
        boolean cpuBreached    = hostCounters.getOrDefault("CPU", 0) >= config.getAlertWindowSize();
        boolean memoryBreached = hostCounters.getOrDefault("Memory", 0) >= config.getAlertWindowSize();

        if (cpuBreached || memoryBreached) {
            // Send one alert per window breach — suppress further alerts until full recovery
            if (hostCounters.getOrDefault("ALERT_SENT", 0) == 0) {
                emailService.sendSystemAlert(info, config.getCpuAlertThreshold(), config.getMemoryAlertThreshold(),
                        config.getDiskAlertThreshold());
                hostCounters.put("ALERT_SENT", 1);
            }
        } else {
            // Both CPU and Memory are below threshold — reset so next breach fires a new alert
            hostCounters.put("ALERT_SENT", 0);
        }
    }

    private static void checkMetric(String host, String type, double value, double threshold,
            Map<String, Integer> counters) {
        if (value > threshold) {
            counters.put(type, counters.getOrDefault(type, 0) + 1);
            logger.warning(String.format("[%s] %s threshold breached (%f > %f). Window count: %d",
                    host, type, value, threshold, counters.get(type)));
        } else {
            counters.put(type, 0);
        }
    }
}
