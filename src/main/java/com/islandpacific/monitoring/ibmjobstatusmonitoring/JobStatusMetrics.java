package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class JobStatusMetrics {

    private static final Logger logger = Logger.getLogger(JobStatusMetrics.class.getName());

    private static final Gauge UPTIME = Gauge.build()
            .name("ibmi_job_status_monitor_uptime_seconds")
            .help("Uptime of the IBM i Job Status monitor in seconds.")
            .register();

    private static final Gauge LAST_SCAN = Gauge.build()
            .name("ibmi_job_status_monitor_last_scan_timestamp_seconds")
            .help("Last time a scan was completed in epoch seconds.")
            .register();

    private static final Gauge MSGW_JOB_COUNT = Gauge.build()
            .name("ibmi_msgw_job_count")
            .help("Number of IBM i jobs currently in MSGW status.")
            .register();

    private static final Gauge MSGW_JOB_ELAPSED = Gauge.build()
            .name("ibmi_msgw_job_elapsed_seconds")
            .help("Elapsed seconds for a job in MSGW status.")
            .labelNames("job_name", "job_user", "job_number", "subsystem")
            .register();

    private final int port;
    private HttpServer server;

    public JobStatusMetrics(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter writer = new OutputStreamWriter(exchange.getResponseBody())) {
                TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "job-status-metrics");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        logger.info("Job status metrics server started on port " + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public void update(List<JobStatusInfo> msgwJobs) {
        long now = Instant.now().getEpochSecond();
        long uptime = (System.currentTimeMillis()
                - ProcessHandle.current().info().startInstant()
                        .orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        UPTIME.set(uptime);
        LAST_SCAN.set(now);
        MSGW_JOB_COUNT.set(msgwJobs.size());

        MSGW_JOB_ELAPSED.clear();
        for (JobStatusInfo job : msgwJobs) {
            MSGW_JOB_ELAPSED.labels(job.getJobName(), job.getJobUser(),
                    job.getJobNumber(), job.getSubsystem()).set(job.getElapsedSeconds());
        }
    }
}
