package com.islandpacific.monitoring.windowsmonitoring;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WindowsMonitorMetricsExporter {
    private final int port;
    private HTTPServer server;

    private static final Gauge CPU_USAGE = Gauge.build()
            .name("windows_cpu_usage_percent")
            .help("Windows CPU usage percentage")
            .labelNames("server")
            .register();

    private static final Gauge MEM_TOTAL = Gauge.build()
            .name("windows_memory_total_gb")
            .help("Windows Total Memory in GB")
            .labelNames("server")
            .register();

    private static final Gauge MEM_USED = Gauge.build()
            .name("windows_memory_used_gb")
            .help("Windows Used Memory in GB")
            .labelNames("server")
            .register();

    private static final Gauge MEM_FREE = Gauge.build()
            .name("windows_memory_free_gb")
            .help("Windows Free Memory in GB")
            .labelNames("server")
            .register();

    private static final Gauge MEM_USAGE_PCT = Gauge.build()
            .name("windows_memory_usage_percent")
            .help("Windows Memory usage percentage")
            .labelNames("server")
            .register();

    private static final Gauge UPTIME = Gauge.build()
            .name("windows_system_uptime_hours")
            .help("Windows System Uptime in Hours")
            .labelNames("server")
            .register();

    private static final Gauge DISK_TOTAL = Gauge.build()
            .name("windows_disk_total_gb")
            .help("Windows Disk Total Size in GB")
            .labelNames("server", "drive")
            .register();

    private static final Gauge DISK_USED = Gauge.build()
            .name("windows_disk_used_gb")
            .help("Windows Disk Used Space in GB")
            .labelNames("server", "drive")
            .register();

    private static final Gauge DISK_USAGE_PCT = Gauge.build()
            .name("windows_disk_usage_percent")
            .help("Windows Disk Usage Percentage")
            .labelNames("server", "drive")
            .register();

    private static final Gauge SERVICE_STATUS = Gauge.build()
            .name("windows_service_status")
            .help("Windows Service Status (1=Running, 0=Other)")
            .labelNames("server", "service")
            .register();

    public WindowsMonitorMetricsExporter(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = new HTTPServer(port);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public void updateMetrics(List<WindowsMonitorInfo> infos) {
        for (WindowsMonitorInfo info : infos) {
            String host = info.getHostName();
            CPU_USAGE.labels(host).set(info.getCpuUtilization());
            MEM_TOTAL.labels(host).set(info.getMemoryTotalGB());
            MEM_USED.labels(host).set(info.getMemoryUsedGB());
            MEM_FREE.labels(host).set(info.getMemoryFreeGB());
            MEM_USAGE_PCT.labels(host).set(info.getMemoryUtilization());
            UPTIME.labels(host).set(info.getSystemUptimeHours());

            // Disk Metrics
            if (info.getDisks() != null) {
                for (Map.Entry<String, WindowsMonitorInfo.DiskInfo> entry : info.getDisks().entrySet()) {
                    String drive = entry.getKey();
                    WindowsMonitorInfo.DiskInfo d = entry.getValue();
                    DISK_TOTAL.labels(host, drive).set(d.getTotalGB());
                    DISK_USED.labels(host, drive).set(d.getUsedGB());
                    DISK_USAGE_PCT.labels(host, drive).set(d.getUsagePercent());
                }
            }

            // Service Metrics
            if (info.getServiceStatuses() != null) {
                for (Map.Entry<String, String> entry : info.getServiceStatuses().entrySet()) {
                    String service = entry.getKey();
                    String status = entry.getValue();
                    SERVICE_STATUS.labels(host, service).set("Running".equalsIgnoreCase(status) ? 1 : 0);
                }
            }
        }
    }
}
