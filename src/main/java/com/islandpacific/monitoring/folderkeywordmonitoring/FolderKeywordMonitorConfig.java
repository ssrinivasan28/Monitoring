package com.islandpacific.monitoring.folderkeywordmonitoring;

import java.util.*;

public class FolderKeywordMonitorConfig {

    private final List<String> folderPaths;
    private final List<String> keywords;
    private final boolean caseSensitive;
    private final boolean recursive;
    private final int checkIntervalMinutes;
    private final int metricsPort;
    private final String clientName;

    public FolderKeywordMonitorConfig(Properties props) {
        this.folderPaths = parseFolderPaths(props);
        this.keywords = parseKeywords(props.getProperty("monitor.keywords", ""));
        this.caseSensitive = Boolean.parseBoolean(props.getProperty("monitor.case.sensitive", "false"));
        this.recursive = Boolean.parseBoolean(props.getProperty("monitor.recursive", "false"));
        this.checkIntervalMinutes = Integer.parseInt(props.getProperty("check.interval.minutes", "60"));
        this.metricsPort = Integer.parseInt(props.getProperty("metrics.port", "3026"));
        this.clientName = props.getProperty("client.name", "FolderLogKeywordMonitor");
    }

    private List<String> parseFolderPaths(Properties props) {
        String raw = props.getProperty("monitor.folder.paths", props.getProperty("monitor.folder.path", ""));
        List<String> list = new ArrayList<>();
        for (String p : raw.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Missing required property: monitor.folder.paths");
        }
        return list;
    }

    private List<String> parseKeywords(String raw) {
        List<String> list = new ArrayList<>();
        for (String k : raw.split(",")) {
            String trimmed = k.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }

    public List<String> getFolderPaths() { return Collections.unmodifiableList(folderPaths); }
    public List<String> getKeywords() { return Collections.unmodifiableList(keywords); }
    public boolean isCaseSensitive() { return caseSensitive; }
    public boolean isRecursive() { return recursive; }
    public int getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public int getMetricsPort() { return metricsPort; }
    public String getClientName() { return clientName; }
}
