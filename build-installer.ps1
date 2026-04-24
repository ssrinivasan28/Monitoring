#Requires -Version 5.1
<#
.SYNOPSIS
    Builds the Island Pacific Monitoring Suite installer executable.

.DESCRIPTION
    This script automates the complete build process:
    1. Builds Java monitoring services using Maven
    2. Copies JAR files to installer resources
    3. Validates required binaries (Prometheus, Grafana, Loki, etc.)
    4. Compiles the Inno Setup installer script
    5. Generates IslandPacificMonitoringSetup.exe

.PARAMETER SkipMaven
    Skip the Maven build step (use existing JARs)

.PARAMETER SkipValidation
    Skip validation of external binaries

.PARAMETER Clean
    Clean build (removes target and output directories first)

.EXAMPLE
    .\build-installer.ps1
    Full build with all steps

.EXAMPLE
    .\build-installer.ps1 -SkipMaven
    Build installer without rebuilding Java services

.EXAMPLE
    .\build-installer.ps1 -Clean
    Clean build from scratch
#>

[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$SkipValidation,
    [switch]$Clean
)

# Script configuration
$ErrorActionPreference = "Stop"
$ScriptRoot = $PSScriptRoot
$ProjectRoot = $ScriptRoot
$InstallerDir = Join-Path $ProjectRoot "installer"
$ResourcesDir = Join-Path $InstallerDir "resources"
$OutputDir = Join-Path $InstallerDir "output"
$TargetDir = Join-Path $ProjectRoot "target"
$MonitorsDir = Join-Path $TargetDir "monitors"

# Inno Setup paths (common installation locations)
$InnoSetupPaths = @(
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe",
    "C:\Program Files (x86)\Inno Setup 5\ISCC.exe",
    "$env:ProgramFiles(x86)\Inno Setup 6\ISCC.exe"
)

# Color output functions
function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[!] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[X] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Gray
}

# Find Inno Setup Compiler
function Find-InnoSetup {
    Write-Step "Locating Inno Setup Compiler..."
    
    foreach ($path in $InnoSetupPaths) {
        if (Test-Path $path) {
            Write-Success "Found: $path"
            return $path
        }
    }
    
    Write-Error "Inno Setup Compiler not found!"
    Write-Info "Please install Inno Setup from: https://jrsoftware.org/isdl.php"
    Write-Info "Searched locations:"
    $InnoSetupPaths | ForEach-Object { Write-Info "  - $_" }
    throw "Inno Setup Compiler not found"
}

# Validate Maven installation
function Test-Maven {
    Write-Step "Checking Maven installation..."
    
    try {
        $mvnVersion = & mvn --version 2>&1 | Select-Object -First 1
        Write-Success "Maven found: $mvnVersion"
        return $true
    }
    catch {
        Write-Error "Maven not found in PATH!"
        Write-Info "Please install Maven from: https://maven.apache.org/download.cgi"
        throw "Maven not found"
    }
}

# Clean build directories
function Invoke-Clean {
    Write-Step "Cleaning build directories..."
    
    if (Test-Path $TargetDir) {
        Remove-Item $TargetDir -Recurse -Force
        Write-Success "Removed: $TargetDir"
    }
    
    if (Test-Path $OutputDir) {
        Remove-Item $OutputDir -Recurse -Force
        Write-Success "Removed: $OutputDir"
    }
}

# Build Java monitoring services
function Invoke-MavenBuild {
    Write-Step "Building Java monitoring services..."
    Write-Info "This may take a few minutes..."
    
    $startTime = Get-Date
    
    try {
        # Run Maven package
        & mvn clean package -DskipTests
        
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed with exit code: $LASTEXITCODE"
        }
        
        $duration = (Get-Date) - $startTime
        Write-Success "Maven build completed in $($duration.TotalSeconds.ToString('F1')) seconds"
        
        # Verify JAR files were created
        if (-not (Test-Path $MonitorsDir)) {
            throw "Monitors directory not found: $MonitorsDir"
        }
        
        $jarFiles = Get-ChildItem $MonitorsDir -Filter "*.jar"
        Write-Success "Generated $($jarFiles.Count) JAR files:"
        $jarFiles | ForEach-Object { 
            $sizeMB = ($_.Length / 1MB).ToString("F2")
            Write-Info ("  - {0} ({1} MB)" -f $_.Name, $sizeMB)
        }
    }
    catch {
        Write-Error "Maven build failed: $_"
        throw
    }
}

