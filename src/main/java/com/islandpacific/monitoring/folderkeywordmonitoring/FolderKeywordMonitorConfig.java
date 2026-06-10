package com.islandpacific.monitoring.folderkeywordmonitoring;

import java.util.*;

public class FolderKeywordMonitorConfig {

    private final String folderPath;
    private final List<String> keywords;
    private final boolean caseSensitive;
    private final int checkIntervalMinutes;
    private final int metricsPort;
    private final String clientName;

    public FolderKeywordMonitorConfig(Properties props) {
        this.folderPath = require(props, "monitor.folder.path");
        this.keywords = parseKeywords(props.getProperty("monitor.keywords", ""));
        this.caseSensitive = Boolean.parseBoolean(props.getProperty("monitor.case.sensitive", "false"));
        this.checkIntervalMinutes = Integer.parseInt(props.getProperty("check.interval.minutes", "60"));
        this.metricsPort = Integer.parseInt(props.getProperty("metrics.port", "3026"));
        this.clientName = props.getProperty("client.name", "FolderLogKeywordMonitor");
    }

    private String require(Properties props, String key) {
        String val = props.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required property: " + key);
        }
        return val.trim();
    }

    private List<String> parseKeywords(String raw) {
        List<String> list = new ArrayList<>();
        for (String k : raw.split(",")) {
            String trimmed = k.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }

    public String getFolderPath() { return folderPath; }
    public List<String> getKeywords() { return Collections.unmodifiableList(keywords); }
    public boolean isCaseSensitive() { return caseSensitive; }
    public int getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public int getMetricsPort() { return metricsPort; }
    public String getClientName() { return clientName; }
}
