# PowerShell script to update email template with custom greeting and log folder name

$serviceFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\LogKeywordMonitorService.java"
$emailFile = "src\main\java\com\islandpacific\monitoring\logkeywordmonitoring\EmailService.java"

# Read the service file
$serviceContent = Get-Content $serviceFile -Raw

# Update the sendAlert call to pass log folder name
$oldSendAlert = 'emailService.sendAlert\("Log Keyword Alert", alertMessage.toString\(\)\);'
$newSendAlert = 'emailService.sendAlert("Log Monitor", alertMessage.toString(), logFolderName);'

$serviceContent = $serviceContent -replace $oldSendAlert, $newSendAlert

# Write back
$serviceContent | Set-Content $serviceFile -NoNewline

Write-Host "Updated LogKeywordMonitorService.java" -ForegroundColor Green

# Read the email service file  
$emailContent = Get-Content $emailFile -Raw

# Update sendAlert method signature to accept logFolderName
$oldSignature = 'public void sendAlert\(String subject, String messageBody\)'
$newSignature = 'public void sendAlert(String subject, String messageBody, String logFolderName)'

$emailContent = $emailContent -replace $oldSignature, $newSignature

# Update the method calls
$emailContent = $emailContent -replace 'sendAlertViaGraphAPI\(subject, messageBody\)', 'sendAlertViaGraphAPI(subject, messageBody, logFolderName)'
$emailContent = $emailContent -replace 'sendAlertViaSMTP\(subject, messageBody\)', 'sendAlertViaSMTP(subject, messageBody, logFolderName)'

# Update private method signatures
$emailContent = $emailContent -replace 'private void sendAlertViaGraphAPI\(String subject, String messageBody\)', 'private void sendAlertViaGraphAPI(String subject, String messageBody, String logFolderName)'
$emailContent = $emailContent -replace 'private void sendAlertViaSMTP\(String subject, String messageBody\)', 'private void sendAlertViaSMTP(String subject, String messageBody, String logFolderName)'

# Update buildAlertHtmlContent calls
$emailContent = $emailContent -replace 'buildAlertHtmlContent\(subject, messageBody, true\)', 'buildAlertHtmlContent(subject, messageBody, logFolderName, true)'
$emailContent = $emailContent -replace 'buildAlertHtmlContent\(subject, messageBody, false\)', 'buildAlertHtmlContent(subject, messageBody, logFolderName, false)'

# Update buildAlertHtmlContent signature
$emailContent = $emailContent -replace 'private String buildAlertHtmlContent\(String subject, String messageBody, boolean useDataUri\)', 'private String buildAlertHtmlContent(String subject, String messageBody, String logFolderName, boolean useDataUri)'

# Add greeting after the subtitle
$oldGreeting = "alertMessage.append\(`"<h4 style='color: #2c3e50; margin-bottom: 15px;'>Keyword Detection Summary</h4>`"\);"
$newGreeting = @'
alertMessage.append("<p style='margin-bottom: 15px;'>Hi Team,</p>");
        alertMessage.append("<p style='margin-bottom: 15px;'>This is an automated alert from <strong>Island Pacific Operations Monitor</strong>.</p>");
        alertMessage.append("<h4 style='color: #2c3e50; margin-bottom: 10px;'>Log Folder: ").append(escapeHtml(logFolderName)).append("</h4>");
        alertMessage.append("<h4 style='color: #2c3e50; margin-bottom: 15px;'>Keyword Detection Summary</h4>");
'@

$emailContent = $emailContent -replace [regex]::Escape($oldGreeting), $newGreeting

# Write back
$emailContent | Set-Content $emailFile -NoNewline

Write-Host "Updated EmailService.java" -ForegroundColor Green
Write-Host "`nEmail template now includes:" -ForegroundColor Cyan
Write-Host "  - Subject: 'Log Monitor'" -ForegroundColor Yellow
Write-Host "  - Greeting: 'Hi Team,'" -ForegroundColor Yellow
Write-Host "  - Automated alert message" -ForegroundColor Yellow
Write-Host "  - Log folder name from properties" -ForegroundColor Yellow
