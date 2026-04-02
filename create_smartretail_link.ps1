# Create a junction point (symlink) to SmartRetail logs folder
# This avoids issues with spaces in the path

$sourcePath = "C:\Program Files\Island Pacific\SmartRetail\SmartRetail_Trunk_NonSSO\server\logs"
$targetPath = "C:\SmartRetailLogs"

# Check if source exists
if (-not (Test-Path $sourcePath)) {
    Write-Host "ERROR: Source path does not exist: $sourcePath" -ForegroundColor Red
    exit 1
}

# Remove existing junction if it exists
if (Test-Path $targetPath) {
    Write-Host "Removing existing junction: $targetPath" -ForegroundColor Yellow
    cmd /c rmdir $targetPath
}

# Create junction point
Write-Host "Creating junction point..." -ForegroundColor Cyan
cmd /c mklink /J $targetPath $sourcePath

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nSUCCESS! Junction created:" -ForegroundColor Green
    Write-Host "  From: $targetPath" -ForegroundColor Yellow
    Write-Host "  To:   $sourcePath" -ForegroundColor Yellow
    Write-Host "`nYou can now use this path in logkeywordmonitor.properties:" -ForegroundColor Cyan
    Write-Host "  monitor.logfile.1.path=C:\\SmartRetailLogs\\catalina.*.log" -ForegroundColor White
}
else {
    Write-Host "ERROR: Failed to create junction point" -ForegroundColor Red
    exit 1
}
