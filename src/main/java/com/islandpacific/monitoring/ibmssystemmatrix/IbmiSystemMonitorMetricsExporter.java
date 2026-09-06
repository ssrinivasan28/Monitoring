package com.islandpacific.monitoring.ibmssystemmatrix;

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

public class IbmiSystemMonitorMetricsExporter {

    private static final Logger logger = Logger.getLogger(IbmiSystemMonitorMetricsExporter.class.getName());

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("ibmi_matrix_monitor_uptime_seconds")
            .help("Uptime of the IBM i matrix monitor in seconds.")
            .register();

    private static final Gauge LAST_UPDATE_TIMESTAMP = Gauge.build()
            .name("ibmi_matrix_monitor_last_metrics_update_timestamp_seconds")
            .help("Last time metrics were updated in epoch seconds.")
            .register();

    private static final Gauge CPU_UTILIZATION = Gauge.build()
            .name("ibmi_cpu_utilization_percent")
            .help("IBM i CPU utilization percentage.")
            .labelNames("host")
            .register();

    private static final Gauge ASP_UTILIZATION = Gauge.build()
            .name("ibmi_asp_utilization_percent")
            .help("IBM i ASP utilization percentage.")
            .labelNames("host")
            .register();

    private static final Gauge SHARED_POOL_UTILIZATION = Gauge.build()
            .name("ibmi_shared_processor_pool_utilization_percent")
            .help("IBM i shared processor pool utilization percentage.")
            .labelNames("host")
            .register();

    private static final Gauge TOTAL_JOBS = Gauge.build()
            .name("ibmi_total_jobs_in_system")
            .help("Total jobs in the IBM i system.")
            .labelNames("host")
            .register();

    private static final Gauge ACTIVE_JOBS = Gauge.build()
            .name("ibmi_active_jobs_in_system")
            .help("Active jobs in the IBM i system.")
            .labelNames("host")
            .register();

    private final int port;
    private HttpServer server;

    public IbmiSystemMonitorMetricsExporter(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new IOException("Cannot bind metrics server to port " + port +
                    " — is another instance already running? Error: " + e.getMessage(), e);
        }
        server.createContext("/metrics", httpExchange -> {
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
        logger.info("IBM i matrix metrics server started on port " + port + "/metrics");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("IBM i matrix metrics server stopped on port " + port);
        }
    }

    public void updateMetrics(List<IbmiSystemMonitorInfo> infos) {
        long now = Instant.now().getEpochSecond();
        long uptimeSeconds = (System.currentTimeMillis()
                - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
        LAST_UPDATE_TIMESTAMP.set(now);

        for (IbmiSystemMonitorInfo i : infos) {
            CPU_UTILIZATION.labels(i.getHost()).set(i.getCpuUtilization());
            ASP_UTILIZATION.labels(i.getHost()).set(i.getAspUtilization());
            SHARED_POOL_UTILIZATION.labels(i.getHost()).set(i.getSharedPoolUtilization());
            TOTAL_JOBS.labels(i.getHost()).set(i.getTotalJobs());
            ACTIVE_JOBS.labels(i.getHost()).set(i.getActiveJobs());
        }
    }
}
