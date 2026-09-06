package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class MetricsServer {

    private static final Logger logger = Logger.getLogger(MetricsServer.class.getName());
    private static final String METRICS_PATH = "/metrics";

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("job_queue_monitor_uptime_seconds")
            .help("Uptime of the job queue monitor application in seconds.")
            .register();

    private static final Gauge LAST_SCAN_TIMESTAMP = Gauge.build()
            .name("job_queue_monitor_last_overall_scan_timestamp_seconds")
            .help("Last time an overall scan was completed in epoch seconds.")
            .register();

    private static final Gauge WAITING_JOBS = Gauge.build()
            .name("job_queue_monitor_waiting_jobs")
            .help("Current number of jobs waiting in the monitored job queue.")
            .labelNames("job_queue_id", "job_queue_name", "job_queue_library")
            .register();

    private static final Gauge THRESHOLD = Gauge.build()
            .name("job_queue_monitor_threshold")
            .help("Configured threshold for the job queue.")
            .labelNames("job_queue_id", "job_queue_name", "job_queue_library")
            .register();

    private final int port;
    private HttpServer server;
    private final Map<String, JobQueueInfo> jobQueueById;

    public MetricsServer(int port, List<JobQueueInfo> configuredJobQueues) {
        this.port = port;
        Map<String, JobQueueInfo> map = new HashMap<>();
        for (JobQueueInfo jq : configuredJobQueues) {
            map.put(jq.getId(), jq);
            WAITING_JOBS.labels(jq.getId(), jq.getName(), jq.getLibrary()).set(0);
            THRESHOLD.labels(jq.getId(), jq.getName(), jq.getLibrary()).set(jq.getThreshold());
        }
        this.jobQueueById = Collections.unmodifiableMap(map);
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
        logger.info("Metrics server started on port " + port + METRICS_PATH);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Metrics server stopped on port " + port);
        }
    }

    public void updateJobCountMetric(String jobQueueId, int count) {
        JobQueueInfo jq = jobQueueById.get(jobQueueId);
        if (jq != null) {
            WAITING_JOBS.labels(jq.getId(), jq.getName(), jq.getLibrary()).set(count);
        }
        long uptimeSeconds = (System.currentTimeMillis()
                - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
    }

    public void setLastOverallScanTimestamp(long timestamp) {
        LAST_SCAN_TIMESTAMP.set(timestamp);
    }
}
