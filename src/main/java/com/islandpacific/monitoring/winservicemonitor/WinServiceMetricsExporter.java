package com.islandpacific.monitoring.winservicemonitor;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.util.List;

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
        for (WinServiceInfo info : results) {
            if (info == null || info.getServiceStatuses() == null) continue;
            String serverLabel = info.getServerName();
            for (java.util.Map.Entry<String, String> entry : info.getServiceStatuses().entrySet()) {
                String service = entry.getKey();
                String value = entry.getValue();
                if (service == null || value == null) continue;
                boolean running = "Running".equalsIgnoreCase(value);
                SERVICE_STATUS.labels(serverLabel, service).set(running ? 1 : 0);
            }
        }
    }

    public void setDownCycles(String server, String service, int count) {
        SERVICE_DOWN_CYCLES.labels(server, service).set(count);
    }
}
