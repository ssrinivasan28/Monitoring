package com.islandpacific.monitoring.winservicemonitor;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WinServiceMetricsExporter {

    private final int port;
    private HTTPServer server;

    // 1 = Running, 0 = anything else
    private static final Gauge SERVICE_STATUS = Gauge.build()
            .name("win_service_status")
            .help("Windows service status: 1=Running, 0=Not Running")
            .labelNames("server", "service")
            .register();

    // Tracks consecutive down count per server+service for alert windowing
    private static final Gauge SERVICE_DOWN_CYCLES = Gauge.build()
            .name("win_service_down_cycles")
            .help("Consecutive monitoring cycles the service has not been Running")
            .labelNames("server", "service")
            .register();

    // Unix epoch seconds when the service last transitioned to Not Running
    private static final Gauge SERVICE_LAST_DOWN_TIME = Gauge.build()
            .name("win_service_last_down_time")
            .help("Unix timestamp (seconds) when the service last went down")
            .labelNames("server", "service")
            .register();

    // Unix epoch seconds when the service last transitioned to Running
    private static final Gauge SERVICE_LAST_UP_TIME = Gauge.build()
            .name("win_service_last_up_time")
            .help("Unix timestamp (seconds) when the service last came up")
            .labelNames("server", "service")
            .register();

    // Tracks previous running state per "server::service" to detect transitions
    private final Map<String, Boolean> previousRunning = new ConcurrentHashMap<>();

    public WinServiceMetricsExporter(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = new HTTPServer(port);
    }

    public void stop() {
        if (server != null) server.close();
    }

    public void updateMetrics(List<WinServiceInfo> results) {
        long now = System.currentTimeMillis() / 1000L;
        for (WinServiceInfo info : results) {
            if (info == null || info.getServiceStatuses() == null) continue;
            String serverLabel = info.getServerName();
            for (Map.Entry<String, String> entry : info.getServiceStatuses().entrySet()) {
                String service = entry.getKey();
                String value = entry.getValue();
                if (service == null || value == null) continue;
                boolean running = "Running".equalsIgnoreCase(value);
                SERVICE_STATUS.labels(serverLabel, service).set(running ? 1 : 0);

                String key = serverLabel + "::" + service;
                Boolean wasRunning = previousRunning.get(key);
                if (wasRunning == null || wasRunning != running) {
                    if (running) {
                        SERVICE_LAST_UP_TIME.labels(serverLabel, service).set(now);
                    } else {
                        SERVICE_LAST_DOWN_TIME.labels(serverLabel, service).set(now);
                    }
                    previousRunning.put(key, running);
                }
            }
        }
    }

    public void setDownCycles(String server, String service, int count) {
        SERVICE_DOWN_CYCLES.labels(server, service).set(count);
    }
}
