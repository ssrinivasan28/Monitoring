package com.islandpacific.monitoring.ibmssystemmatrix;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
public class IbmiSystemMonitorMetricsExporter {
    private final int port;
    private HttpServer server;
    private final ConcurrentHashMap<String, IbmiSystemMonitorInfo> metrics = new ConcurrentHashMap<>();
    private final AtomicLong lastUpdate = new AtomicLong(0);

    public IbmiSystemMonitorMetricsExporter(int port) { this.port = port; }

    public void start() throws java.io.IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", ex -> {
            String resp = generate();
            ex.getResponseHeaders().set("Content-Type", "text/plain");
            ex.sendResponseHeaders(200, resp.length());
            try (OutputStream os = ex.getResponseBody()) { os.write(resp.getBytes()); }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    public void stop() { if (server != null) server.stop(0); }

    public void updateMetrics(List<IbmiSystemMonitorInfo> infos) {
        metrics.clear();
        infos.forEach(i -> metrics.put(i.getHost(), i));
        lastUpdate.set(Instant.now().getEpochSecond());
    }

    private String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append("ibmi_last_update ").append(lastUpdate.get()).append("\n");
        for (IbmiSystemMonitorInfo i : metrics.values()) {
            sb.append(String.format("ibmi_cpu_utilization_percent{host=\"%s\"} %.2f\n", i.getHost(), i.getCpuUtilization()));
            sb.append(String.format("ibmi_asp_utilization_percent{host=\"%s\"} %.2f\n", i.getHost(), i.getAspUtilization()));
            sb.append(String.format("ibmi_shared_processor_pool_utilization_percent{host=\"%s\"} %.2f\n", i.getHost(), i.getSharedPoolUtilization()));
            sb.append(String.format("ibmi_total_jobs_in_system{host=\"%s\"} %d\n", i.getHost(), i.getTotalJobs()));
            sb.append(String.format("ibmi_active_jobs_in_system{host=\"%s\"} %d\n", i.getHost(), i.getActiveJobs()));
        }
        return sb.toString();
    }
}
