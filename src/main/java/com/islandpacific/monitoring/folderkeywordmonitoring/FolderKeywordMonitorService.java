package com.islandpacific.monitoring.folderkeywordmonitoring;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FolderKeywordMonitorService {

    private final Logger logger;
    private final FolderKeywordMonitorConfig config;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, Long> totalFilesMatched;
    private final ConcurrentHashMap<String, Long> totalFilesScanned;

    // file path -> last modified time already alerted; re-alerts if the file is overwritten with a newer mtime
    private final Map<String, FileTime> alertedFiles;

    public FolderKeywordMonitorService(Logger logger, FolderKeywordMonitorConfig config,
            EmailService emailService,
            ConcurrentHashMap<String, Long> totalFilesMatched,
            ConcurrentHashMap<String, Long> totalFilesScanned) {
        this.logger = logger;
        this.config = config;
        this.emailService = emailService;
        this.totalFilesMatched = totalFilesMatched;
        this.totalFilesScanned = totalFilesScanned;
        this.alertedFiles = new HashMap<>();
    }

    public void checkAndAlert() {
        Map<Path, List<String>> matched = new LinkedHashMap<>();

        for (String folderPath : config.getFolderPaths()) {
            Path root = Paths.get(folderPath);
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                logger.warning("Monitor folder does not exist or is not a directory: " + root);
                continue;
            }
            scanFolder(root, folderPath, matched);
        }

        if (!matched.isEmpty()) {
            sendAlert(matched);
        } else {
            logger.fine("No new keyword matches found.");
        }
    }

    private void scanFolder(Path root, String folderKey, Map<Path, List<String>> matched) {
        try (Stream<Path> paths = config.isRecursive() ? Files.walk(root) : Files.list(root)) {
            paths.filter(Files::isRegularFile)
                .forEach(file -> {
                    totalFilesScanned.compute(folderKey, (k, v) -> (v == null ? 0L : v) + 1L);
                    try {
                        List<String> foundKeywords = findMatchedKeywords(file);
                        if (!foundKeywords.isEmpty()) {
                            String pathStr = file.toAbsolutePath().toString();
                            FileTime mtime = Files.getLastModifiedTime(file);
                            FileTime alertedMtime = alertedFiles.get(pathStr);
                            if (alertedMtime == null || mtime.compareTo(alertedMtime) > 0) {
                                matched.put(file, foundKeywords);
                                alertedFiles.put(pathStr, mtime);
                                totalFilesMatched.compute(folderKey, (k, v) -> (v == null ? 0L : v) + 1L);
                                logger.info("Keyword match found: " + pathStr + " [" + String.join(", ", foundKeywords) + "]");
                            }
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Error reading file: " + file, e);
                    }
                });
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error scanning folder: " + root, e);
        }
    }

    private List<String> findMatchedKeywords(Path file) throws IOException {
        Set<String> found = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String searchLine = config.isCaseSensitive() ? line : line.toLowerCase();
                for (String keyword : config.getKeywords()) {
                    String searchKeyword = config.isCaseSensitive() ? keyword : keyword.toLowerCase();
                    if (searchLine.contains(searchKeyword)) {
                        found.add(keyword);
                    }
                }
            }
        }
        return new ArrayList<>(found);
    }

    private void sendAlert(Map<Path, List<String>> matchedFiles) {
        StringBuilder body = new StringBuilder();
        body.append("<h4 style='color:#2c3e50;margin-bottom:15px;'>Keyword Detection Summary</h4>");
        body.append("<p>The following files contained one or more monitored keywords:</p>");
        body.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;'>");
        body.append("<thead><tr style='background-color:#34495e;color:white;'>");
        body.append("<th style='padding:10px;text-align:left;border:1px solid #ddd;'>File Name</th>");
        body.append("<th style='padding:10px;text-align:left;border:1px solid #ddd;'>Folder</th>");
        body.append("<th style='padding:10px;text-align:left;border:1px solid #ddd;'>Keywords Found</th>");
        body.append("</tr></thead><tbody>");

        int row = 0;
        for (Map.Entry<Path, List<String>> entry : matchedFiles.entrySet()) {
            Path file = entry.getKey();
            String rowColor = (row++ % 2 == 0) ? "#f9f9f9" : "#ffffff";
            String fileName = file.getFileName().toString();
            String folder = file.getParent() != null ? file.getParent().toAbsolutePath().toString() : "";
            body.append("<tr style='background-color:").append(rowColor).append(";'>");
            body.append("<td style='padding:10px;border:1px solid #ddd;font-family:monospace;font-size:12px;'>")
               .append(escapeHtml(fileName)).append("</td>");
            body.append("<td style='padding:10px;border:1px solid #ddd;font-family:monospace;font-size:12px;'>")
               .append(escapeHtml(folder)).append("</td>");
            body.append("<td style='padding:10px;border:1px solid #ddd;font-family:monospace;font-size:12px;'>")
               .append(escapeHtml(String.join(", ", entry.getValue()))).append("</td>");
            body.append("</tr>");
        }

        body.append("</tbody></table>");
        body.append("<p style='color:#7f8c8d;font-size:13px;'>Keywords monitored: ")
           .append(escapeHtml(String.join(", ", config.getKeywords()))).append("</p>");

        try {
            emailService.sendAlert("Folder Keyword Alert", body.toString());
            logger.info("Email alert sent for " + matchedFiles.size() + " file(s).");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send email alert: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
