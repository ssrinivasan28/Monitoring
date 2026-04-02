package com.islandpacific.monitoring.filesystemerrormonitoring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.time.Instant;

/**
 * Service class responsible for generating Prometheus-style metrics
 * for the Windows File System Error Monitor application.
 */
public class FileSystemErrorMetrics {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private volatile long lastScanTimestamp;

    public FileSystemErrorMetrics(Logger logger,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
        this.totalFileCounts = totalFileCounts;
        this.newFileCounts = newFileCounts;
        this.lastScanTimestamp = 0;
    }

    public void setLastScanTimestamp(long timestamp) {
        this.lastScanTimestamp = timestamp;
    }

    /**
     * Generates Prometheus-style metrics as a String.
     *
     * @return A String containing the metrics in Prometheus exposition format.
     */
    public String generateMetrics() {
        StringBuilder metrics = new StringBuilder();

        long uptimeSeconds = (System.currentTimeMillis() - ProcessHandle.current().info().startInstant().orElse(Instant.EPOCH).toEpochMilli()) / 1000;
        metrics.append("# HELP fs_error_monitor_uptime_seconds Uptime of the Windows File System Error monitor application in seconds.\n");
        metrics.append("# TYPE fs_error_monitor_uptime_seconds gauge\n");
        metrics.append("fs_error_monitor_uptime_seconds ").append(uptimeSeconds).append("\n");

        metrics.append("# HELP fs_error_monitor_last_scan_timestamp_seconds Last time a scan was completed in epoch seconds.\n");
        metrics.append("# TYPE fs_error_monitor_last_scan_timestamp_seconds gauge\n");
        metrics.append("fs_error_monitor_last_scan_timestamp_seconds ").append(lastScanTimestamp).append("\n");

        metrics.append("# HELP fs_error_total_files Total number of error files found for a given location and file type.\n");
        metrics.append("# TYPE fs_error_total_files gauge\n");
        totalFileCounts.forEach((location, extCounts) -> {
            extCounts.forEach((ext, count) -> {
                metrics.append(String.format("fs_error_total_files{location=\"%s\",file_type=\"%s\"} %d\n",
                        location, ext.substring(1), count));
            });
        });

        metrics.append("# HELP fs_error_new_files_detected Total number of new error files detected for a given location and file type.\n");
        metrics.append("# TYPE fs_error_new_files_detected counter\n");
        newFileCounts.forEach((location, extCounts) -> {
            extCounts.forEach((ext, count) -> {
                metrics.append(String.format("fs_error_new_files_detected{location=\"%s\",file_type=\"%s\"} %d\n",
                        location, ext.substring(1), count));
            });
        });

        return metrics.toString();
    }
}
