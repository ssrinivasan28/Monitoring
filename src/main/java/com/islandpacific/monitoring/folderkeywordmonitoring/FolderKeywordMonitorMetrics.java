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
        List<String> labelNames = Collections.singletonList("folder");

        GaugeMetricFamily matched = new GaugeMetricFamily(
                "folder_keyword_files_matched_total",
                "Total files where a keyword was found",
                labelNames);
        for (Map.Entry<String, Long> entry : totalFilesMatched.entrySet()) {
            matched.addMetric(Collections.singletonList(entry.getKey()), entry.getValue());
        }
        mfs.add(matched);

        GaugeMetricFamily scanned = new GaugeMetricFamily(
                "folder_keyword_files_scanned_total",
                "Total files scanned",
                labelNames);
        for (Map.Entry<String, Long> entry : totalFilesScanned.entrySet()) {
            scanned.addMetric(Collections.singletonList(entry.getKey()), entry.getValue());
        }
        mfs.add(scanned);

        return mfs;
    }
}
