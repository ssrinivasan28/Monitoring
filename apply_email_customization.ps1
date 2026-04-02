# Complete update script for email customization

Write-Host "=== Updating Log Keyword Monitor Email Customization ===" -ForegroundColor Cyan

# 1. Update LogKeywordMonitorConfig.java to add logFolderName field and getter
Write-Host "`n1. Updating LogKeywordMonitorConfig.java..." -ForegroundColor Yellow

$configFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorConfig.java"
$configContent = Get-Content $configFile -Raw

# Add logFolderName field after logFileConfigs
$configContent = $configContent -replace '(private final List<LogFileConfig> logFileConfigs;)', @'
private final List<LogFileConfig> logFileConfigs;
    private final String logFolderName;
'@

# Update constructor to read logFolderName
$configContent = $configContent -replace '(public LogKeywordMonitorConfig\(Properties props\) \{[\r\n\s]+this\.logFileConfigs = parseLogFileConfigs\(props\);)', @'
public LogKeywordMonitorConfig(Properties props) {
        this.logFileConfigs = parseLogFileConfigs(props);
        this.logFolderName = props.getProperty("monitor.log.folder.name", "Application Logs");
'@

# Add getter method before the LogFileConfig inner class
$configContent = $configContent -replace '(public List<LogFileConfig> getLogFileConfigs\(\) \{[\r\n\s]+return Collections\.unmodifiableList\(logFileConfigs\);[\r\n\s]+\})', @'
public List<LogFileConfig> getLogFileConfigs() {
        return Collections.unmodifiableList(logFileConfigs);
    }

    public String getLogFolderName() {
        return logFolderName;
    }
'@

$configContent | Set-Content $configFile -NoNewline
Write-Host "   ✓ Added logFolderName field and getter" -ForegroundColor Green

# 2. Update LogKeywordMonitorService.java to read and pass logFolderName
Write-Host "`n2. Updating LogKeywordMonitorService.java..." -ForegroundColor Yellow

$serviceFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorService.java"
$serviceContent = Get-Content $serviceFile -Raw

# Add logFolderName field
$serviceContent = $serviceContent -replace '(private final EmailService emailService;)', @'
private final EmailService emailService;
    private final String logFolderName;
'@

# Update constructor to get logFolderName from config
$serviceContent = $serviceContent -replace '(this\.emailService = emailService;)', @'
this.emailService = emailService;
        this.logFolderName = config.getLogFolderName();
'@

# Update sendAlert call
$serviceContent = $serviceContent -replace 'emailService\.sendAlert\("Log Keyword Alert", alertMessage\.toString\(\)\);', 'emailService.sendAlert("Log Monitor", alertMessage.toString(), logFolderName);'

$serviceContent | Set-Content $serviceFile -NoNewline
Write-Host "   ✓ Added logFolderName field and updated sendAlert call" -ForegroundColor Green

# 3. Update EmailService.java
Write-Host "`n3. Updating EmailService.java..." -ForegroundColor Yellow

$emailFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\EmailService.java"
$emailContent = Get-Content $emailFile -Raw

# Update sendAlert method signature
$emailContent = $emailContent -replace 'public void sendAlert\(String subject, String messageBody\)', 'public void sendAlert(String subject, String messageBody, String logFolderName)'

# Update method calls
$emailContent = $emailContent -replace 'sendAlertViaGraphAPI\(subject, messageBody\);', 'sendAlertViaGraphAPI(subject, messageBody, logFolderName);'
$emailContent = $emailContent -replace 'sendAlertViaSMTP\(subject, messageBody\);', 'sendAlertViaSMTP(subject, messageBody, logFolderName);'

# Update private method signatures
$emailContent = $emailContent -replace 'private void sendAlertViaGraphAPI\(String subject, String messageBody\)', 'private void sendAlertViaGraphAPI(String subject, String messageBody, String logFolderName)'
$emailContent = $emailContent -replace 'private void sendAlertViaSMTP\(String subject, String messageBody\)', 'private void sendAlertViaSMTP(String subject, String messageBody, String logFolderName)'

# Update buildAlertHtmlContent calls
$emailContent = $emailContent -replace 'buildAlertHtmlContent\(subject, messageBody, true\)', 'buildAlertHtmlContent(subject, messageBody, logFolderName, true)'
$emailContent = $emailContent -replace 'buildAlertHtmlContent\(subject, messageBody, false\)', 'buildAlertHtmlContent(subject, messageBody, logFolderName, false)'

# Update buildAlertHtmlContent signature
$emailContent = $emailContent -replace 'private String buildAlertHtmlContent\(String subject, String messageBody, boolean useDataUri\)', 'private String buildAlertHtmlContent(String subject, String messageBody, String logFolderName, boolean useDataUri)'

# Add greeting and log folder name in HTML content (after header)
$emailContent = $emailContent -replace '(\.append\("<td class=\\"content-area\\">"\))', @'
.append("<td class=\"content-area\">")
                .append("<p style='margin-bottom: 15px;'>Hi Team,</p>")
                .append("<p style='margin-bottom: 15px;'>This is an automated alert from <strong>Island Pacific Operations Monitor</strong>.</p>")
                .append("<h4 style='color: #2c3e50; margin-bottom: 10px; border-bottom: 2px solid #3498db; padding-bottom: 5px;'>Log Folder: ").append(messageBody.contains("<") ? logFolderName : escapeHtml(logFolderName)).append("</h4>")
'@

$emailContent | Set-Content $emailFile -NoNewline
Write-Host "   ✓ Updated method signatures and added greeting" -ForegroundColor Green

# 4. Update LogKeywordMonitorService.java HTML message building
Write-Host "`n4. Updating alert message HTML..." -ForegroundColor Yellow

$serviceContent = Get-Content $serviceFile -Raw

# Remove the old h4 for "Keyword Detection Summary" since we'll have it in EmailService
$serviceContent = $serviceContent -replace 'alertMessage\.append\("<h4 style=''color: #2c3e50; margin-bottom: 15px;''>Keyword Detection Summary</h4>"\);[\r\n\s]+', ''

$serviceContent | Set-Content $serviceFile -NoNewline
Write-Host "   ✓ Cleaned up duplicate headers" -ForegroundColor Green

Write-Host "`n=== Update Complete ===" -ForegroundColor Green
Write-Host "`nChanges made:" -ForegroundColor Cyan
Write-Host "  ✓ Added monitor.log.folder.name property" -ForegroundColor White
Write-Host "  ✓ Email subject changed to 'Log Monitor'" -ForegroundColor White
Write-Host "  ✓ Added 'Hi Team,' greeting" -ForegroundColor White
Write-Host "  ✓ Added automated alert message" -ForegroundColor White
Write-Host "  ✓ Log folder name displayed in email" -ForegroundColor White
Write-Host "`nNext: Rebuild with 'mvn package -DskipTests'" -ForegroundColor Yellow
