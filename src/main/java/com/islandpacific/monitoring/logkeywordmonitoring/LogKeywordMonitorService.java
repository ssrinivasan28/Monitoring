package com.islandpacific.monitoring.logkeywordmonitoring;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core monitoring service that scans log files for keywords.
 * Tracks file positions, detects new matches, and triggers email alerts.
 * Supports wildcard patterns for monitoring multiple log files.
 */
public class LogKeywordMonitorService {

    private final Logger logger;
    private final List<LogKeywordMonitorConfig.LogFileConfig> logFileConfigs;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts;
    private final ConcurrentHashMap<String, Long> linesScanned;
    private final ConcurrentHashMap<String, Long> readErrors;

    // Track last read position for each log file (by absolute path)
    private final Map<String, Long> filePositions;

    // Track if we've sent an alert for each keyword in each file
    private final Map<String, Set<String>> alertedKeywords;

    // Persisted state file — survives service restarts so positions are not lost
    private static final String POSITIONS_STATE_FILE = "logs/processed/lkm_positions.properties";

    // Overridable for testing
    private final String positionsStateFile;

    public LogKeywordMonitorService(
            Logger logger,
            List<LogKeywordMonitorConfig.LogFileConfig> logFileConfigs,
            EmailService emailService,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts,
            ConcurrentHashMap<String, Long> linesScanned,
            ConcurrentHashMap<String, Long> readErrors,
            LogKeywordMonitorMetrics metrics) {
        this(logger, logFileConfigs, emailService, totalMatchCounts, newMatchCounts,
                linesScanned, readErrors, metrics, POSITIONS_STATE_FILE);
    }

    LogKeywordMonitorService(
            Logger logger,
            List<LogKeywordMonitorConfig.LogFileConfig> logFileConfigs,
            EmailService emailService,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> totalMatchCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> newMatchCounts,
            ConcurrentHashMap<String, Long> linesScanned,
            ConcurrentHashMap<String, Long> readErrors,
            LogKeywordMonitorMetrics metrics,
            String positionsStateFile) {
        this.logger = logger;
        this.logFileConfigs = logFileConfigs;
        this.emailService = emailService;
        this.totalMatchCounts = totalMatchCounts;
        this.newMatchCounts = newMatchCounts;
        this.linesScanned = linesScanned;
        this.readErrors = readErrors;
        this.positionsStateFile = positionsStateFile;
        this.filePositions = new HashMap<>();
        this.alertedKeywords = new HashMap<>();
        loadPersistedPositions();
    }

