package com.islandpacific.monitoring.logkeywordmonitoring;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Configuration parser for Log Keyword Monitor.
 * Reads and parses logkeywordmonitor.properties file.
 */
public class LogKeywordMonitorConfig {

    private final List<LogFileConfig> logFileConfigs;

    public LogKeywordMonitorConfig(Properties props) {
        this.logFileConfigs = parseLogFileConfigs(props);
    }

    private List<LogFileConfig> parseLogFileConfigs(Properties props) {
        List<LogFileConfig> configs = new ArrayList<>();
        Set<String> processedIndices = new HashSet<>();

        // Find all log file configurations by looking for monitor.logfile.N.path
        // properties
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("monitor.logfile.") && key.endsWith(".path")) {
                // Extract index (e.g., "1" from "monitor.logfile.1.path")
                String indexStr = key.substring("monitor.logfile.".length(), key.lastIndexOf(".path"));

                if (!processedIndices.contains(indexStr)) {
                    processedIndices.add(indexStr);

                    String pathStr = props.getProperty("monitor.logfile." + indexStr + ".path");
                    if (pathStr != null && !pathStr.trim().isEmpty()) {
                        LogFileConfig config = parseLogFileConfig(props, indexStr, pathStr);
                        configs.add(config);
                    }
                }
            }
        }

        return configs;
    }

    private LogFileConfig parseLogFileConfig(Properties props, String index, String pathStr) {
        // Keep the original path pattern string
        String pathPattern = pathStr;

        // Parse keywords (comma-separated)
        String keywordsStr = props.getProperty("monitor.logfile." + index + ".keywords", "");
        List<String> keywords = new ArrayList<>();
        if (!keywordsStr.trim().isEmpty()) {
            for (String keyword : keywordsStr.split(",")) {
                keywords.add(keyword.trim());
            }
        }

        // Parse case sensitivity
        boolean caseSensitive = Boolean.parseBoolean(
                props.getProperty("monitor.logfile." + index + ".case.sensitive", "false"));

        // Parse regex mode
        boolean regexMode = Boolean.parseBoolean(
                props.getProperty("monitor.logfile." + index + ".regex.mode", "false"));

        // Parse alert settings
        boolean alertOnFirstMatch = Boolean.parseBoolean(
                props.getProperty("monitor.logfile." + index + ".alert.on.first.match",
                        props.getProperty("alert.on.first.match", "true")));

        // Parse name (display name for this log configuration)
        String name = props.getProperty("monitor.logfile." + index + ".name", "Log File " + index);

        return new LogFileConfig(pathPattern, name, keywords, caseSensitive, regexMode, alertOnFirstMatch);
    }

    public List<LogFileConfig> getLogFileConfigs() {
        return Collections.unmodifiableList(logFileConfigs);
    }

    /**
     * Configuration for a single log file or pattern to monitor.
     */
    public static class LogFileConfig {
        private final String pathPattern; // Original path pattern (may contain wildcards)
        private final String name; // Display name for this log configuration
        private final boolean isPattern; // True if path contains wildcards
        private final List<String> keywords;
        private final boolean caseSensitive;
        private final boolean regexMode;
        private final boolean alertOnFirstMatch;
        private final List<Pattern> compiledPatterns;
        private List<Path> resolvedPaths; // Actual files matching the pattern

        public LogFileConfig(String pathPattern, String name, List<String> keywords, boolean caseSensitive,
                boolean regexMode, boolean alertOnFirstMatch) {
            this.pathPattern = pathPattern;
            this.name = name;
            this.isPattern = pathPattern.contains("*") || pathPattern.contains("?");
            this.keywords = keywords;
            this.caseSensitive = caseSensitive;
            this.regexMode = regexMode;
            this.alertOnFirstMatch = alertOnFirstMatch;

            // Pre-compile regex patterns if in regex mode
            this.compiledPatterns = new ArrayList<>();
            if (regexMode) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                for (String keyword : keywords) {
                    try {
                        compiledPatterns.add(Pattern.compile(keyword, flags));
                    } catch (Exception e) {
                        // If regex compilation fails, treat as literal string
                        compiledPatterns.add(Pattern.compile(Pattern.quote(keyword), flags));
                    }
                }
            }

            // Initialize resolved paths
            this.resolvedPaths = new ArrayList<>();
            refreshMatchingFiles();
        }

        /**
         * Refreshes the list of files matching the pattern.
         * Call this periodically to detect new log files.
         */
        public void refreshMatchingFiles() {
            if (isPattern) {
                resolvedPaths = expandWildcardPattern(pathPattern);
            } else {
                // Single static file
                resolvedPaths = Collections.singletonList(Paths.get(pathPattern));
            }
        }

        /**
         * Expands a wildcard pattern to a list of matching files.
         * Handles paths with spaces correctly.
         */
        private List<Path> expandWildcardPattern(String pattern) {
            try {
                // Normalize path separators to system default
                String normalizedPattern = pattern.replace('/', File.separatorChar).replace('\\', File.separatorChar);

                // Find the last separator before any wildcard character
                int lastSeparatorBeforeWildcard = -1;
                for (int i = 0; i < normalizedPattern.length(); i++) {
                    char c = normalizedPattern.charAt(i);
                    if (c == '*' || c == '?') {
                        break;
                    }
                    if (c == File.separatorChar) {
                        lastSeparatorBeforeWildcard = i;
                    }
                }

                String dirPath;
                String filePattern;

                if (lastSeparatorBeforeWildcard >= 0) {
                    dirPath = normalizedPattern.substring(0, lastSeparatorBeforeWildcard);
                    filePattern = normalizedPattern.substring(lastSeparatorBeforeWildcard + 1);
                } else {
                    // No directory separator found, use current directory
                    dirPath = ".";
                    filePattern = normalizedPattern;
                }

                // Now we can safely use Paths.get on the directory part (no wildcards)
                Path parentDir = Paths.get(dirPath);

                // Check if directory exists
                if (!Files.exists(parentDir) || !Files.isDirectory(parentDir)) {
                    return Collections.emptyList();
                }

                // Use DirectoryStream with glob pattern
                List<Path> matchedFiles = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir, filePattern)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            matchedFiles.add(entry);
                        }
                    }
                }

                // Sort by filename for consistent ordering
                matchedFiles.sort(Comparator.comparing(Path::toString));
                return matchedFiles;

            } catch (IOException e) {
                // Return empty list on error
                return Collections.emptyList();
            }
        }

        public String getPathPattern() {
            return pathPattern;
        }

        public String getName() {
            return name;
        }

        public boolean isPattern() {
            return isPattern;
        }

        public List<Path> getResolvedPaths() {
            return Collections.unmodifiableList(resolvedPaths);
        }

        /**
         * @deprecated Use getResolvedPaths() instead for pattern support
         */
        @Deprecated
        public Path getPath() {
            // For backward compatibility, return first resolved path or null if pattern has
            // no matches
            return resolvedPaths.isEmpty() ? null : resolvedPaths.get(0);
        }

        public List<String> getKeywords() {
            return Collections.unmodifiableList(keywords);
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public boolean isRegexMode() {
            return regexMode;
        }

        public boolean isAlertOnFirstMatch() {
            return alertOnFirstMatch;
        }

        public List<Pattern> getCompiledPatterns() {
            return Collections.unmodifiableList(compiledPatterns);
        }

        /**
         * Checks if a line contains any of the configured keywords.
         * 
         * @param line The line to check
         * @return Map of keyword to match count (0 or 1 per keyword)
         */
        public Map<String, Integer> findMatches(String line) {
            Map<String, Integer> matches = new HashMap<>();

            if (regexMode) {
                // Use compiled regex patterns
                for (int i = 0; i < keywords.size(); i++) {
                    String keyword = keywords.get(i);
                    Pattern pattern = compiledPatterns.get(i);
                    if (pattern.matcher(line).find()) {
                        matches.put(keyword, 1);
                    }
                }
            } else {
                // Simple substring matching
                String searchLine = caseSensitive ? line : line.toLowerCase();
                for (String keyword : keywords) {
                    String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();
                    if (searchLine.contains(searchKeyword)) {
                        matches.put(keyword, 1);
                    }
                }
            }

            return matches;
        }
    }
}
