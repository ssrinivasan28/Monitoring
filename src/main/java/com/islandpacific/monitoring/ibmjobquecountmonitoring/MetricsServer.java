package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MetricsServer {

    private static final Logger logger = Logger.getLogger(MetricsServer.class.getName());
    private static final String METRICS_PATH = "/metrics";

    private final int port;
    private HttpServer server;
    private final List<JobQueueInfo> configuredJobQueues; // To get threshold info for metrics
    private final ConcurrentHashMap<String, Integer> lastJobCounts; // Reference to the map in JobQueueMonitor
    private long lastOverallScanTimestamp = 0;


    public MetricsServer(int port, List<JobQueueInfo> configuredJobQueues) {
        this.port = port;
        this.configuredJobQueues = Collections.unmodifiableList(configuredJobQueues);
        this.lastJobCounts = new ConcurrentHashMap<>(); // Initialize local map
        // Populate initial map with all configured job queue IDs and a default count of 0
        for (JobQueueInfo jqInfo : configuredJobQueues) {
            this.lastJobCounts.put(jqInfo.getId(), 0);
        }
    }


    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(METRICS_PATH, httpExchange -> {
            String response = generateMetrics();
            httpExchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            httpExchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor()); // Use a single thread for metrics requests
        server.start();
        logger.info("Metrics server started on port " + port + METRICS_PATH);
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0); // Stop immediately
            logger.info("Metrics server stopped on port " + port);
        }
    }

    public void updateJobCountMetric(String jobQueueId, int count) {
        lastJobCounts.put(jobQueueId, count);
    }


    public void setLastOverallScanTimestamp(long timestamp) {
        this.lastOverallScanTimestamp = timestamp;
    }

    private String generateMetrics() {
        StringBuilder metrics = new StringBuilder();

        long uptimeSeconds = (System.currentTimeMillis() - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        metrics.append("# HELP job_queue_monitor_uptime_seconds Uptime of the job queue monitor application in seconds.\n");
        metrics.append("# TYPE job_queue_monitor_uptime_seconds gauge\n");
        metrics.append("job_queue_monitor_uptime_seconds ").append(uptimeSeconds).append("\n");

        metrics.append("# HELP job_queue_monitor_last_overall_scan_timestamp_seconds Last time an overall scan was completed in epoch seconds.\n");
        metrics.append("# TYPE job_queue_monitor_last_overall_scan_timestamp_seconds gauge\n");
        metrics.append("job_queue_monitor_last_overall_scan_timestamp_seconds ").append(lastOverallScanTimestamp).append("\n");

        metrics.append("# HELP job_queue_monitor_waiting_jobs Current number of jobs waiting in the monitored job queue.\n");
        metrics.append("# TYPE job_queue_monitor_waiting_jobs gauge\n");
        // Iterate through all monitored job queues for their last observed counts
        for (Map.Entry<String, Integer> entry : lastJobCounts.entrySet()) {
            // Find the corresponding JobQueueInfo to get name and library for labels
            JobQueueInfo currentJqInfo = configuredJobQueues.stream()
                                            .filter(jq -> jq.getId().equals(entry.getKey()))
                                            .findFirst().orElse(null);
            if (currentJqInfo != null) {
                metrics.append(String.format("job_queue_monitor_waiting_jobs{job_queue_id=\"%s\", job_queue_name=\"%s\", job_queue_library=\"%s\"} %d\n",
                                            currentJqInfo.getId(), currentJqInfo.getName(), currentJqInfo.getLibrary(), entry.getValue()));
            } else {
                 logger.log(Level.WARNING, "Metrics: Could not find JobQueueInfo for ID: " + entry.getKey());
            }
        }
        
        metrics.append("# HELP job_queue_monitor_threshold Configured threshold for the job queue.\n");
        metrics.append("# TYPE job_queue_monitor_threshold gauge\n");
        // Iterate through all configured job queues for their thresholds
        for (JobQueueInfo jqInfo : configuredJobQueues) {
            metrics.append(String.format("job_queue_monitor_threshold{job_queue_id=\"%s\", job_queue_name=\"%s\", job_queue_library=\"%s\"} %d\n",
                                        jqInfo.getId(), jqInfo.getName(), jqInfo.getLibrary(), jqInfo.getThreshold()));
        }

        return metrics.toString();
    }
}