# Copy JARs to installer resources
function Copy-MonitoringJars {
    Write-Step "Copying JAR files to installer resources..."
    
    $jarFiles = Get-ChildItem $MonitorsDir -Filter "*.jar"
    
    if ($jarFiles.Count -eq 0) {
        throw "No JAR files found in $MonitorsDir"
    }
    
    $copiedCount = 0
    
    foreach ($jar in $jarFiles) {
        # Determine target directory based on JAR name
        $targetSubDir = switch -Regex ($jar.Name) {
            "^IBMIFSErrorMonitor" { "IBMIFSErrorMonitor" }
            "^IBMRealTimeIFSMonitor" { "IBMRealTimeIFSMonitor" }
            "^IBMJobQueCountMonitor" { "IBMJobQueCountMonitor" }
            "^IBMJobQueStatusMonitor" { "IBMJobQueStatusMonitor" }
            "^IBMSubSystemMonitor" { "IBMSubSystemMonitoring" }
            "^IBMMatrixMonitor" { "IBMSystemMatrix" }
            "^IBMQSYSOPRMonitor" { "QSYSOPRMonitoring" }
            "^IBMUserProfileChecker" { "IBMUserProfileChecker" }
            "^IBMNetworkEnabler" { "NetWorkEnabler" }
            "^ServerUpTimeMonitor" { "ServerUpTimeMonitor" }
            "^WinFSErrorMonitor" { "WinFSErrorMonitor" }
            "^WinFSCardinalityMonitor" { "WinFSCardinalityMonitor" }
            "^WinMonitor" { "WinMonitor" }
            "^LogKeywordMonitor" { "LogKeywordMonitor" }
            "^IBMFileMemberMonitor" { "IBMFileMemberMonitor" }
            "^IBMJobDurationMonitor" { "IBMJobDurationMonitor" }
            "^WinServiceMonitor" { "WinServiceMonitor" }
            default { $null }
        }
        
        if ($targetSubDir) {
            $targetDir = Join-Path $ResourcesDir "monitoring-services\$targetSubDir"
            
            # Create directory if it doesn't exist
            if (-not (Test-Path $targetDir)) {
                New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
            }
            
            # Copy JAR file
            Copy-Item $jar.FullName -Destination $targetDir -Force
            Write-Info ("  [OK] {0} -> {1}\\" -f $jar.Name, $targetSubDir)
            
            # Special case for WinMonitor: also copy its properties file
            if ($targetSubDir -eq "WinMonitor") {
                $winProps = Join-Path $PSScriptRoot "windowsmonitor.properties"
                if (Test-Path $winProps) {
                    Copy-Item $winProps -Destination $targetDir -Force
                    Write-Info "  [OK] windowsmonitor.properties -> WinMonitor\\"
                }
            }
            
            $copiedCount++
        }
        else {
            Write-Warning "Unknown JAR file (skipped): $($jar.Name)"
        }
    }
    
    Write-Success "Copied $copiedCount JAR files to installer resources"
}

# Validate required binaries
function Test-RequiredBinaries {
    Write-Step "Validating required binaries..."
    
    $requiredBinaries = @(
        @{ Path = "WinSW.exe"; Name = "Windows Service Wrapper" },
        @{ Path = "prometheus\prometheus.exe"; Name = "Prometheus" },
        @{ Path = "grafana\bin\grafana-server.exe"; Name = "Grafana" },
        @{ Path = "loki\loki.exe"; Name = "Loki" },
        @{ Path = "promtail\promtail.exe"; Name = "Promtail" }
    )
    
    $allFound = $true
    
    foreach ($binary in $requiredBinaries) {
        $fullPath = Join-Path $ResourcesDir $binary.Path
        
        if (Test-Path $fullPath) {
            $sizeMB = ((Get-Item $fullPath).Length / 1MB).ToString("F2")
            Write-Info ("  [OK] {0}: {1} ({2} MB)" -f $binary.Name, $binary.Path, $sizeMB)
        }
        else {
            Write-Warning "Missing: $($binary.Name) at $($binary.Path)"
            $allFound = $false
        }
    }
    
    if (-not $allFound) {
        Write-Warning "Some binaries are missing. Download them manually:"
        Write-Info "  - Prometheus: https://prometheus.io/download/"
        Write-Info "  - Grafana: https://grafana.com/grafana/download"
        Write-Info "  - Loki: https://github.com/grafana/loki/releases"
        Write-Info "  - Promtail: https://github.com/grafana/loki/releases"
        Write-Info "  - WinSW: https://github.com/winsw/winsw/releases"
        
        if (-not $SkipValidation) {
            throw "Required binaries missing. Use -SkipValidation to continue anyway."
        }
    }
    else {
        Write-Success "All required binaries found"
    }
}

