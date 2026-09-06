package com.islandpacific.monitoring.ibmierrormonitoring;

import io.prometheus.client.Gauge;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class IFSErrorMonitorMetrics {

    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("error_notifier_uptime_seconds")
            .help("Uptime of the error notifier application in seconds.")
            .register();

    private static final Gauge LAST_SCAN_TIMESTAMP_SECONDS = Gauge.build()
            .name("error_notifier_last_scan_timestamp_seconds")
            .help("Last time a scan was completed in epoch seconds.")
            .register();

    private static final Gauge TOTAL_FILES = Gauge.build()
            .name("error_notifier_total_files")
            .help("Total number of files found for a given location and file type.")
            .labelNames("location", "file_type")
            .register();

    private static final Gauge NEW_FILES_DETECTED = Gauge.build()
            .name("error_notifier_new_files_detected")
            .help("Number of new files detected in the last scan for a given location and file type.")
            .labelNames("location", "file_type")
            .register();

    public IFSErrorMonitorMetrics(Logger logger,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
    }

    public void setLastScanTimestamp(long timestamp) {
        LAST_SCAN_TIMESTAMP_SECONDS.set(timestamp);
    }

    public static void updateUptime() {
        long processStartMillis = ProcessHandle.current().info().startInstant()
                .orElse(Instant.EPOCH).toEpochMilli();
        UPTIME_SECONDS.set((System.currentTimeMillis() - processStartMillis) / 1000.0);
    }

    public static void updateCountMetrics(
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
        totalFileCounts.forEach((location, extCounts) ->
            extCounts.forEach((ext, count) ->
                TOTAL_FILES.labels(location, ext.startsWith(".") ? ext.substring(1) : ext).set(count)));
        newFileCounts.forEach((location, extCounts) ->
            extCounts.forEach((ext, count) ->
                NEW_FILES_DETECTED.labels(location, ext.startsWith(".") ? ext.substring(1) : ext).set(count)));
    }

    public String generateMetrics() {
        return "";
    }
}
