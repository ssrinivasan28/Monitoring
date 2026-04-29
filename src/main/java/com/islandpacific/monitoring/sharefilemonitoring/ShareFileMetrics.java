package com.islandpacific.monitoring.sharefilemonitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.time.Instant;

public class ShareFileMetrics {

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("sharefile_monitor_uptime_seconds")
            .help("Uptime of the ShareFile monitor in seconds.")
            .register();

    private static final Gauge LAST_SCAN_TIMESTAMP = Gauge.build()
            .name("sharefile_monitor_last_scan_timestamp_seconds")
            .help("Epoch seconds of the last completed ShareFile scan.")
            .register();

    private static final Gauge FILE_COUNT = Gauge.build()
            .name("sharefile_folder_file_count_current")
            .help("Current file count in a monitored ShareFile folder.")
            .labelNames("location")
            .register();

    private static final Counter TOO_FEW_ALERTS = Counter.build()
            .name("sharefile_too_few_files_alerts_total")
            .help("Alerts sent due to file count below minimum threshold.")
            .labelNames("location")
            .register();

    private static final Counter TOO_MANY_ALERTS = Counter.build()
            .name("sharefile_too_many_files_alerts_total")
            .help("Alerts sent due to file count above maximum threshold.")
            .labelNames("location")
            .register();

    public static void updateUptime() {
        long startMs = ProcessHandle.current().info().startInstant()
                .orElse(Instant.EPOCH).toEpochMilli();
        UPTIME_SECONDS.set((System.currentTimeMillis() - startMs) / 1000.0);
    }

    public static void setLastScanTimestamp(long epochSeconds) {
        LAST_SCAN_TIMESTAMP.set(epochSeconds);
    }

    public static void setFileCount(String location, int count) {
        FILE_COUNT.labels(location).set(count);
    }

    public static void incrementTooFewAlert(String location) {
        TOO_FEW_ALERTS.labels(location).inc();
    }

    public static void incrementTooManyAlert(String location) {
        TOO_MANY_ALERTS.labels(location).inc();
    }
}