# Compile Inno Setup installer
function Invoke-InnoSetupCompile {
    param([string]$InnoSetupPath)
    
    Write-Step "Compiling Inno Setup installer..."
    
    $issFile = Join-Path $ProjectRoot "installer.iss"
    
    if (-not (Test-Path $issFile)) {
        throw "Installer script not found: $issFile"
    }
    
    Write-Info "Script: $issFile"
    Write-Info "Compiler: $InnoSetupPath"
    
    $startTime = Get-Date
    
    try {
        # Run Inno Setup Compiler
        $output = & $InnoSetupPath $issFile 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Inno Setup compilation failed!"
            $output | ForEach-Object { Write-Info $_ }
            throw "Inno Setup compilation failed with exit code: $LASTEXITCODE"
        }
        
        $duration = (Get-Date) - $startTime
        Write-Success "Compilation completed in $($duration.TotalSeconds.ToString('F1')) seconds"
        
        # Display compilation output
        $output | ForEach-Object { Write-Info $_ }
    }
    catch {
        Write-Error "Compilation failed: $_"
        throw
    }
}

# Verify output executable
function Test-OutputExecutable {
    Write-Step "Verifying output executable..."
    
    $exePath = Join-Path $OutputDir "IslandPacificMonitoringSetup.exe"
    
    if (Test-Path $exePath) {
        $fileInfo = Get-Item $exePath
        $sizeMB = ($fileInfo.Length / 1MB).ToString("F2")
        
        Write-Success "Installer created successfully!"
        Write-Info "  Location: $exePath"
        Write-Info "  Size: $sizeMB MB"
        Write-Info "  Modified: $($fileInfo.LastWriteTime)"
        
        return $exePath
    }
    else {
        throw "Output executable not found: $exePath"
    }
}

# Main build process
function Start-Build {
    $buildStartTime = Get-Date
    
    Write-Host "`n================================================================" -ForegroundColor Cyan
    Write-Host "  Island Pacific Monitoring Suite - Installer Build Script  " -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    
    try {
        # Step 1: Clean (if requested)
        if ($Clean) {
            Invoke-Clean
        }
        
        # Step 2: Find Inno Setup
        $innoSetupPath = Find-InnoSetup
        
        # Step 3: Build Java services (unless skipped)
        if (-not $SkipMaven) {
            Test-Maven
            Invoke-MavenBuild
            Copy-MonitoringJars
        }
        else {
            Write-Warning "Skipping Maven build (using existing JARs)"
        }
        
        # Step 4: Validate binaries (unless skipped)
        if (-not $SkipValidation) {
            Test-RequiredBinaries
        }
        else {
            Write-Warning "Skipping binary validation"
        }
        
        # Step 5: Compile installers
        Invoke-InnoSetupCompile -InnoSetupPath $innoSetupPath

        # Compile standalone ServiceScheduler installer if it exists
        $ssIss = Join-Path $ProjectRoot "ServiceScheduler.iss"
        if (Test-Path $ssIss) {
            Write-Step "Compiling ServiceScheduler standalone installer..."
            # Ensure the latest JAR is in installer resources
            $ssJarSrc = Join-Path $MonitorsDir "ServiceScheduler.jar"
            $ssJarDst = Join-Path $ResourcesDir "monitoring-services\ServiceScheduler\ServiceScheduler.jar"
            if (Test-Path $ssJarSrc) {
                Copy-Item $ssJarSrc -Destination $ssJarDst -Force
                Write-Info "  Copied ServiceScheduler.jar to installer resources"
            }
            $ssOutput = & $innoSetupPath $ssIss 2>&1
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "ServiceScheduler.iss compilation failed (non-fatal)"
                $ssOutput | ForEach-Object { Write-Info $_ }
            } else {
                Write-Success "ServiceSchedulerSetup.exe compiled"
            }
        }

        # Step 6: Verify output
        $exePath = Test-OutputExecutable
        
        # Build summary
        $totalDuration = (Get-Date) - $buildStartTime
        
        Write-Host "`n================================================================" -ForegroundColor Green
        Write-Host "                    BUILD SUCCESSFUL!                        " -ForegroundColor Green
        Write-Host "================================================================" -ForegroundColor Green
        Write-Host ""
        Write-Success "Total build time: $($totalDuration.TotalSeconds.ToString('F1')) seconds"
        Write-Success "Installer: $exePath"
        Write-Host ""
        Write-Info "Next steps:"
        Write-Info "  1. Test the installer on a clean VM"
        Write-Info "  2. Verify all services install correctly"
        Write-Info "  3. Check Grafana dashboards load properly"
        Write-Host ""
        
        return $exePath
    }
    catch {
        Write-Host "`n================================================================" -ForegroundColor Red
        Write-Host "                      BUILD FAILED!                          " -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Error "Error: $_"
        Write-Host ""
        Write-Info "Check the error messages above for details."
        Write-Host ""
        exit 1
    }
}

# Run the build
Start-Build
