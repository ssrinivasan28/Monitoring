# Island Pacific Operation Monitor Suite
## Functional Specification Document

**Document Version**: 1.0  
**Date**: January 2026  
**Author**: Island Pacific Retail Systems

---

## 1. Overview

The Island Pacific Operation Monitor Suite is a comprehensive monitoring solution designed to monitor IBM i servers, Windows file systems, and server infrastructure. It provides real-time metrics collection, alerting via email (SMTP or Microsoft 365 OAuth2), and beautiful Grafana dashboards for visualization.

### 1.1 Purpose
This document describes the functional capabilities, components, and behavior of the Island Pacific Operation Monitor Suite.

### 1.2 Scope
- IBM i server monitoring (IFS, job queues, subsystems, system health)
- Windows file system monitoring
- Metrics collection and visualization
- Email alerting system
- Installation and deployment

---

## 2. Components

### Core Infrastructure

| Component | Description | Default Port |
|-----------|-------------|--------------|
| **Prometheus** | Time-series metrics database that scrapes and stores metrics from all monitors | 9090 |
| **Grafana** | Dashboard and visualization platform for viewing metrics and creating alerts | 3000 |
| **Loki** | Log aggregation system (optional, for centralized logging) | 3100 |
| **Promtail** | Log shipping agent that sends logs to Loki | N/A |

### IBM i Monitors

| Monitor | Description | Default Port | Properties File |
|---------|-------------|--------------|-----------------|
| **IFS Error Monitor** | Monitors IBM i IFS directories for error files (e.g., APP_ERRORS) | 3010 | monitor.properties |
| **Real-Time IFS Monitor** | Real-time monitoring of IFS file changes | 3011 | ifsmonitor.properties |
| **File Member Monitor** | Monitors IBM i file members for changes and growth | 3014 | filemembermonitor.properties |
| **Job Queue Count Monitor** | Monitors job queue depths and counts | 3012 | jobqueuemonitor.properties |
| **Job Queue Status Monitor** | Monitors individual job statuses in queues | 3013 | joblist.properties |
| **SubSystem Monitor** | Monitors IBM i subsystem status and jobs | 3016 | subsystem.properties |
| **System Matrix Monitor** | Collects system-wide IBM i metrics | 3017 | systemmonitor.properties |
| **User Profile Checker** | Monitors user profile status and changes | 3018 | userprofilecheck.properties |
| **Network Enabler** | Monitors network connectivity | 3019 | networkenable.properties |
| **QSYSOPR Monitor** | Monitors QSYSOPR message queue for critical messages | 3020 | job_failure.properties |

---

## IBM i Monitor Details

### IFS Error Monitor
**Purpose**: Monitors IBM i Integrated File System (IFS) directories for error files that indicate application failures or issues.

**What it does**:
- Scans configured IFS directories (e.g., `/APP_ERRORS`, `/BATCH_ERRORS`) for new files
- Detects files matching specified extensions (`.err`, `.log`, `.txt`, etc.)
- Tracks file counts by extension type
- Sends email alerts when new error files are detected
- Exposes metrics to Prometheus for trending and alerting

**Use Cases**:
- Application error log monitoring
- Batch job failure detection
- EDI/interface error file monitoring
- Custom application error tracking

**Key Configuration** (`monitor.properties`):
```properties
ibmi.host=your-ibmi-server
ibmi.user=MONITORUSER
ibmi.password=password
client.name=CLIENTNAME
monitor.locations[0].name=APP_ERRORS
monitor.locations[0].path=/APP_ERRORS
monitor.locations[0].file.extensions=.err,.log
```

---

### Real-Time IFS Monitor
**Purpose**: Provides real-time monitoring of IFS file system changes with immediate alerting capabilities.

**What it does**:
- Watches specified IFS directories for file creation, modification, and deletion
- Detects changes in near real-time (configurable polling interval)
- Tracks file metadata (size, timestamp, count)
- Alerts on threshold breaches (file count, total size)
- Monitors multiple directories simultaneously

**Use Cases**:
- Real-time error detection
- File landing zone monitoring
- Interface file arrival tracking
- Batch file processing validation

**Key Configuration** (`ifsmonitor.properties`):
```properties
ibmi.host=your-ibmi-server
monitor.interval.seconds=30
monitor.directories=/INTERFACE/IN,/INTERFACE/OUT
alert.threshold.filecount=100
```

---

### File Member Monitor
**Purpose**: Monitors IBM i physical/logical file members for changes, size growth, and member count to detect anomalies in application data files.

**What it does**:
- Scans configured IBM i files/members for size and record count changes
- Tracks member additions/removals
- Alerts when member sizes exceed thresholds
- Exposes metrics for capacity and growth trending

