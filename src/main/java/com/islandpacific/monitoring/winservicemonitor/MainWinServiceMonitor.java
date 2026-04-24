package com.islandpacific.monitoring.winservicemonitor;

import com.islandpacific.monitoring.common.AppLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWinServiceMonitor {

    private static Logger logger;

    private static String propertiesFilePath = "winservicemonitor.properties";
    private static String emailPropertiesFilePath = "email.properties";

    private static WinServiceMonitorConfig config;
    private static WinServiceMonitorService monitorService;
    private static WinServiceMetricsExporter metricsExporter;
    private static EmailService emailService;

    // Track consecutive down cycles per server+service to implement alert windowing
    // Key: "server::service" -> consecutive down count
    private static final ConcurrentHashMap<String, Integer> downCycles = new ConcurrentHashMap<>();
    // Track whether a down-alert has been sent so we can send a recovery alert
    private static final ConcurrentHashMap<String, Boolean> alertedDown = new ConcurrentHashMap<>();
    // Track consecutive stable (up) cycles after recovery — must reach alertWindowSize before re-alerting
    private static final ConcurrentHashMap<String, Integer> stableCycles = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length >= 1) propertiesFilePath = args[0];
        if (args.length >= 2) emailPropertiesFilePath = args[1];

        try {
            Properties appProps = loadProperties(propertiesFilePath);
            Properties emailProps = loadProperties(emailPropertiesFilePath);

            config = WinServiceMonitorConfig.fromProperties(appProps, emailProps);
            AppLogger.setupLogger("winservicemonitor", config.getLogLevel(), config.getLogFolder());
            logger = AppLogger.getLogger();

            AppLogger.startScheduledLogPurge(config.getLogRetentionDays(), config.getLogPurgeIntervalHours());

            monitorService = new WinServiceMonitorService();
            emailService = new EmailService(config);
            metricsExporter = new WinServiceMetricsExporter(config.getMetricsPort());
            metricsExporter.start();

            logger.info("Win Service Monitor started.");
            logger.info("Monitoring " + config.getServers().size() + " server(s): " + config.getServers());
            for (String srv : config.getServers()) {
                logger.info("Services on " + srv + ": " + config.getServicesForServer(srv));
            }
            logger.info("Interval: " + config.getMonitorIntervalSeconds() + "s, alert window: "
                    + config.getAlertWindowSize() + " cycles");
            logger.info("Metrics exposed on port " + config.getMetricsPort());

            ExecutorService pollPool = Executors.newFixedThreadPool(config.getPollThreads());
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    runMonitoringCycle(pollPool);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unhandled error in monitoring cycle: " + e.getMessage(), e);
                }
            }, 5, config.getMonitorIntervalSeconds(), TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logger != null) logger.info("Shutting down Win Service Monitor...");
                scheduler.shutdown();
                pollPool.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) scheduler.shutdownNow();
                    if (!pollPool.awaitTermination(10, TimeUnit.SECONDS)) pollPool.shutdownNow();
                } catch (InterruptedException ie) {
                    scheduler.shutdownNow();
                    pollPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                if (metricsExporter != null) metricsExporter.stop();
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

    private static void runMonitoringCycle(ExecutorService pollPool) {
        List<Future<WinServiceInfo>> futures = new ArrayList<>();

        for (String server : config.getServers()) {
            futures.add(pollPool.submit(() -> {
                try {
                    WinServiceMonitorConfig.Credentials creds = config.getCredentialsForServer(server);
                    return monitorService.checkServices(server, config.getServicesForServer(server), creds);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error polling server: " + server, e);
                    return null;
                }
            }));
        }

        List<WinServiceInfo> results = new ArrayList<>();
        for (Future<WinServiceInfo> future : futures) {
            try {
                WinServiceInfo info = future.get(35, TimeUnit.SECONDS);
                if (info != null) {
                    results.add(info);
                    processAlerts(info);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error retrieving poll result: " + e.getMessage(), e);
            }
        }

        metricsExporter.updateMetrics(results);
    }

    private static void processAlerts(WinServiceInfo info) {
        String server = info.getServerName();
        String serverKey = server + "::__server__";

        // Check if all services report ServerUnreachable — treat as a server-level event
        boolean allUnreachable = !info.getServiceStatuses().isEmpty()
                && info.getServiceStatuses().values().stream()
                        .allMatch(WinServiceMonitorService.SERVER_UNREACHABLE::equals);

        if (allUnreachable) {
            stableCycles.put(serverKey, 0);
            int count = downCycles.getOrDefault(serverKey, 0) + 1;
            downCycles.put(serverKey, count);
            // Reset per-service state so stale counts don't fire spurious alerts after recovery
            for (String svc : info.getServiceStatuses().keySet()) {
                String key = server + "::" + svc;
                downCycles.put(key, 0);
                stableCycles.put(key, 0);
            }
            logger.warning("[" + server + "] Server is unreachable (cycle " + count + "/" + config.getAlertWindowSize() + ")");
            // Alert only on the exact threshold — not repeatedly
            if (count == config.getAlertWindowSize()) {
                logger.warning("[" + server + "] ALERT: server unreachable");
                emailService.sendServerUnreachableAlert(server);
                alertedDown.put(serverKey, true);
            }
            return;
        }

        // Server is reachable — track stable cycles before allowing re-alert
        downCycles.put(serverKey, 0);

        if (Boolean.TRUE.equals(alertedDown.get(serverKey))) {
            int stable = stableCycles.getOrDefault(serverKey, 0) + 1;
            stableCycles.put(serverKey, stable);
            if (stable >= config.getAlertWindowSize()) {
                logger.info("[" + server + "] Server is reachable again (stable for " + stable + " cycles).");
                emailService.sendServerRecoveryAlert(server);
                alertedDown.put(serverKey, false);
                stableCycles.put(serverKey, 0);
            } else {
                logger.info("[" + server + "] Server reachable but waiting for stability (" + stable + "/" + config.getAlertWindowSize() + ")");
                return; // still in cooldown — skip service checks this cycle
            }
        }

        for (Map.Entry<String, String> entry : info.getServiceStatuses().entrySet()) {
            String service = entry.getKey();
            String status = entry.getValue();
            String key = server + "::" + service;
            boolean running = "Running".equalsIgnoreCase(status);

            if (!running) {
                int count = downCycles.getOrDefault(key, 0) + 1;
                downCycles.put(key, count);
                metricsExporter.setDownCycles(server, service, count);

                logger.warning("[" + server + "] Service '" + service + "' is " + status
                        + " (down cycle " + count + "/" + config.getAlertWindowSize() + ")");

                if (count == config.getAlertWindowSize()) {
                    String displayName = info.getDisplayName(service);
                    logger.warning("[" + server + "] ALERT threshold reached for service '" + displayName + "'");
                    emailService.sendServiceDownAlert(server, displayName, status);
                    alertedDown.put(key, true);
                }
            } else {
                downCycles.put(key, 0);
                metricsExporter.setDownCycles(server, service, 0);

                if (Boolean.TRUE.equals(alertedDown.get(key))) {
                    int svcStable = stableCycles.getOrDefault(key, 0) + 1;
                    stableCycles.put(key, svcStable);
                    if (svcStable >= config.getAlertWindowSize()) {
                        String displayName = info.getDisplayName(service);
                        logger.info("[" + server + "] Service '" + displayName + "' has RECOVERED (stable for "
                                + svcStable + " cycles).");
                        emailService.sendServiceRecoveryAlert(server, displayName);
                        alertedDown.put(key, false);
                        stableCycles.put(key, 0);
                    } else {
                        logger.info("[" + server + "] Service '" + service + "' up but waiting for stability ("
                                + svcStable + "/" + config.getAlertWindowSize() + ")");
                    }
                } else {
                    stableCycles.put(key, 0);
                }
            }
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
