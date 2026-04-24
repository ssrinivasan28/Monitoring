package com.islandpacific.monitoring.logkeywordmonitoring;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Prometheus metrics exporter for Log Keyword Monitor.
 * Exposes metrics about keyword matches, lines scanned, and errors.
 */
public class LogKeywordMonitorMetrics extends Collector {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts;
    private final ConcurrentHashMap<String, Long> linesScanned;
    private final ConcurrentHashMap<String, Long> readErrors;

    public LogKeywordMonitorMetrics(
            Logger logger,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts,
            ConcurrentHashMap<String, Long> linesScanned,
            ConcurrentHashMap<String, Long> readErrors) {
        this.totalMatchCounts = totalMatchCounts;
        this.newMatchCounts = newMatchCounts;
        this.linesScanned = linesScanned;
        this.readErrors = readErrors;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        // Metric: log_keyword_matches_total
        GaugeMetricFamily totalMatches = new GaugeMetricFamily(
                "log_keyword_matches_total",
                "Total number of keyword matches per log file",
                Arrays.asList("logfile", "keyword"));

        for (Map.Entry<String, ConcurrentHashMap<String, Long>> fileEntry : totalMatchCounts.entrySet()) {
            String logFile = fileEntry.getKey();
            for (Map.Entry<String, Long> keywordEntry : fileEntry.getValue().entrySet()) {
                String keyword = keywordEntry.getKey();
                long count = keywordEntry.getValue();
                totalMatches.addMetric(Arrays.asList(logFile, keyword), count);
            }
        }
        mfs.add(totalMatches);

        // Metric: log_keyword_matches_new
        GaugeMetricFamily newMatches = new GaugeMetricFamily(
                "log_keyword_matches_new",
                "New keyword matches since last check",
                Arrays.asList("logfile", "keyword"));

        for (Map.Entry<String, ConcurrentHashMap<String, Long>> fileEntry : newMatchCounts.entrySet()) {
            String logFile = fileEntry.getKey();
            for (Map.Entry<String, Long> keywordEntry : fileEntry.getValue().entrySet()) {
                String keyword = keywordEntry.getKey();
                long count = keywordEntry.getValue();
                newMatches.addMetric(Arrays.asList(logFile, keyword), count);
            }
        }
        mfs.add(newMatches);

        // Metric: log_file_lines_scanned_total
        GaugeMetricFamily linesScannedMetric = new GaugeMetricFamily(
                "log_file_lines_scanned_total",
                "Total number of lines scanned per log file",
                Collections.singletonList("logfile"));

        for (Map.Entry<String, Long> entry : linesScanned.entrySet()) {
            linesScannedMetric.addMetric(Collections.singletonList(entry.getKey()), entry.getValue());
        }
        mfs.add(linesScannedMetric);

        // Metric: log_file_read_errors_total
        GaugeMetricFamily readErrorsMetric = new GaugeMetricFamily(
                "log_file_read_errors_total",
                "Total number of read errors per log file",
                Collections.singletonList("logfile"));

        for (Map.Entry<String, Long> entry : readErrors.entrySet()) {
            readErrorsMetric.addMetric(Collections.singletonList(entry.getKey()), entry.getValue());
        }
        mfs.add(readErrorsMetric);

        return mfs;
    }
}