**Use Cases**:
- Monitoring application master/transaction files for unexpected growth
- Detecting runaway batch processes increasing member size
- Auditing member creation/removal events

**Key Configuration** (`filemembermonitor.properties`):
```properties
ibmi.host=your-ibmi-server
client.name=CLIENTNAME
files[0].library=PRODLIB
files[0].file=CUSTFILE
files[0].member=*FIRST
alert.threshold.size.mb=500
alert.threshold.records=1000000
```

**Exposed Metrics**:
- `ibm_filemember_size_bytes{library="PRODLIB",file="CUSTFILE",member="MBR"}`
- `ibm_filemember_record_count{library="PRODLIB",file="CUSTFILE",member="MBR"}`

---

### Job Queue Count Monitor
**Purpose**: Monitors IBM i job queue depths to detect backlogs and processing delays.

**What it does**:
- Queries job queue status via IBM i system APIs
- Tracks number of jobs waiting in each monitored queue
- Calculates queue depth trends over time
- Alerts when queue depth exceeds thresholds
- Provides metrics for capacity planning

**Use Cases**:
- Batch job backlog detection
- Night batch monitoring
- Peak load monitoring
- SLA compliance tracking

**Key Configuration** (`jobqueuemonitor.properties`):
```properties
ibmi.host=your-ibmi-server
jobqueue.names=QBATCH,QINTER,NIGHTBATCH,CUSTQUEUE
alert.threshold.depth=50
```

**Exposed Metrics**:
- `ibm_jobqueue_count{queue="QBATCH"}` - Current job count per queue
- `ibm_jobqueue_held_count{queue="QBATCH"}` - Held jobs per queue

---

### Job Queue Status Monitor
**Purpose**: Monitors individual jobs in queues for status, duration, and potential issues.

**What it does**:
- Lists all jobs in monitored queues
- Tracks job status (ACTIVE, HELD, MSGW, etc.)
- Monitors job run duration
- Detects long-running jobs
- Identifies jobs waiting for messages (MSGW)
- Alerts on job failures or stuck jobs

**Use Cases**:
- Long-running job detection
- Job failure alerting
- Message wait (MSGW) detection
- Batch processing SLA monitoring

**Key Configuration** (`joblist.properties`):
```properties
ibmi.host=your-ibmi-server
monitor.queues=QBATCH,NIGHTBATCH
alert.longrunning.minutes=60
alert.on.msgw=true
```

**Exposed Metrics**:
- `ibm_job_status{job="JOBNAME",status="ACTIVE"}` - Job status
- `ibm_job_duration_seconds{job="JOBNAME"}` - Job run time

---

### SubSystem Monitor
**Purpose**: Monitors IBM i subsystem health, status, and job activity.

**What it does**:
- Monitors subsystem status (ACTIVE, INACTIVE, RESTRICTED)
- Tracks active job counts per subsystem
- Monitors subsystem pool allocation
- Detects subsystem failures or unexpected stops
- Tracks subsystem storage usage

**Use Cases**:
- Subsystem availability monitoring
- Capacity monitoring
- Detecting subsystem crashes
- Pool utilization tracking

**Key Configuration** (`subsystem.properties`):
```properties
ibmi.host=your-ibmi-server
client.name=CLIENTNAME
subsystems=QBATCH,QINTER,QCMN,QSPL,CUSTOM1
alert.on.inactive=true
```

**Exposed Metrics**:
- `ibm_subsystem_status{subsystem="QBATCH"}` - 1=active, 0=inactive
- `ibm_subsystem_jobs_active{subsystem="QBATCH"}` - Active job count
- `ibm_subsystem_jobs_max{subsystem="QBATCH"}` - Max jobs allowed

---

### System Matrix Monitor
**Purpose**: Collects comprehensive system-wide IBM i performance and health metrics.

**What it does**:
- Monitors CPU utilization (system and interactive)
- Tracks disk space usage (ASPs)
- Monitors memory pool utilization
- Tracks system job counts
- Monitors page faulting and disk I/O
- Collects PTF and system status information

**Use Cases**:
- System health dashboards
- Performance trending
- Capacity planning
- Problem diagnosis
- SLA reporting

**Key Configuration** (`systemmonitor.properties`):
```properties
ibmi.host=your-ibmi-server
client.name=CLIENTNAME
collect.cpu=true
collect.disk=true
collect.memory=true
collect.jobs=true
```

**Exposed Metrics**:
- `ibm_system_cpu_percent` - CPU utilization
- `ibm_system_asp_used_percent{asp="1"}` - Disk usage per ASP
- `ibm_system_memory_pool_used{pool="*BASE"}` - Memory pool usage
- `ibm_system_active_jobs` - Total active jobs
- `ibm_system_page_faults` - Page fault rate

