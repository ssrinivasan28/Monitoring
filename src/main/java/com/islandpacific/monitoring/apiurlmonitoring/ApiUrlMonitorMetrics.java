package com.islandpacific.monitoring.apiurlmonitoring;

import io.prometheus.client.Gauge;

public class ApiUrlMonitorMetrics {

    private static final Gauge API_STATUS = Gauge.build()
            .name("api_url_status")
            .help("API URL status: 1=UP, 0=DOWN")
            .labelNames("url_code", "url")
            .register();

    private static final Gauge API_RESPONSE_TIME_MS = Gauge.build()
            .name("api_url_response_time_ms")
            .help("Last response time in milliseconds (-1 if connection failed)")
            .labelNames("url_code", "url")
            .register();

    private static final Gauge MONITOR_UPTIME = Gauge.build()
            .name("api_url_monitor_uptime_seconds")
            .help("Seconds since API URL monitor started")
            .register();

    private final long startTime = System.currentTimeMillis();

    public void update(String code, String url, double status, long responseTimeMs) {
        API_STATUS.labels(code, url).set(status);
        API_RESPONSE_TIME_MS.labels(code, url).set(responseTimeMs);
    }

    public void updateUptime() {
        MONITOR_UPTIME.set((System.currentTimeMillis() - startTime) / 1000.0);
    }
}
