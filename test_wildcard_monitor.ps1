# Test script for Log Keyword Monitor with wildcard patterns

Write-Host "Creating test log files with date stamps..." -ForegroundColor Cyan

# Create test directory
New-Item -Path "C:\Logs" -ItemType Directory -Force | Out-Null

# Create date-stamped log files
@"
2026-01-30 10:00:00 INFO Application started
2026-01-30 10:05:00 INFO Processing batch job
2026-01-30 10:10:00 ERROR Database connection timeout
2026-01-30 10:15:00 INFO Batch completed
"@ | Out-File -FilePath "C:\Logs\application_2026-01-30.log" -Encoding UTF8

@"
2026-01-31 09:00:00 INFO Application started
2026-01-31 09:05:00 INFO User login: admin
2026-01-31 09:10:00 FATAL System crash - OutOfMemoryError
2026-01-31 09:15:00 ERROR Failed to recover
2026-01-31 09:20:00 INFO System restarted
"@ | Out-File -FilePath "C:\Logs\application_2026-01-31.log" -Encoding UTF8

Write-Host "`nCreated test log files:" -ForegroundColor Green
Get-ChildItem "C:\Logs\application_*.log" | ForEach-Object {
    Write-Host "  - $($_.Name) ($($_.Length) bytes)" -ForegroundColor Yellow
}

Write-Host "`nStarting Log Keyword Monitor..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the monitor`n" -ForegroundColor Gray

# Run the monitor
java -jar "target\monitors\LogKeywordMonitor.jar" "logkeywordmonitor.properties"