---

### User Profile Checker
**Purpose**: Monitors IBM i user profile status for security and compliance.

**What it does**:
- Checks user profile status (ENABLED, DISABLED, EXPIRED)
- Monitors password expiration dates
- Detects unauthorized status changes
- Tracks user profile modifications
- Alerts on security-related changes

**Use Cases**:
- Security compliance monitoring
- Audit trail for user changes
- Password expiration tracking
- Detecting disabled critical accounts

**Key Configuration** (`userprofilecheck.properties`):
```properties
ibmi.host=your-ibmi-server
client.name=CLIENTNAME
monitor.profiles=QSECOFR,APPUSER,BATCHUSER,WEBUSER
alert.on.disabled=true
alert.on.password.expiring.days=7
```

**Exposed Metrics**:
- `ibm_userprofile_status{user="APPUSER"}` - 1=enabled, 0=disabled
- `ibm_userprofile_password_expires_days{user="APPUSER"}` - Days until expiry

---

### Network Enabler Monitor
**Purpose**: Monitors network connectivity and communication status on IBM i.

**What it does**:
- Tests connectivity to configured endpoints
- Monitors TCP/IP stack status
- Tracks network interface status
- Measures response times
- Detects network failures

**Use Cases**:
- Network health monitoring
- Remote system connectivity
- Interface availability tracking
- Network troubleshooting

**Key Configuration** (`networkenable.properties`):
```properties
ibmi.host=your-ibmi-server
network.endpoints=192.168.1.100:443,dbserver:1433
ping.interval.seconds=60
alert.on.failure=true
```

---

### QSYSOPR Monitor
**Purpose**: Monitors the QSYSOPR message queue for critical system and application messages.

**What it does**:
- Reads messages from QSYSOPR message queue
- Filters messages by severity and type
- Detects critical system messages
- Identifies job failure notifications
- Alerts on configurable message patterns

**Use Cases**:
- System message monitoring
- Job failure notification
- Disk space warnings
- Security alerts
- Custom application messages

**Key Configuration** (`job_failure.properties`):
```properties
ibmi.host=your-ibmi-server
client.name=CLIENTNAME
message.severity.min=30
message.types=*ESCAPE,*NOTIFY
alert.patterns=CPF*,CPA*,MCH*
```

**Exposed Metrics**:
- `ibm_qsysopr_message_count{severity="40"}` - Messages by severity
- `ibm_qsysopr_critical_count` - Critical message count

### Windows Monitors

| Monitor | Description | Default Port | Properties File |
|---------|-------------|--------------|-----------------|
| **Windows FS Error Monitor** | Monitors Windows directories for error files | 3021 | fserrormonitor.properties |
| **Windows FS Cardinality Monitor** | Monitors file counts and sizes in directories | 3022 | fscardinalitymonitor.properties |

### Other Monitors

| Monitor | Description | Default Port | Properties File |
|---------|-------------|--------------|-----------------|
| **Server UpTime Monitor** | Monitors server availability and uptime | 3015 | serverinfo.properties |

---

## Installation Roles

The installer supports three deployment modes:

### 1. Monitoring Server (Full Installation)
- Installs Prometheus, Grafana, Loki (optional)
- Installs selected IBM i and Windows monitors
- Central hub for metrics collection and visualization
- Typically installed on a dedicated monitoring server

### 2. Windows Agent (WinAgent)
- Installs only Windows monitors (FS Error, Cardinality)
- Installs Promtail for log shipping to central Loki
- No Prometheus/Grafana/Loki installed
- Lightweight deployment for application servers

### 3. Log Agent Only
- Installs only Promtail
- Ships logs to central Loki server
- Minimal footprint

---

## What the Installer Does

### Pre-Installation
1. Detects existing installations (upgrade vs. new install)
2. Validates port availability
3. Loads previous configuration from registry (if upgrading)

### Installation Steps

#### 1. Directory Structure Creation
```
{Install Path}\
├── services\           # WinSW service executables
├── logs\               # Application logs (preserved on upgrade)
├── data\               # Metrics data (preserved on upgrade)
│   ├── prometheus\     # Prometheus TSDB data
│   └── loki\           # Loki log data
├── prometheus\         # Prometheus binaries and config
├── grafana\            # Grafana server
│   ├── bin\
│   ├── conf\
│   └── data\           # Dashboards, users (preserved)
├── loki\               # Loki server (if Monitoring Server role)
├── promtail\           # Promtail agent (if Log Agent role)
└── monitoring-services\
    ├── IBMIFSErrorMonitor\
    ├── IBMRealTimeIFSMonitor\
    ├── IBMJobQueCountMonitor\
    ├── IBMJobQueStatusMonitor\
    ├── ServerUpTimeMonitor\
    ├── IBMSubSystemMonitoring\
    ├── IBMSystemMatrix\
    ├── IBMUserProfileChecker\
    ├── NetWorkEnabler\
    ├── QSYSOPRMonitoring\
    ├── WinFSErrorMonitor\
    └── WinFSCardinalityMonitor\
```