    private void loadPersistedPositions() {
        Path stateFile = Paths.get(positionsStateFile);
        if (!Files.exists(stateFile)) {
            logger.info("No persisted log positions found (first run or state file missing). Full scan will run.");
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(stateFile)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                try {
                    filePositions.put(key, Long.parseLong(props.getProperty(key)));
                } catch (NumberFormatException e) {
                    logger.warning("Ignoring invalid position entry in state file for: " + key);
                }
            }
            logger.info("Loaded persisted read positions for " + filePositions.size() + " log file(s).");
        } catch (IOException e) {
            logger.warning("Could not load persisted log positions: " + e.getMessage() + ". Full scan will run.");
        }
    }

    private void persistPositions() {
        try {
            Path stateFile = Paths.get(positionsStateFile);
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            filePositions.forEach((path, pos) -> props.setProperty(path, String.valueOf(pos)));
            try (OutputStream out = Files.newOutputStream(stateFile,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(out, "LogKeywordMonitor file positions — do not edit manually");
            }
        } catch (IOException e) {
            logger.warning("Could not persist log positions: " + e.getMessage());
        }
    }

    /**
     * Main check method - scans all configured log files for keywords.
     */
    public void checkLogsAndSendAlerts() {
        logger.fine("Starting log keyword check cycle...");

        // Reset per-cycle new counts in-place (do not clear the shared map — that would zero Prometheus metrics)
        newMatchCounts.values().forEach(Map::clear);

        for (LogKeywordMonitorConfig.LogFileConfig logFileConfig : logFileConfigs) {
            try {
                // Refresh file list for patterns (detects new date-stamped files)
                if (logFileConfig.isPattern()) {
                    logFileConfig.refreshMatchingFiles();
                    logger.fine("Pattern '" + logFileConfig.getPathPattern() + "' resolved to " +
                            logFileConfig.getResolvedPaths().size() + " file(s)");
                }

                // Check each resolved file path
                for (Path logPath : logFileConfig.getResolvedPaths()) {
                    try {
                        checkSingleLogFile(logFileConfig, logPath);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error checking log file: " + logPath, e);
                        readErrors.merge(logPath.toString(), 1L, Long::sum);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing log config: " + logFileConfig.getPathPattern(), e);
            }
        }

        // Send email alerts for any new matches
        sendAlertsForNewMatches();
    }

    private void checkSingleLogFile(LogKeywordMonitorConfig.LogFileConfig logFileConfig, Path logPath)
            throws IOException {
        String logPathStr = logPath.toAbsolutePath().toString();

        // Check if file exists
        if (!Files.exists(logPath)) {
            logger.fine("Log file does not exist (yet): " + logPathStr);
            return;
        }

        // Get file size
        long currentSize = Files.size(logPath);
        long lastPosition = filePositions.getOrDefault(logPathStr, 0L);

        // If file was truncated or rotated, start from beginning
        if (currentSize < lastPosition) {
            logger.info("Log file appears to have been rotated or truncated: " + logPathStr);
            lastPosition = 0L;
            filePositions.put(logPathStr, 0L);
        }

        // If no new data, skip
        if (currentSize == lastPosition) {
            logger.fine("No new data in log file: " + logPathStr);
            return;
        }

        logger.fine("Reading new data from " + logPathStr + " (position " + lastPosition + " to " + currentSize + ")");

        // Read new lines from file using UTF-8; use a FileInputStream with skip to honour the byte offset
        long linesRead = 0;
        long newPosition = lastPosition;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(logPath.toFile())) {
            fis.skip(lastPosition);
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    linesRead++;

                    Map<String, Integer> matches = logFileConfig.findMatches(line);
                    if (!matches.isEmpty()) {
                        logger.fine("Found matches in line: " + line.substring(0, Math.min(100, line.length())));

                        totalMatchCounts.putIfAbsent(logPathStr, new ConcurrentHashMap<>());
                        newMatchCounts.putIfAbsent(logPathStr, new ConcurrentHashMap<>());

                        for (Map.Entry<String, Integer> entry : matches.entrySet()) {
                            String keyword = entry.getKey();
                            int count = entry.getValue();
                            totalMatchCounts.get(logPathStr).merge(keyword, (long) count, Long::sum);
                            newMatchCounts.get(logPathStr).merge(keyword, (long) count, Long::sum);
                            logger.info("Keyword match: '" + keyword + "' in " + logPathStr);
                        }
                    }
                }
            }
            // Record byte position as current file size (we read to EOF)
            newPosition = currentSize;
        }
        filePositions.put(logPathStr, newPosition);
        persistPositions();
        linesScanned.merge(logPathStr, linesRead, Long::sum);
        logger.fine("Scanned " + linesRead + " new lines from " + logPathStr);
    }

    private void sendAlertsForNewMatches() {
        boolean anyNew = newMatchCounts.values().stream().anyMatch(m -> !m.isEmpty());
        if (!anyNew) {
            logger.fine("No new keyword matches to alert on.");
            return;
        }

        // Build alert message with HTML table
        StringBuilder alertMessage = new StringBuilder();
        alertMessage.append("<h4 style='color: #2c3e50; margin-bottom: 15px;'>Keyword Detection Summary</h4>");
        alertMessage.append("<p style='margin-bottom: 20px;'>The following keywords were detected in monitored log files:</p>");
        
        alertMessage.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        alertMessage.append("<thead>");
        alertMessage.append("<tr style='background-color: #34495e; color: white;'>");
        alertMessage.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Log File</th>");
        alertMessage.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Keyword</th>");
        alertMessage.append("<th style='padding: 12px; text-align: center; border: 1px solid #ddd;'>New Matches</th>");
        alertMessage.append("</tr>");
        alertMessage.append("</thead>");
        alertMessage.append("<tbody>");

        boolean hasNewAlerts = false;
        int rowCount = 0;

        for (Map.Entry<String, ConcurrentHashMap<String, Long>> fileEntry : newMatchCounts.entrySet()) {
            String logFile = fileEntry.getKey();
            Map<String, Long> keywordCounts = fileEntry.getValue();

            if (keywordCounts.isEmpty()) {
                continue;
            }

            // Get config for this log file to check alert settings
            LogKeywordMonitorConfig.LogFileConfig config = findConfigForPath(logFile);
            // Default false (alert every time) when config not found — safer than silently suppressing
            boolean alertOnFirstMatch = (config != null) && config.isAlertOnFirstMatch();

            // Initialize alerted keywords set for this file
            alertedKeywords.putIfAbsent(logFile, new HashSet<>());
            Set<String> alerted = alertedKeywords.get(logFile);

            // Extract just the filename for cleaner display
            String fileName = logFile.substring(Math.max(logFile.lastIndexOf('\\'), logFile.lastIndexOf('/')) + 1);
            
            boolean firstKeywordForFile = true;

            for (Map.Entry<String, Long> keywordEntry : keywordCounts.entrySet()) {
                String keyword = keywordEntry.getKey();
                long count = keywordEntry.getValue();

                // Check if we should alert
                boolean shouldAlert = !alertOnFirstMatch || !alerted.contains(keyword);

                if (shouldAlert) {
                    // Alternate row colors
                    String rowColor = (rowCount % 2 == 0) ? "#f9f9f9" : "#ffffff";
                    
                    alertMessage.append("<tr style='background-color: ").append(rowColor).append(";'>");
                    
                    // Only show filename in first row for this file
                    if (firstKeywordForFile) {
                        alertMessage.append("<td style='padding: 10px; border: 1px solid #ddd; font-size: 12px; font-family: monospace;' title='")
                                   .append(escapeHtml(logFile)).append("'>")
                                   .append(escapeHtml(fileName)).append("</td>");
                        firstKeywordForFile = false;
                    } else {
                        alertMessage.append("<td style='padding: 10px; border: 1px solid #ddd;'></td>");
                    }
                    
                    // Keyword column with color coding
                    String keywordColor = getKeywordColor(keyword);
                    alertMessage.append("<td style='padding: 10px; border: 1px solid #ddd;'>")
                               .append("<span style='background-color: ").append(keywordColor)
                               .append("; color: white; padding: 4px 8px; border-radius: 3px; font-weight: bold; font-size: 12px;'>")
                               .append(escapeHtml(keyword)).append("</span></td>");
                    
                    // Count column
                    alertMessage.append("<td style='padding: 10px; border: 1px solid #ddd; text-align: center; font-weight: bold;'>")
                               .append(count).append("</td>");
                    
                    alertMessage.append("</tr>");
                    
                    hasNewAlerts = true;
                    rowCount++;

                    // Mark as alerted if alertOnFirstMatch is enabled
                    if (alertOnFirstMatch) {
                        alerted.add(keyword);
                    }
                }
            }
        }
        
        alertMessage.append("</tbody>");
        alertMessage.append("</table>");
        alertMessage.append("<p style='color: #7f8c8d; font-size: 13px;'>Please review the log files for full details.</p>");

        if (hasNewAlerts) {
            // Send email alert
            try {
                emailService.sendAlert("Log Keyword Alert", alertMessage.toString());
                logger.info("Email alert sent for keyword matches");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send email alert: " + e.getMessage(), e);
            }
        }
    }

    private String getKeywordColor(String keyword) {
        String keywordLower = keyword.toLowerCase();
        if (keywordLower.contains("fatal") || keywordLower.contains("outofmemory") || keywordLower.contains("stackoverflow")) {
            return "#c0392b"; // Dark red for critical
        } else if (keywordLower.contains("error") || keywordLower.contains("severe")) {
            return "#e74c3c"; // Red for errors
        } else if (keywordLower.contains("exception")) {
            return "#e67e22"; // Orange for exceptions
        } else if (keywordLower.contains("warn")) {
            return "#f39c12"; // Yellow/orange for warnings
        } else {
            return "#3498db"; // Blue for other keywords
        }
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private LogKeywordMonitorConfig.LogFileConfig findConfigForPath(String logPath) {
        for (LogKeywordMonitorConfig.LogFileConfig config : logFileConfigs) {
            // Check if this path matches any of the resolved paths
            for (Path resolvedPath : config.getResolvedPaths()) {
                if (resolvedPath.toAbsolutePath().toString().equals(logPath)) {
                    return config;
                }
            }
        }
        return null;
    }
}
