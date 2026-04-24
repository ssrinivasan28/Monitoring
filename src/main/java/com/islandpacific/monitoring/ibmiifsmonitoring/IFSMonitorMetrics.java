package com.islandpacific.monitoring.ibmiifsmonitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap; // Added import for ConcurrentHashMap
import java.util.logging.Logger; // Added import for Logger


public class IFSMonitorMetrics {

    // Gauge for application uptime, calculated from process start time
    private static final Gauge UPTIME_SECONDS = Gauge.build()
            .name("ifs_monitor_uptime_seconds")
            .help("Uptime of the IFS monitor application in seconds.")
            .register();

    // Gauge for the timestamp of the last completed scan
    private static final Gauge LAST_SCAN_TIMESTAMP_SECONDS = Gauge.build()
            .name("ifs_monitor_last_scan_timestamp_seconds")
            .help("Last time an IFS folder scan was completed in epoch seconds.")
            .register();

    // Gauge for the current file count in a specific IFS folder
    private static final Gauge IFS_FILE_COUNT_CURRENT = Gauge.build()
            .name("ifs_folder_file_count_current")
            .help("Current number of files in a monitored IFS folder.")
            .labelNames("location") // Label by location name
            .register();

    // Counter for alerts sent due to too few files
    private static final Counter IFS_TOO_FEW_FILES_ALERTS_TOTAL = Counter.build()
            .name("ifs_too_few_files_alerts_total")
            .help("Total number of alerts sent due to file count being below minimum threshold.")
            .labelNames("location")
            .register();

    // Counter for alerts sent due to too many files
    private static final Counter IFS_TOO_MANY_FILES_ALERTS_TOTAL = Counter.build()
            .name("ifs_too_many_files_alerts_total")
            .help("Total number of alerts sent due to file count exceeding maximum threshold.")
            .labelNames("location")
            .register();

    public IFSMonitorMetrics(Logger logger,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
    }

    public void setLastScanTimestamp(long timestamp) {
        LAST_SCAN_TIMESTAMP_SECONDS.set(timestamp);
    }

    /**
     * Updates the uptime metric based on the application process's start time.
     * This method provides a more robust uptime calculation.
     */
    public static void updateUptime() {
        long processStartTimeMillis = ProcessHandle.current().info().startInstant()
                                        .orElse(Instant.EPOCH).toEpochMilli();
        long uptimeSeconds = (System.currentTimeMillis() - processStartTimeMillis) / 1000;
        UPTIME_SECONDS.set(uptimeSeconds);
    }

    /**
     * Sets the current file count for a specific IFS folder.
     * @param locationName The name of the monitoring location.
     * @param count The current number of files.
     */
    public static void setFileCount(String locationName, int count) {
        IFS_FILE_COUNT_CURRENT.labels(locationName).set(count);
    }

    /**
     * Increments the counter for "too few files" alerts for a specific location.
     * @param locationName The name of the monitoring location.
     */
    public static void incrementTooFewFilesAlert(String locationName) {
        IFS_TOO_FEW_FILES_ALERTS_TOTAL.labels(locationName).inc();
    }

    /**
     * Increments the counter for "too many files" alerts for a specific location.
     * @param locationName The name of the monitoring location.
     */
    public static void incrementTooManyFilesAlert(String locationName) {
        IFS_TOO_MANY_FILES_ALERTS_TOTAL.labels(locationName).inc();
    }
}
