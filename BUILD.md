# Island Pacific Monitoring Suite - Build Guide

## Quick Start

### Automated Build (Recommended)

```powershell
# Full build (Maven + Installer)
.\build-installer.ps1

# Build installer only (skip Maven)
.\build-installer.ps1 -SkipMaven

# Clean build from scratch
.\build-installer.ps1 -Clean

# Skip binary validation (if missing Prometheus/Grafana)
.\build-installer.ps1 -SkipValidation
```

---

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17+**
   - Download: https://adoptium.net/
   - Verify: `java -version`

2. **Apache Maven 3.6+**
   - Download: https://maven.apache.org/download.cgi
   - Verify: `mvn --version`

3. **Inno Setup 6**
   - Download: https://jrsoftware.org/isdl.php
   - Install to default location: `C:\Program Files (x86)\Inno Setup 6\`

### Required Binaries (in `installer/resources/`)

Download and place these binaries in the appropriate subdirectories:

| Binary | Download | Location |
|--------|----------|----------|
| **Prometheus** | https://prometheus.io/download/ | `installer/resources/prometheus/` |
| **Grafana** | https://grafana.com/grafana/download | `installer/resources/grafana/` |
| **Loki** | https://github.com/grafana/loki/releases | `installer/resources/loki/` |
| **Promtail** | https://github.com/grafana/loki/releases | `installer/resources/promtail/` |
| **WinSW** | https://github.com/winsw/winsw/releases | `installer/resources/` |

---

## Build Process

### Step 1: Build Java Monitoring Services

```powershell
# Build all 12 monitoring JARs
mvn clean package

# Output: target/monitors/*.jar
```

**Generated JARs:**
- IBMIFSErrorMonitor.jar
- IBMRealTimeIFSMonitor.jar
- IBMJobQueCountMonitor.jar
- IBMJobQueStatusMonitor.jar
- IBMSubSystemMonitor.jar
- IBMMatrixMonitor.jar
- IBMQSYSOPRMonitor.jar
- IBMProfieDisable.jar (UserProfileChecker)
- IBMNetworkEnabler.jar
- ServerUpTimeMonitor.jar
- WinFSErrorMonitor.jar
- WinFSCardinalityMonitor.jar

### Step 2: Copy JARs to Installer Resources

The build script automatically copies JARs to:
```
installer/resources/monitoring-services/
├── IBMIFSErrorMonitor/
├── IBMRealTimeIFSMonitor/
├── IBMJobQueCountMonitor/
├── IBMJobQueStatusMonitor/
├── ServerUpTimeMonitor/
├── IBMSubSystemMonitoring/
├── IBMSystemMatrix/
├── IBMUserProfileChecker/
├── NetWorkEnabler/
├── QSYSOPRMonitoring/
├── WinFSErrorMonitor/
└── WinFSCardinalityMonitor/
```

### Step 3: Compile Inno Setup Installer

```powershell
# Using the build script (recommended)
.\build-installer.ps1

# Or manually
& "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer.iss
```

**Output:** `installer/output/IslandPacificMonitoringSetup.exe`

---

## Manual Build Steps

If you prefer to build manually:

### 1. Build Java Services
```powershell
mvn clean package -DskipTests
```

### 2. Copy JARs Manually
```powershell
# Example for one service
Copy-Item "target\monitors\IBMIFSErrorMonitor.jar" `
          "installer\resources\monitoring-services\IBMIFSErrorMonitor\" -Force
```

### 3. Compile Installer
```powershell
& "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer.iss
```

---

## Build Script Options

### Parameters

| Parameter | Description |
|-----------|-------------|
| `-SkipMaven` | Skip Maven build, use existing JARs |
| `-SkipValidation` | Skip validation of external binaries |
| `-Clean` | Remove target/ and output/ before building |

### Examples

```powershell
# Quick rebuild after code changes
.\build-installer.ps1

# Rebuild installer only (JARs unchanged)
.\build-installer.ps1 -SkipMaven

# Fresh build from scratch
.\build-installer.ps1 -Clean

# Build without Prometheus/Grafana validation
.\build-installer.ps1 -SkipValidation

# Combine options
.\build-installer.ps1 -Clean -SkipValidation
```

---

## Troubleshooting

### "Maven not found"
```powershell
# Add Maven to PATH
$env:Path += ";C:\apache-maven-3.9.0\bin"

# Or install via Chocolatey
choco install maven
```

### "Inno Setup Compiler not found"
- Install from: https://jrsoftware.org/isdl.php
- Or update `$InnoSetupPaths` in build-installer.ps1

### "Required binaries missing"
- Download from links above
- Place in correct subdirectories
- Or use `-SkipValidation` to build anyway

### "JAR file not copied"
- Check JAR naming in `pom.xml` (finalName)
- Update mapping in `Copy-MonitoringJars` function

### Build fails with "Access Denied"
```powershell
# Run as Administrator
Start-Process powershell -Verb RunAs -ArgumentList "-File build-installer.ps1"
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build Installer

on: [push, pull_request]

jobs:
  build:
    runs-on: windows-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Maven
      uses: stCarolas/setup-maven@v4.5
    
    - name: Install Inno Setup
      run: choco install innosetup -y
    
    - name: Build Installer
      run: .\build-installer.ps1 -SkipValidation
    
    - name: Upload Installer
      uses: actions/upload-artifact@v3
      with:
        name: installer
        path: installer/output/*.exe
```

---

## Output

### Successful Build

```
╔════════════════════════════════════════════════════════════════╗
║                    BUILD SUCCESSFUL!                           ║
╚════════════════════════════════════════════════════════════════╝

✓ Total build time: 45.2 seconds
✓ Installer: C:\RD\Monitoring\Monitoring\installer\output\IslandPacificMonitoringSetup.exe

Next steps:
  1. Test the installer on a clean VM
  2. Verify all services install correctly
  3. Check Grafana dashboards load properly
```

### Installer Details

- **Filename:** IslandPacificMonitoringSetup.exe
- **Size:** ~150-300 MB (compressed)
- **Compression:** LZMA (solid)
- **Contents:**
  - Prometheus binaries
  - Grafana binaries
  - Loki + Promtail
  - 12 monitoring service JARs
  - WinSW service wrapper
  - Configuration templates
  - Installation wizard

---

## Testing the Installer

### Test Checklist

- [ ] Run installer on clean Windows Server 2019/2022
- [ ] Verify all services install and start
- [ ] Check Grafana accessible at http://localhost:3000
- [ ] Check Prometheus at http://localhost:9090
- [ ] Verify monitors expose metrics
- [ ] Test email alerts (SMTP and OAuth2)
- [ ] Test multi-client installation
- [ ] Test upgrade from previous version
- [ ] Test uninstallation (data preservation)

### Quick Test

```powershell
# Install silently for testing
.\IslandPacificMonitoringSetup.exe /SILENT /CLIENT=TestClient

# Check services
Get-Service | Where-Object { $_.Name -like "IPMonitoring*" }

# Test Grafana
Start-Process "http://localhost:3000"
```

---

## Version Management

Update version in:
1. `installer.iss` → `#define AppVersion "1.0.0"`
2. `pom.xml` → `<version>1.0.0</version>`

---

## Support

For build issues:
1. Check this guide
2. Review build script output
3. Check `compile.log` for Maven errors
4. Check Inno Setup compiler output
