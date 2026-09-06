#Requires -Version 5.1
<#
.SYNOPSIS
    Builds the Windows Monitoring Agent installer executable.

.DESCRIPTION
    1. Builds all Java JARs via Maven
    2. Copies Windows JARs to installer resources
    3. Copies root-level .properties files to installer resources
    4. Compiles WindowsMonitoringSetup.iss -> WindowsMonitoringAgentSetup.exe

.PARAMETER SkipMaven
    Skip Maven build (use existing JARs)

.PARAMETER Clean
    Remove target/ and installer output before building

.EXAMPLE
    .\build-windows-installer.ps1
    Full build

.EXAMPLE
    .\build-windows-installer.ps1 -SkipMaven
    Repackage installer without rebuilding JARs
#>

[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$ProjectRoot  = $PSScriptRoot
$ResourcesDir = Join-Path $ProjectRoot "installer\resources"
$OutputDir    = Join-Path $ProjectRoot "installer\output"
$MonitorsDir  = Join-Path $ProjectRoot "target\monitors"

$InnoSetupPaths = @(
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe",
    "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
)

function Write-Step    { param([string]$m) Write-Host "`n==> $m" -ForegroundColor Cyan }
function Write-Ok      { param([string]$m) Write-Host "  [OK] $m" -ForegroundColor Green }
function Write-Warn    { param([string]$m) Write-Host "  [!]  $m" -ForegroundColor Yellow }
function Write-Info    { param([string]$m) Write-Host "       $m" -ForegroundColor Gray }

# JAR name prefix -> installer resources subfolder
$JarMap = @{
    "WinMonitor"              = "WinMonitor"
    "WinFSErrorMonitor"       = "WinFSErrorMonitor"
    "WinFSCardinalityMonitor" = "WinFSCardinalityMonitor"
    "LogKeywordMonitor"       = "LogKeywordMonitor"
    "ServerUpTimeMonitor"     = "ServerUpTimeMonitor"
    "WinServiceMonitor"       = "WinServiceMonitor"
    "FolderLogKeywordMonitor" = "FolderLogKeywordMonitor"
    "ShareFileMonitor"        = "ShareFileMonitor"
    "SSLCertMonitor"          = "SSLCertMonitor"
    "APIMonitor"              = "APIMonitor"
    "CredTool"                = "CredTool"
}

# Root .properties filename -> installer resources subfolder
# null = skip (no matching subfolder needed)
$PropsMap = @{
    "email.properties"               = $null   # copied to every Windows monitor folder
    "windowsmonitor.properties"      = "WinMonitor"
    "fserrormonitor.properties"      = "WinFSErrorMonitor"
    "fscardinalitymonitor.properties"= "WinFSCardinalityMonitor"
    "logkeywordmonitor.properties"   = "LogKeywordMonitor"
    "serverinfo.properties"          = "ServerUpTimeMonitor"
    "winservicemonitor.properties"   = "WinServiceMonitor"
    "folderlogkeywordmonitor.properties" = "FolderLogKeywordMonitor"
    "sharefilemonitor.properties"    = "ShareFileMonitor"
    "sslcertmonitor.properties"      = "SSLCertMonitor"
    "apiurlmonitor.properties"       = "APIMonitor"
}

# Subfolders that get a copy of email.properties
$EmailFolders = @(
    "WinMonitor", "WinFSErrorMonitor", "WinFSCardinalityMonitor",
    "LogKeywordMonitor", "ServerUpTimeMonitor", "WinServiceMonitor",
    "FolderLogKeywordMonitor", "ShareFileMonitor", "SSLCertMonitor", "APIMonitor"
)

# ─────────────────────────────────────────────
if ($Clean) {
    Write-Step "Cleaning build directories..."
    $tgt = Join-Path $ProjectRoot "target"
    if (Test-Path $tgt)       { Remove-Item $tgt -Recurse -Force; Write-Ok "Removed target/" }
    if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force; Write-Ok "Removed installer/output/" }
}

# ─────────────────────────────────────────────
Write-Step "Locating Inno Setup..."
$iscc = $null
foreach ($p in $InnoSetupPaths) { if (Test-Path $p) { $iscc = $p; break } }
if (-not $iscc) { throw "Inno Setup 6 not found. Install from https://jrsoftware.org/isdl.php" }
Write-Ok $iscc

# ─────────────────────────────────────────────
if (-not $SkipMaven) {
    Write-Step "Building Java services (mvn clean package -DskipTests)..."
    & mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
    Write-Ok "Maven build complete"
} else {
    Write-Warn "Skipping Maven build"
}

# ─────────────────────────────────────────────
Write-Step "Copying Windows JARs to installer resources..."
if (-not (Test-Path $MonitorsDir)) { throw "No JARs found in target\monitors. Run without -SkipMaven first." }

$copied = 0
foreach ($jar in (Get-ChildItem $MonitorsDir -Filter "*.jar")) {
    $match = $JarMap.Keys | Where-Object { $jar.Name -like "$_*" } | Select-Object -First 1
    if (-not $match) { Write-Warn "Skipped (not Windows): $($jar.Name)"; continue }

    $destDir = Join-Path $ResourcesDir "monitoring-services\$($JarMap[$match])"
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $jar.FullName -Destination $destDir -Force
    Write-Ok "$($jar.Name)  ->  monitoring-services\$($JarMap[$match])\"
    $copied++
}
Write-Info "$copied JAR(s) copied"

# ─────────────────────────────────────────────
Write-Step "Copying properties files to installer resources..."

foreach ($entry in $PropsMap.GetEnumerator()) {
    $fileName  = $entry.Key
    $subFolder = $entry.Value
    $srcPath   = Join-Path $ProjectRoot $fileName

    if (-not (Test-Path $srcPath)) { Write-Warn "Missing at root: $fileName"; continue }

    if ($null -eq $subFolder) {
        # email.properties goes to every Windows monitor folder
        foreach ($folder in $EmailFolders) {
            $destDir = Join-Path $ResourcesDir "monitoring-services\$folder"
            if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
            Copy-Item $srcPath -Destination $destDir -Force
        }
        Write-Ok "email.properties  ->  all Windows monitor folders"
    } else {
        $destDir = Join-Path $ResourcesDir "monitoring-services\$subFolder"
        if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
        Copy-Item $srcPath -Destination $destDir -Force
        Write-Ok "$fileName  ->  monitoring-services\$subFolder\"
    }
}

# ─────────────────────────────────────────────
Write-Step "Compiling WindowsMonitoringSetup.iss..."
$issFile = Join-Path $ProjectRoot "WindowsMonitoringSetup.iss"
if (-not (Test-Path $issFile)) { throw "WindowsMonitoringSetup.iss not found at $issFile" }

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

$output = & $iscc $issFile 2>&1
if ($LASTEXITCODE -ne 0) {
    $output | ForEach-Object { Write-Info $_ }
    throw "Inno Setup compilation failed"
}
$output | ForEach-Object { Write-Info $_ }

# ─────────────────────────────────────────────
$exePath = Join-Path $OutputDir "WindowsMonitoringAgentSetup.exe"
if (-not (Test-Path $exePath)) { throw "Expected output not found: $exePath" }

$sizeMB = ((Get-Item $exePath).Length / 1MB).ToString("F1")
Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Ok  "Output : $exePath"
Write-Ok  "Size   : $sizeMB MB"
Write-Host ""
