package com.islandpacific.monitoring.folderkeywordmonitoring;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FolderKeywordMonitorMetrics extends Collector {

    private final ConcurrentHashMap<String, Long> totalFilesMatched;
    private final ConcurrentHashMap<String, Long> totalFilesScanned;

    public FolderKeywordMonitorMetrics(Logger logger,
            ConcurrentHashMap<String, Long> totalFilesMatched,
            ConcurrentHashMap<String, Long> totalFilesScanned) {
        this.totalFilesMatched = totalFilesMatched;
        this.totalFilesScanned = totalFilesScanned;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        GaugeMetricFamily matched = new GaugeMetricFamily(
                "folder_keyword_files_matched_total",
                "Total files where a keyword was found",
                Collections.emptyList());
        matched.addMetric(Collections.emptyList(),
                totalFilesMatched.getOrDefault("total", 0L));
        mfs.add(matched);

        GaugeMetricFamily scanned = new GaugeMetricFamily(
                "folder_keyword_files_scanned_total",
                "Total files scanned",
                Collections.emptyList());
        scanned.addMetric(Collections.emptyList(),
                totalFilesScanned.getOrDefault("total", 0L));
        mfs.add(scanned);

        return mfs;
    }
}
