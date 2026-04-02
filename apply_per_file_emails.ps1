# Script to update code for per-log-file email alerts with names

Write-Host "=== Updating for Per-Log-File Email Alerts ===" -ForegroundColor Cyan

# 1. Update LogKeywordMonitorConfig.java to add name field to LogFileConfig
Write-Host "`n1. Updating LogKeywordMonitorConfig.java..." -ForegroundColor Yellow

$configFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorConfig.java"
$configContent = Get-Content $configFile -Raw

# Remove global logFolderName if it exists
$configContent = $configContent -replace 'private final String logFolderName;[\r\n\s]+', ''
$configContent = $configContent -replace 'this\.logFolderName = props\.getProperty\("monitor\.log\.folder\.name", "Application Logs"\);[\r\n\s]+', ''
$configContent = $configContent -replace 'public String getLogFolderName\(\) \{[\r\n\s]+return logFolderName;[\r\n\s]+\}[\r\n\s]+', ''

# Add name field to LogFileConfig class (after pathPattern field)
$configContent = $configContent -replace '(private final String pathPattern; // Original path pattern \(may contain wildcards\))', @'
private final String pathPattern; // Original path pattern (may contain wildcards)
        private final String name; // Display name for this log configuration
'@

# Update LogFileConfig constructor signature
$configContent = $configContent -replace 'public LogFileConfig\(String pathPattern, List<String> keywords, boolean caseSensitive,[\r\n\s]+boolean regexMode, boolean alertOnFirstMatch\)', 'public LogFileConfig(String pathPattern, String name, List<String> keywords, boolean caseSensitive, boolean regexMode, boolean alertOnFirstMatch)'

# Update constructor body to set name
$configContent = $configContent -replace '(this\.pathPattern = pathPattern;)', @'
this.pathPattern = pathPattern;
            this.name = name;
'@

# Add getName() getter method (after getPathPattern)
$configContent = $configContent -replace '(public String getPathPattern\(\) \{[\r\n\s]+return pathPattern;[\r\n\s]+\})', @'
public String getPathPattern() {
            return pathPattern;
        }

        public String getName() {
            return name;
        }
'@

# Update parseLogFileConfig to read name and pass it to constructor
$configContent = $configContent -replace '(String pathPattern = pathStr;)', @'
String pathPattern = pathStr;

        // Parse name (display name for this log configuration)
        String name = props.getProperty("monitor.logfile." + index + ".name", "Log File " + index);
'@

$configContent = $configContent -replace 'return new LogFileConfig\(pathPattern, keywords, caseSensitive, regexMode, alertOnFirstMatch\);', 'return new LogFileConfig(pathPattern, name, keywords, caseSensitive, regexMode, alertOnFirstMatch);'

$configContent | Set-Content $configFile -NoNewline
Write-Host "   ✓ Added name field to LogFileConfig" -ForegroundColor Green

# 2. Update LogKeywordMonitorService.java to send separate emails per log file config
Write-Host "`n2. Updating LogKeywordMonitorService.java..." -ForegroundColor Yellow

$serviceFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorService.java"
$serviceContent = Get-Content $serviceFile -Raw

# Remove global logFolderName field if it exists
$serviceContent = $serviceContent -replace 'private final String logFolderName;[\r\n\s]+', ''
$serviceContent = $serviceContent -replace 'this\.logFolderName = config\.getLogFolderName\(\);[\r\n\s]+', ''

# Replace the entire sendAlertsForNewMatches method to send separate emails per config
$oldMethod = '(?s)private void sendAlertsForNewMatches\(\).*?^\s{4}\}'
$newMethod = @'
private void sendAlertsForNewMatches() {
        if (newMatchCounts.isEmpty()) {
            logger.fine("No new keyword matches to alert on.");
            return;
        }

        // Group matches by log file config (send separate email for each config)
        Map<LogKeywordMonitorConfig.LogFileConfig, Map<String, Map<String, Long>>> matchesByConfig = new HashMap<>();
        
        for (Map.Entry<String, ConcurrentHashMap<String, Long>> fileEntry : newMatchCounts.entrySet()) {
            String logFile = fileEntry.getKey();
            Map<String, Long> keywordCounts = fileEntry.getValue();

            if (keywordCounts.isEmpty()) {
                continue;
            }

            // Find which config this file belongs to
            LogKeywordMonitorConfig.LogFileConfig config = findConfigForPath(logFile);
            if (config != null) {
                matchesByConfig.putIfAbsent(config, new HashMap<>());
                matchesByConfig.get(config).put(logFile, keywordCounts);
            }
        }

        // Send separate email for each log file configuration
        for (Map.Entry<LogKeywordMonitorConfig.LogFileConfig, Map<String, Map<String, Long>>> configEntry : matchesByConfig.entrySet()) {
            LogKeywordMonitorConfig.LogFileConfig config = configEntry.getKey();
            Map<String, Map<String, Long>> filesForConfig = configEntry.getValue();
            
            sendAlertForConfig(config, filesForConfig);
        }
    }

    private void sendAlertForConfig(LogKeywordMonitorConfig.LogFileConfig config, Map<String, Map<String, Long>> filesWithMatches) {
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

        for (Map.Entry<String, Map<String, Long>> fileEntry : filesWithMatches.entrySet()) {
            String logFile = fileEntry.getKey();
            Map<String, Long> keywordCounts = fileEntry.getValue();

            boolean alertOnFirstMatch = config.isAlertOnFirstMatch();

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
            // Send email alert with config name
            try {
                emailService.sendAlert("Log Monitor", alertMessage.toString(), config.getName());
                logger.info("Email alert sent for: " + config.getName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send email alert: " + e.getMessage(), e);
            }
        }
    }
'@

$serviceContent = $serviceContent -replace $oldMethod, $newMethod, [System.Text.RegularExpressions.RegexOptions]::Multiline

$serviceContent | Set-Content $serviceFile -NoNewline
Write-Host "   ✓ Updated to send separate emails per log file config" -ForegroundColor Green

Write-Host "`n=== Update Complete ===" -ForegroundColor Green
Write-Host "`nChanges made:" -ForegroundColor Cyan
Write-Host "  ✓ Added monitor.logfile.N.name property" -ForegroundColor White
Write-Host "  ✓ Each log file config sends separate email" -ForegroundColor White
Write-Host "  ✓ Email shows log folder name from config" -ForegroundColor White
Write-Host "`nNext: Rebuild with 'mvn package -DskipTests'" -ForegroundColor Yellow