#### 2. Service Installation
For each selected monitor, the installer:
- Copies the JAR file to the appropriate directory
- Copies properties files (only if they don't exist - preserves customizations)
- Creates WinSW service XML configuration
- Registers the Windows service
- Starts the service

#### 3. Configuration Updates
- **prometheus.yml**: Adds scrape targets for each enabled monitor
- **grafana/conf/custom.ini**: Sets port and admin password
- **grafana/conf/provisioning/**: Configures datasources and dashboard providers
- **email.properties**: Generated for each monitor with SMTP/OAuth2 settings
- **Monitor properties**: Updates IBM i credentials, ports, client names

#### 4. Registry Storage
All configuration is stored in the Windows Registry for persistence:
```
HKLM\Software\IslandPacific\Monitoring\
├── InstallPath
├── PrometheusPort
├── GrafanaPort
├── GrafanaAdminPassword
├── IBMiServer
├── IBMiUser
├── IBMiPassword
├── ClientName
├── EmailAuthMethod
├── SmtpHost, SmtpPort, SmtpUsername, SmtpPassword
├── OAuthTenant, OAuthClientId, OAuthClientSecret
├── EmailFrom, EmailTo, EmailBcc
└── [Per-monitor port settings]
```

---

## Email Alert Configuration

### SMTP Authentication
Traditional username/password authentication for email servers:
- SMTP Host and Port
- Username and Password
- TLS/StartTLS support

### OAuth2 (Microsoft 365)
Modern authentication using Azure AD:
- Tenant ID
- Client ID and Secret
- Graph API integration
- No password storage required

### Email Features
- **HTML formatted emails** with company logo
- **High/Normal/Low importance** settings
- **To and BCC recipients** support
- **Client name in subject** for easy identification

---

## Multi-Client Support

The installer supports multiple client installations on the same server:
- Each client gets a unique **Client Instance ID**
- Separate installation directories
- Separate Windows services (suffixed with client ID)
- Separate registry branches
- Port offset (+100 per client) to avoid conflicts

---

## Upgrade Behavior

When upgrading an existing installation:
- **JAR files**: Always updated to latest version
- **Properties files**: Preserved (only created if missing)
- **Data directories**: Preserved (metrics, dashboards, logs)
- **Configuration**: Loaded from registry, can be modified
- **Services**: Restarted after upgrade

---

## Post-Installation

### Accessing Dashboards
- **Grafana**: `http://localhost:3000` (or configured port)
- **Prometheus**: `http://localhost:9090` (or configured port)

### Pre-configured Dashboards
- IBM i Monitoring - Performance Monitor
- IBM i and Windows Realtime Server Monitor
- IBM i JobQue Count Monitor
- IBM i JobQue Status Monitor
- IBM i Monitoring – IFS Error Monitor
- And more...

### Service Management
All services are installed as Windows Services and can be managed via:
- Services.msc
- PowerShell: `Get-Service IPMonitoring*`
- Command line: `sc query IPMonitoring*`

---

## Uninstallation

The uninstaller:
1. Stops all monitoring services
2. Unregisters Windows services
3. Removes service executables and XML configurations
4. **Preserves** logs and data directories (for reinstallation)
5. Cleans up registry entries

---

## File Descriptions

| File | Purpose |
|------|---------|
| `installer.iss` | Inno Setup installer script |
| `pom.xml` | Maven build configuration |
| `*.properties` | Monitor configuration files |
| `email.properties` | Email settings (generated by installer) |
| `WinSW.exe` | Windows Service Wrapper for Java services |

---

## Troubleshooting

### Common Issues

1. **Port conflicts**: Use `netstat -ano | findstr :PORT` to check
2. **Service won't start**: Check logs in `{app}\logs\`
3. **Email not sending**: Verify email.properties configuration
4. **IBM i connection failed**: Verify credentials and network access

### Log Locations
- Monitor logs: `{app}\monitoring-services\{MonitorName}\logs\`
- Prometheus logs: `{app}\prometheus\`
- Grafana logs: `{app}\grafana\data\log\`

---

## Version Information

- **Version**: 1.0.0
- **Publisher**: Island Pacific Retail Systems
- **Support**: https://www.islandpacific.com/
