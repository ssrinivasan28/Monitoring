package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SubsystemMetricsExporter {

    private static final Logger logger = Logger.getLogger(SubsystemMetricsExporter.class.getName());
    private static final String METRICS_PATH = "/metrics";

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("ibmi_subsystem_monitor_uptime_seconds")
            .help("Uptime of the IBM i subsystem monitor application in seconds.")
            .register();

    private static final Gauge LAST_UPDATE_TIMESTAMP = Gauge.build()
            .name("ibmi_subsystem_monitor_last_metrics_update_timestamp_seconds")
            .help("Last time subsystem metrics were updated in epoch seconds.")
            .register();

    private static final Gauge SUBSYSTEM_STATUS = Gauge.build()
            .name("ibmi_subsystem_status")
            .help("Current operational status of the IBM i subsystem (1=active, 0=inactive/other).")
            .labelNames("subsystem_name", "subsystem_library", "status_text")
            .register();

    private static final Gauge SUBSYSTEM_INFO = Gauge.build()
            .name("ibmi_subsystem_info")
            .help("General information about the IBM i subsystem.")
            .labelNames("subsystem_name", "subsystem_library", "description")
            .register();

    private final int port;
    private HttpServer server;

    public SubsystemMetricsExporter(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(METRICS_PATH, httpExchange -> {
            httpExchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
            httpExchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody())) {
                TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
            } finally {
                httpExchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        logger.info("Subsystem metrics server started on port " + port + METRICS_PATH);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Subsystem metrics server stopped on port " + port);
        }
    }

    public void updateMetrics(List<SubsystemInfo> subsystems) {
        long now = Instant.now().getEpochSecond();
        long uptimeSeconds = (System.currentTimeMillis()
                - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
        LAST_UPDATE_TIMESTAMP.set(now);

        for (SubsystemInfo sub : subsystems) {
            double statusValue = "ACTIVE".equalsIgnoreCase(sub.getStatus()) ? 1.0 : 0.0;
            SUBSYSTEM_STATUS.labels(sub.getName(), sub.getLibrary(), sub.getStatus()).set(statusValue);
            SUBSYSTEM_INFO.labels(sub.getName(), sub.getLibrary(),
                    sub.getDescription() != null ? sub.getDescription() : "").set(1);
        }

        logger.fine("Subsystem metrics updated. Total: " + subsystems.size());
    }

    public void updateMetricsForNotFound(String uniqueKey, String subsystemName, String subsystemLibrary) {
        SUBSYSTEM_STATUS.labels(subsystemName, subsystemLibrary, "NOT FOUND").set(0);
    }
}
