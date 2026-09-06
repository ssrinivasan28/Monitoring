package com.islandpacific.monitoring.ibmsqlthresholdmonitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.time.Instant;

/**
 * Prometheus metrics for IBM SQL Threshold Monitor.
 */
public class SqlThresholdMetrics {

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("sql_threshold_monitor_uptime_seconds")
            .help("Uptime of the IBM SQL Threshold monitor application in seconds.")
            .register();

    private static final Gauge LAST_SCAN_TIMESTAMP_SECONDS = Gauge.build()
            .name("sql_threshold_monitor_last_scan_timestamp_seconds")
            .help("Last time a SQL threshold scan was completed in epoch seconds.")
            .register();

    private static final Gauge SQL_ROW_COUNT_CURRENT = Gauge.build()
            .name("sql_threshold_row_count_current")
            .help("Current row count returned by a monitored SQL check. -1 indicates an error executing the query.")
            .labelNames("check")
            .register();

    private static final Counter SQL_THRESHOLD_ALERTS_TOTAL = Counter.build()
            .name("sql_threshold_alerts_total")
            .help("Total number of alerts sent due to a SQL check exceeding its row count threshold.")
            .labelNames("check")
            .register();

    public static void updateUptime() {
        long processStartTimeMillis = ProcessHandle.current().info().startInstant()
                .orElse(Instant.EPOCH).toEpochMilli();
        long uptimeSeconds = (System.currentTimeMillis() - processStartTimeMillis) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
    }

    public static void setLastScanTimestamp(long timestamp) {
        LAST_SCAN_TIMESTAMP_SECONDS.set(timestamp);
    }

    public static void setRowCount(String checkCode, int count) {
        SQL_ROW_COUNT_CURRENT.labels(checkCode).set(count);
    }

    public static void setErrorState(String checkCode) {
        SQL_ROW_COUNT_CURRENT.labels(checkCode).set(-1);
    }

    public static void incrementAlert(String checkCode) {
        SQL_THRESHOLD_ALERTS_TOTAL.labels(checkCode).inc();
    }
}
