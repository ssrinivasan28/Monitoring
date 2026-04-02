package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference; // Import AtomicReference
import java.util.logging.Level;
import java.util.logging.Logger;


public class SubsystemMetricsExporter {

    private static final Logger logger = Logger.getLogger(SubsystemMetricsExporter.class.getName());
    private static final String METRICS_PATH = "/metrics";

    private final int port;
    private HttpServer server;

    // Use ConcurrentHashMap to store the latest subsystem info, keyed by "LIBRARY/SUBSYSTEM_DESCRIPTION"
    private final ConcurrentHashMap<String, SubsystemInfo> latestSubsystemMetrics = new ConcurrentHashMap<>();
    // Use AtomicReference for lastMetricsUpdateTime for thread-safe updates in a concurrent environment
    private final AtomicReference<Long> lastMetricsUpdateTime = new AtomicReference<>(0L); 


    public SubsystemMetricsExporter(int port) {
        this.port = port;
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
        logger.info("Subsystem Metrics server started on port " + port + METRICS_PATH);
    }

    /**
     * Stops the HTTP server gracefully.
     */
    public void stop() {
        if (server != null) {
            server.stop(0); // Stop immediately
            logger.info("Subsystem Metrics server stopped on port " + port);
        }
    }


    public void updateMetrics(List<SubsystemInfo> subsystems) {
      
        
       
        latestSubsystemMetrics.clear(); 
        for (SubsystemInfo sub : subsystems) {
            // Key by "LIBRARY/SUBSYSTEM_DESCRIPTION" for uniqueness in metrics
            String uniqueKey = sub.getLibrary() + "/" + sub.getName();
            latestSubsystemMetrics.put(uniqueKey, sub);
        }
        this.lastMetricsUpdateTime.set(Instant.now().getEpochSecond()); // Update using set()
        logger.fine("Subsystem metrics updated. Total: " + subsystems.size());
    }

  
    public void updateMetricsForNotFound(String uniqueKey, String subsystemName, String subsystemLibrary) {
        // Add it with a dummy SubsystemInfo to ensure it appears in metrics as inactive.
        latestSubsystemMetrics.put(uniqueKey, new SubsystemInfo(subsystemName, subsystemName, "NOT FOUND", subsystemLibrary));
        // Note: lastMetricsUpdateTime is primarily updated by the main updateMetrics call.
    }

    /**
     * Generates the Prometheus text exposition format for the collected metrics.
     *
     * @return A string containing the metrics data.
     */
    private String generateMetrics() {
        StringBuilder metrics = new StringBuilder();

        long uptimeSeconds = (System.currentTimeMillis() - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        metrics.append("# HELP ibmi_subsystem_monitor_uptime_seconds Uptime of the IBM i subsystem monitor application in seconds.\n");
        metrics.append("# TYPE ibmi_subsystem_monitor_uptime_seconds gauge\n");
        metrics.append("ibmi_subsystem_monitor_uptime_seconds ").append(uptimeSeconds).append("\n");

        metrics.append("# HELP ibmi_subsystem_monitor_last_metrics_update_timestamp_seconds Last time subsystem metrics were updated in epoch seconds.\n");
        metrics.append("# TYPE ibmi_subsystem_monitor_last_metrics_update_timestamp_seconds gauge\n");
        metrics.append("ibmi_subsystem_monitor_last_metrics_update_timestamp_seconds ").append(lastMetricsUpdateTime.get()).append("\n");

        // Metrics for each subsystem
        for (Map.Entry<String, SubsystemInfo> entry : latestSubsystemMetrics.entrySet()) {
            SubsystemInfo sub = entry.getValue();
            // Subsystem Status (0 for inactive/not found, 1 for active)
            // Map 'ACTIVE' to 1 and anything else (INACTIVE, ENDING, NOT FOUND) to 0.
            double statusValue = "ACTIVE".equalsIgnoreCase(sub.getStatus()) ? 1.0 : 0.0;
            metrics.append("# HELP ibmi_subsystem_status Current operational status of the IBM i subsystem (1=active, 0=inactive/other).\n");
            metrics.append("# TYPE ibmi_subsystem_status gauge\n");
            // Use subsystem_name and subsystem_library labels
            metrics.append(String.format("ibmi_subsystem_status{subsystem_name=\"%s\", subsystem_library=\"%s\", status_text=\"%s\"} %.0f\n",
                                        sub.getName(), sub.getLibrary(), sub.getStatus(), statusValue));
            
            // Subsystem Info (static info as a gauge with value 1, using labels)
            metrics.append("# HELP ibmi_subsystem_info General information about the IBM i subsystem.\n");
            metrics.append("# TYPE ibmi_subsystem_info gauge\n");
            metrics.append(String.format("ibmi_subsystem_info{subsystem_name=\"%s\", subsystem_library=\"%s\", description=\"%s\"} 1\n",
                                        sub.getName(), sub.getLibrary(), sub.getDescription()));
        }

        return metrics.toString();
    }
}