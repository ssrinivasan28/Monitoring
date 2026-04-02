# PowerShell script to update LogKeywordMonitorService.java with HTML table formatting

$filePath = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorService.java"

# Read the file
$content = Get-Content $filePath -Raw

# Define the new sendAlertsForNewMatches method with HTML formatting
$newMethod = @'
    private void sendAlertsForNewMatches() {
        if (newMatchCounts.isEmpty()) {
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
            boolean alertOnFirstMatch = (config != null) ? config.isAlertOnFirstMatch() : true;

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
'@

# Find and replace the sendAlertsForNewMatches method
$pattern = '(?s)    private void sendAlertsForNewMatches\(\).*?\r\n    \}'
$content = $content -replace $pattern, $newMethod

# Write back to file
$content | Set-Content $filePath -NoNewline

Write-Host "Successfully updated LogKeywordMonitorService.java with HTML table formatting" -ForegroundColor Green
