package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Prometheus metrics for Windows File System Cardinality Monitor.
 */
public class FileSystemCardinalityMetrics {

    // Gauge for application uptime
    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("fs_cardinality_monitor_uptime_seconds")
            .help("Uptime of the Windows File System Cardinality monitor application in seconds.")
            .register();

    // Gauge for the timestamp of the last completed scan
    private static final Gauge LAST_SCAN_TIMESTAMP_SECONDS = Gauge.build()
            .name("fs_cardinality_monitor_last_scan_timestamp_seconds")
            .help("Last time a Windows folder scan was completed in epoch seconds.")
            .register();

    // Gauge for the current file count in a specific folder
    private static final Gauge FS_FILE_COUNT_CURRENT = Gauge.build()
            .name("fs_folder_file_count_current")
            .help("Current number of files in a monitored Windows folder.")
            .labelNames("location")
            .register();

    // Counter for alerts sent due to too few files
    private static final Counter FS_TOO_FEW_FILES_ALERTS_TOTAL = Counter.build()
            .name("fs_too_few_files_alerts_total")
            .help("Total number of alerts sent due to file count being below minimum threshold.")
            .labelNames("location")
            .register();

    // Counter for alerts sent due to too many files
    private static final Counter FS_TOO_MANY_FILES_ALERTS_TOTAL = Counter.build()
            .name("fs_too_many_files_alerts_total")
            .help("Total number of alerts sent due to file count exceeding maximum threshold.")
            .labelNames("location")
            .register();

    public FileSystemCardinalityMetrics(Logger logger,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
    }

    public void setLastScanTimestamp(long timestamp) {
        LAST_SCAN_TIMESTAMP_SECONDS.set(timestamp);
    }

    /**
     * Updates the uptime metric based on the application process's start time.
     */
    public static void updateUptime() {
        long processStartTimeMillis = ProcessHandle.current().info().startInstant()
                                        .orElse(Instant.EPOCH).toEpochMilli();
        long uptimeSeconds = (System.currentTimeMillis() - processStartTimeMillis) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
    }

    /**
     * Sets the current file count for a specific folder.
     * @param locationName The name of the monitoring location.
     * @param count The current number of files.
     */
    public static void setFileCount(String locationName, int count) {
        FS_FILE_COUNT_CURRENT.labels(locationName).set(count);
    }

    /**
     * Increments the counter for "too few files" alerts for a specific location.
     * @param locationName The name of the monitoring location.
     */
    public static void incrementTooFewFilesAlert(String locationName) {
        FS_TOO_FEW_FILES_ALERTS_TOTAL.labels(locationName).inc();
    }

    /**
     * Increments the counter for "too many files" alerts for a specific location.
     * @param locationName The name of the monitoring location.
     */
    public static void incrementTooManyFilesAlert(String locationName) {
        FS_TOO_MANY_FILES_ALERTS_TOTAL.labels(locationName).inc();
    }
}
