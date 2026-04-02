# Quick Build Examples
# Copy and paste these commands to build your installer

# ============================================
# EXAMPLE 1: Full Build (Recommended)
# ============================================
# Builds everything from scratch
.\build-installer.ps1


# ============================================
# EXAMPLE 2: Quick Rebuild
# ============================================
# After making changes to Java code
mvn clean package
.\build-installer.ps1 -SkipMaven


# ============================================
# EXAMPLE 3: Installer Only
# ============================================
# When you only changed the ISS file
.\build-installer.ps1 -SkipMaven


# ============================================
# EXAMPLE 4: Clean Build
# ============================================
# Start fresh, remove all build artifacts
.\build-installer.ps1 -Clean


# ============================================
# EXAMPLE 5: Build Without Validation
# ============================================
# If you don't have Prometheus/Grafana yet
.\build-installer.ps1 -SkipValidation


# ============================================
# EXAMPLE 6: Test the Installer
# ============================================
# After successful build
$installer = "installer\output\IslandPacificMonitoringSetup.exe"

# Check file size
(Get-Item $installer).Length / 1MB

# Run installer (GUI mode)
Start-Process $installer

# Or silent install for testing
& $installer /SILENT /CLIENT=TestClient /DIR="C:\Test\IPMonitoring"


# ============================================
# TROUBLESHOOTING
# ============================================

# Check if Maven is installed
mvn --version

# Check if Inno Setup is installed
Test-Path "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"

# List generated JARs
Get-ChildItem target\monitors -Filter *.jar | Select-Object Name, Length

# Check installer resources
Get-ChildItem installer\resources -Recurse -File | 
Where-Object { $_.Extension -in '.exe', '.jar' } | 
Select-Object FullName, @{N = 'SizeMB'; E = { [math]::Round($_.Length / 1MB, 2) } }


# ============================================
# MANUAL BUILD (if script fails)
# ============================================

# Step 1: Build Java
mvn clean package

# Step 2: Copy JARs (example for one service)
Copy-Item "target\monitors\IBMIFSErrorMonitor.jar" `
    "installer\resources\monitoring-services\IBMIFSErrorMonitor\" -Force

# Step 3: Compile installer
& "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer.iss


# ============================================
# FIRST TIME SETUP
# ============================================

# 1. Install prerequisites
# - Java JDK 17: https://adoptium.net/
# - Maven: https://maven.apache.org/download.cgi
# - Inno Setup: https://jrsoftware.org/isdl.php

# 2. Download required binaries to installer/resources/
# - Prometheus: https://prometheus.io/download/
# - Grafana: https://grafana.com/grafana/download
# - Loki: https://github.com/grafana/loki/releases
# - Promtail: https://github.com/grafana/loki/releases
# - WinSW: https://github.com/winsw/winsw/releases

# 3. Run the build
.\build-installer.ps1
