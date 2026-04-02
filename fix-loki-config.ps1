# Fix loki.yaml - remove deprecated max_look_back_period
$lokiYaml = "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\loki\loki.yaml"

$content = Get-Content $lokiYaml -Raw

# Remove the chunk_store_config section with max_look_back_period
$content = $content -replace "chunk_store_config:\s*\r?\n\s*max_look_back_period:\s*\S+\s*\r?\n", ""

$content | Set-Content $lokiYaml -Encoding UTF8
Write-Host "Fixed loki.yaml - removed deprecated max_look_back_period"

# Restart Loki
Write-Host "Restarting Loki..."
& "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoring-Loki.exe" restart
Start-Sleep -Seconds 2

# Check status
$svc = Get-Service -Name "*Loki*IOHANA*"
Write-Host "Loki service status: $($svc.Status)"
