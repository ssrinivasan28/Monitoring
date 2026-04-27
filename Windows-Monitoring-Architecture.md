# Windows Monitoring Suite — Architecture & Design Document

**Suite:** Island Pacific Operations Monitor — Windows Agent  
**Version:** 1.0  
**Author:** Island Pacific Retail Systems  
**Purpose:** Architecture reference for the five Windows-targeted monitoring microservices

---

## 1. Overview

The Windows Monitoring Suite is a collection of five independent Java microservices that monitor different aspects of Windows server infrastructure. Each service is a self-contained shaded JAR, runs as a named Windows service via WinSW (Windows Service Wrapper), exposes a Prometheus `/metrics` endpoint, and sends HTML email alerts via SMTP or Microsoft 365 OAuth2.

The five services are:

| Service | JAR | Default Port | Windows Service Name |
|---|---|---|---|
| WinMonitor | `WinMonitor.jar` | 3022 | `IPMonitoring_WinMonitor` |
| WinFSErrorMonitor | `WinFSErrorMonitor.jar` | 3020 | `IPMonitoring_WinFSErrorMonitor` |
| WinFSCardinalityMonitor | `WinFSCardinalityMonitor.jar` | 3021 | `IPMonitoring_WinFSCardinalityMonitor` |
| LogKeywordMonitor | `LogKeywordMonitor.jar` | 3023 | `IPMonitoring_LogKeywordMonitor` |
| ServerUpTimeMonitor | `ServerUpTimeMonitor.jar` | 3014 | `IPMonitoring_ServerUpTimeMonitor` |

> **Port offset rule:** For multi-client installations on the same machine, each additional client instance receives a 100-port offset (client 2: 3120, 3121, etc.).

---

## 2. Common Architecture

All five services share the same internal structure, threading model, configuration pattern, and email delivery infrastructure. Understanding the common layer is the foundation for understanding each individual service.

### 2.1 Package Structure

Every service package follows this layout:

```
com.islandpacific.monitoring.<package>/
├── Main*.java                  Entry point — loads config, wires services, starts scheduler
├── *Config.java                Parses .properties files into typed config objects
├── *Service.java               Core business logic (the "what to monitor" implementation)
├── *Metrics.java               Prometheus metrics — either static Gauge fields or Collector subclass
├── *AppServer.java             Tiny HTTP server on configured port serving /metrics
├── EmailService.java           Sends HTML email alerts via SMTP or OAuth2
└── OAuth2TokenProvider.java    Azure AD bearer token fetching with 5-minute expiry buffer cache
```

### 2.2 Threading Model

All services use a single-threaded scheduler:

```
Main thread
  └── Loads config, wires services, starts HTTP server
  └── Registers shutdown hook
  └── Schedules monitoring loop → newSingleThreadScheduledExecutor()
        └── Monitoring cycle runs on scheduler thread (no overlap, no stacking)

Metrics HTTP server
  └── Single-threaded executor — serves /metrics independently of monitoring loop

Log purge thread
  └── Daemon thread — removes log files older than retention period
```

The monitoring cycle is deliberately single-threaded: each cycle completes fully before the next begins. This means a slow or hung cycle delays the next poll rather than stacking concurrent cycles that could overload the target system.

**Exception:** WinMonitor uses a fixed thread pool (`windows.poll.threads`, default 5) to poll multiple hosts in parallel within each cycle. All host results are collected before the cycle completes.

### 2.3 Configuration Pattern

Every service is configured by two `.properties` files:

**`email.properties`** — shared across all monitors, controls alert delivery:

| Property | Description |
|---|---|
| `mail.auth.method` | `SMTP` or `OAUTH2` |
| `mail.smtp.host` | SMTP server hostname |
| `mail.smtp.port` | SMTP port (default `25`) |
| `mail.from` | Sender address |
| `mail.to` | Primary recipient(s), comma-separated |
| `mail.bcc` | BCC recipient(s), comma-separated |
| `mail.smtp.auth` | `true` / `false` |
| `mail.smtp.starttls.enable` | `true` / `false` |
| `mail.smtp.username` / `mail.smtp.password` | SMTP credentials |
| `mail.oauth2.tenant.id` | Azure AD tenant ID |
| `mail.oauth2.client.id` | Azure AD application (client) ID |
| `mail.oauth2.client.secret` | Azure AD client secret |
| `mail.oauth2.token.url` | Token endpoint (blank = default Microsoft endpoint) |
| `mail.oauth2.graph.mail.url` | Graph API sendMail URL |
| `log.level` | Java log level (`INFO`, `WARNING`, `SEVERE`) |
| `log.folder` | Directory for log files |
| `log.retention.days` | Days to retain log files |
| `log.purge.interval.hours` | How often to run the log purge |

**Monitor-specific `.properties`** — controls what is monitored (each service has its own, documented in the per-service sections below).

Properties files are installed with `onlyifdoesntexist` — upgrading the monitoring package never overwrites a live configuration file. All site-specific settings survive upgrades.

### 2.4 Logging

All services use the shared `AppLogger` from `com.islandpacific.monitoring.common`:

- Produces daily-rotating log files named `<moduleName>_YYYY-MM-DD.log`
- Writes to `/app/logs` when running inside Docker (auto-detected), otherwise to the configured `log.folder`
- Background daemon thread purges files older than `log.retention.days`
- `AppLogger.setupLogger()` is always called before any other initialisation to ensure all subsequent log output goes to the correct file

### 2.5 Email Delivery

Two delivery modes are supported, selected by `mail.auth.method`:

**SMTP** — Standard JavaMail. Supports plain (no auth), authenticated (username/password), and STARTTLS. Suitable for internal mail relays.

**OAuth2 / Microsoft Graph API** — For Microsoft 365 environments. The service authenticates as an Azure AD application (client credentials flow — no user interaction), obtains a bearer token from the Azure AD token endpoint, and posts the email to the Microsoft Graph `/sendMail` API. Tokens are cached in memory and reused until 60 seconds before expiry to avoid a round-trip on every email send.

**Email format** — All alert emails use an HTML card layout:
- Island Pacific logo bar (embedded inline as `cid:logo` for OAuth2, data URI for SMTP)
- Colour-coded badge (red for alerts, green for recovery, blue for info)
- Details table with relevant metrics and thresholds
- Footer with timestamp and monitor host name

### 2.6 Graceful Shutdown

All services register a JVM shutdown hook that:
1. Calls `scheduler.shutdown()` to stop accepting new cycles
2. Calls `scheduler.awaitTermination(10–30s)`, falling back to `shutdownNow()`
3. Stops the metrics HTTP server
4. Flushes and closes any open per-location `FileHandler` log files

### 2.7 Prometheus Metrics

Metrics are served at `http://localhost:<port>/metrics` in standard Prometheus text format. Prometheus scrapes this endpoint; Grafana dashboards visualise the data; Promtail/Loki handles log aggregation.

Metric values are updated at the end of each successful poll cycle. If a cycle fails or a host times out, previously published values remain unchanged in Prometheus until the next successful poll — metrics never drop to zero due to a transient failure.

---

## 3. WinMonitor

**Package:** `windowsmonitoring`  
**Properties file:** `windowsmonitor.properties`  
**Default port:** 3022

### 3.1 Purpose

Continuously monitors the health of one or more Windows servers: CPU, memory, disk usage, running processes, system uptime, and Windows service states. Publishes all metrics to Prometheus and sends email alerts when any metric breaches its configured threshold for a sustained number of consecutive poll cycles.

### 3.2 System Context

```
┌────────────────────────────────────────────────┐
│             Monitored Environment              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Local   │  │ Remote   │  │ Remote   │     │
│  │  Host    │  │ Host 1   │  │ Host 2   │     │
└──┴──────────┴──┴──────────┴──┴──────────┴─────┘
        │ OSHI (local)     │ WinRM/PowerShell
        └──────────────────┘
                   │
        ┌──────────▼──────────┐
        │      WinMonitor     │
        │  ┌───────────────┐  │◄── Prometheus scrapes :3022/metrics
        │  │ Metrics HTTP  │  │
        │  └───────────────┘  │
        │  ┌───────────────┐  │
        │  │ Email Alerts  │──┼──► SMTP / Microsoft 365
        │  └───────────────┘  │
        └─────────────────────┘
```

### 3.3 What Is Measured

| Metric | Local Collection | Remote Collection |
|---|---|---|
| CPU utilisation % | OSHI processor tick snapshots (interval average) | WMI `Win32_Processor.LoadPercentage` |
| Memory total GB | OSHI `GlobalMemory` | WMI `Win32_OperatingSystem.TotalVisibleMemorySize` |
| Memory used / free GB | OSHI `GlobalMemory` | Derived from WMI values |
| Memory utilisation % | Derived from raw bytes | Derived from raw KB values |
| Disk total / used / free GB | OSHI `OSFileStore` per logical volume | WMI `Win32_LogicalDisk` |
| Disk utilisation % | Derived | Derived |
| Top N processes (name + CPU %) | OSHI process list sorted by CPU | `Get-Process` sorted by CPU |
| System uptime (hours) | OSHI `getSystemUptime()` | WMI `LastBootUpTime` date arithmetic |
| Windows service run state | `sc query <serviceName>` | `Get-Service` |

### 3.4 Local vs Remote Collection

**Local (OSHI):** When the monitored host is the local machine, the OSHI (Operating System and Hardware Information) library reads system counters directly — no external processes spawned, no network required. CPU is measured as the delta between processor tick snapshots taken between consecutive poll cycles, giving a true interval-average CPU percentage.

**Remote (PowerShell/WinRM):** When the monitored host is a remote machine, WinRM is used. Each metric group is a separate PowerShell invocation returning JSON. If credentials are configured, they are passed as a PowerShell `PSCredential` object constructed from an OS environment variable — the password never appears in the command-line string itself, preventing exposure in process listings or audit logs. Each PowerShell process is given 25 seconds to complete; if it does not finish, it is forcibly terminated and its result discarded for that cycle.

### 3.5 Alert System — Sustained Breach Window

WinMonitor does not alert on a single threshold breach. Every alert type uses a **consecutive breach counter**: the threshold must be exceeded for `windows.alert.window.size` consecutive poll cycles before an email is sent. Once the metric recovers to within threshold, the counter resets to zero.

```
Cycle 1: CPU = 95%  → breach counter = 1  (no alert)
Cycle 2: CPU = 97%  → breach counter = 2  (no alert)
Cycle 3: CPU = 92%  → breach counter = 3  → ALERT SENT
Cycle 4: CPU = 91%  → breach counter = 4  → suppressed
Cycle 5: CPU = 60%  → breach counter = 0  → recovered, window reset
```

### 3.6 Alert Types

| Alert | Trigger | Email content |
|---|---|---|
| System alert (CPU/Memory) | Either CPU or memory above threshold for N consecutive cycles | Breached metrics table, stopped services, top N processes, uptime |
| Disk usage alert | Disk utilisation above threshold for N consecutive cycles | Per-drive usage figures |
| Rapid disk growth alert | Any drive grew >5% in a single cycle, for N consecutive cycles | Drive letter, growth magnitude |
| Service stopped alert | A configured service is non-Running for N consecutive cycles | Service name, current state |
| Monitoring cycle error | Unexpected exception during the polling cycle itself | Exception message (no windowing — immediate alert) |

### 3.7 Prometheus Metrics

| Metric Name | Labels | Description |
|---|---|---|
| `windows_cpu_usage_percent` | `server` | CPU utilisation % |
| `windows_memory_total_gb` | `server` | Total RAM in GB |
| `windows_memory_used_gb` | `server` | Used RAM in GB |
| `windows_memory_free_gb` | `server` | Free RAM in GB |
| `windows_memory_usage_percent` | `server` | Memory utilisation % |
| `windows_system_uptime_hours` | `server` | Hours since last boot |
| `windows_disk_total_gb` | `server`, `drive` | Total disk per drive |
| `windows_disk_used_gb` | `server`, `drive` | Used disk per drive |
| `windows_disk_usage_percent` | `server`, `drive` | Disk utilisation % per drive |
| `windows_service_status` | `server`, `service` | 1 = Running, 0 = any other state |

The `drive` label is the mount point — `C:\` for local OSHI, `C:` for remote WMI.

### 3.8 Configuration Reference — `windowsmonitor.properties`

| Property | Default | Description |
|---|---|---|
| `windows.servers.list` | `localhost` | Comma-separated hosts to monitor |
| `windows.server.<host>.username` | — | WinRM username for a remote host |
| `windows.server.<host>.password` | — | WinRM password for a remote host |
| `windows.monitor.interval.seconds` | `300` | Poll cycle interval |
| `windows.poll.threads` | `5` | Max parallel host polls per cycle |
| `windows.alert.threshold.cpu` | `90` | CPU % that triggers breach windowing |
| `windows.alert.threshold.memory` | `90` | Memory % that triggers breach windowing |
| `windows.alert.threshold.disk` | `90` | Disk % that triggers breach windowing |
| `windows.alert.window.size` | `3` | Consecutive breaches before alert fires |
| `windows.top.n.processes` | `5` | Number of top processes in alert email |
| `windows.services.to.monitor` | — | Comma-separated Windows service names |
| `metrics.exporter.port` | `3022` | Prometheus HTTP port |

### 3.9 Host Resilience

If a remote host is unreachable or a PowerShell invocation times out:
- That host's result is skipped for the current cycle
- Its previously published Prometheus metrics remain unchanged
- Breach counters for that host do not advance and do not reset
- The next cycle retries the host normally

---

## 4. WinFSErrorMonitor

**Package:** `filesystemerrormonitoring`  
**Properties file:** `fserrormonitor.properties`  
**Default port:** 3020

### 4.1 Purpose

Watches one or more configured Windows folders for the appearance of new error files (identified by file extension). When a new file is detected it sends an email alert immediately — there is no breach windowing. Each file is tracked by its absolute path so it is never reported more than once across restarts.

### 4.2 What Is Monitored

- **Locations:** Any number of local Windows folder paths, each with its own configured file extensions to watch (e.g. `.err`, `.wrn`, `.dmp`)
- **Scan depth:** Non-recursive — only immediate folder contents are scanned (depth = 1)
- **New file detection:** Each location maintains a `Set<String>` of previously seen absolute file paths, persisted to a state file under `logs/processed/`. On startup the state file is re-read so files seen before a service restart are not re-alerted
- **Email trigger:** Immediate on first detection of a new file — no consecutive-cycle windowing

### 4.3 State Persistence

State files are stored at:
```
logs/processed/fs_error_<locationname>.txt
```
Each line is an absolute file path that has already been reported. The file is appended to as new files are detected. On service start the file is read back into memory to restore the processed-paths set.

### 4.4 Prometheus Metrics

| Metric Name | Labels | Description |
|---|---|---|
| `fs_error_total_files` | `location`, `file_type` | Total files currently present per extension |
| `fs_error_new_files_detected` | `location`, `file_type` | Cumulative new files detected since service start |
| `fs_error_monitor_last_scan_timestamp_seconds` | — | Epoch seconds of the last completed scan |
| `fs_error_monitor_uptime_seconds` | — | Service uptime in seconds |

### 4.5 Configuration Reference — `fserrormonitor.properties`

| Property | Default | Description |
|---|---|---|
| `email.config.path` | `email.properties` | Path to the shared email properties file |
| `client.name` | `Windows FS Error Monitor` | Client name used in email subject and body |
| `check.interval.minutes` | — | Scan interval in minutes (preferred) |
| `monitor.interval.ms` | `300000` | Scan interval in milliseconds (fallback if `check.interval.minutes` absent) |
| `metrics.port` | `3020` | Prometheus HTTP port |
| `log.level` | `INFO` | Log level |
| `log.folder` | `logs` | Log file directory |
| `log.retention.days` | `30` | Log retention period |
| `log.purge.interval.hours` | `24` | Log purge frequency |
| `fs.location.<code>.name` | — | Display name for this monitored location |
| `fs.location.<code>.path` | — | Absolute folder path to monitor |
| `fs.location.<code>.file.types` | — | Comma-separated extensions to watch (e.g. `err,wrn`) |
| `fs.location.<code>.skip.to.emails` | — | Additional email recipients for this location |
| `fs.location.<code>.email.importance` | `Normal` | Email importance flag |

Multiple locations are configured by repeating the `fs.location.<code>.*` block with different `<code>` values (e.g. `fs.location.invoices.*`, `fs.location.shipments.*`).

---

## 5. WinFSCardinalityMonitor

**Package:** `filesystemcardinalitymonitoring`  
**Properties file:** `fscardinalitymonitor.properties`  
**Default port:** 3021

### 5.1 Purpose

Monitors the file count within configured Windows folders against minimum and maximum thresholds. Alerts when the count has been below the minimum or above the maximum for a sustained number of consecutive scan cycles. Unlike WinFSErrorMonitor (which fires immediately on new files), this monitor uses a breach window to avoid transient count fluctuations causing alert noise.

### 5.2 What Is Monitored

- **Locations:** Any number of local Windows folder paths, each with independent min/max thresholds
- **File types:** Optional extension filter — if configured, only matching files count toward the total. If empty, all regular files count
- **Recursive scan:** Optional per-location (`recursive=true/false`). Default non-recursive
- **Zero-file suppression:** Optional per-location `ignore.zero.file.alert=true` suppresses the too-few alert specifically when the count is exactly zero (useful for folders that are legitimately empty at off-peak times)

### 5.3 Alert System — Consecutive Breach Window

```
Cycle 1: count=2, min=5  → tooFewBreachCount = 1  (no alert)
Cycle 2: count=1, min=5  → tooFewBreachCount = 2  (no alert)
Cycle 3: count=0, min=5  → tooFewBreachCount = 3  → ALERT SENT
Cycle 4: count=8, min=5  → counters reset to 0     (recovered)
```

- `ALERT_WINDOW_SIZE = 3` (hardcoded)
- Both `tooFewBreachCount` and `tooManyBreachCount` are maintained independently per location
- When either breach fires, the other counter resets to zero
- On recovery (count within range), both counters reset to zero
- If `ignoreZeroFileAlert=true` and count=0, the tooFewBreachCount resets to 0 — no alert accumulates

### 5.4 New File Tracking

In addition to threshold alerting, the service tracks which individual files have been seen, persisting their absolute paths to state files (same pattern as WinFSErrorMonitor). This feeds the `newFilesDetected` Prometheus counter.

State files stored at:
```
logs/processed/fs_cardinality_<locationname>.txt
```

### 5.5 Per-Location Logging

Each monitored location gets its own `FileHandler`-backed logger in addition to the main logger. Location log files are written to `logs/<locationName>/`. This allows per-location audit trails without mixing all locations into the main log.

### 5.6 Prometheus Metrics

| Metric Name | Labels | Description |
|---|---|---|
| `fs_cardinality_file_count` | `location` | Current total file count |
| `fs_cardinality_too_few_alerts_total` | `location` | Cumulative too-few-files alert fires |
| `fs_cardinality_too_many_alerts_total` | `location` | Cumulative too-many-files alert fires |
| `fs_cardinality_monitor_uptime_seconds` | — | Service uptime in seconds |
| `fs_cardinality_last_scan_timestamp_seconds` | — | Epoch seconds of last completed scan |

### 5.7 Configuration Reference — `fscardinalitymonitor.properties`

| Property | Default | Description |
|---|---|---|
| `email.config.path` / `client.name` / `metrics.port` / `log.*` | (same pattern as WinFSErrorMonitor) | |
| `monitor.interval.minutes` | `5` | Scan interval in minutes |
| `fs.location.<code>.name` | — | Display name |
| `fs.location.<code>.path` | — | Absolute folder path |
| `fs.location.<code>.min.files` | `0` | Minimum expected file count |
| `fs.location.<code>.max.files` | `Integer.MAX_VALUE` | Maximum expected file count |
| `fs.location.<code>.file.types` | — | Comma-separated extensions (empty = all files) |
| `fs.location.<code>.recursive` | `false` | Scan sub-folders |
| `fs.location.<code>.ignore.zero.file.alert` | `false` | Suppress alert when count=0 |
| `fs.location.<code>.alert.too.few.subject` | `[%s] File Count Below Minimum - %s` | Email subject template |
| `fs.location.<code>.alert.too.many.subject` | `[%s] File Count Above Maximum - %s` | Email subject template |
| `fs.location.<code>.email.importance` | `Normal` | Email importance flag |

---

## 6. LogKeywordMonitor

**Package:** `logkeywordmonitoring`  
**Properties file:** `logkeywordmonitor.properties`  
**Default port:** 3023

### 6.1 Purpose

Incrementally scans one or more log files for configured keywords. Reads only newly written bytes since the last scan (position tracking), supporting both static file paths and wildcard patterns that match multiple files (e.g. daily-rotated logs). Sends an HTML email alert when new keyword matches are found.

### 6.2 Incremental Reading

The service tracks the last read **byte position** per file (by absolute path) in an in-memory `Map<String, Long>`. On each cycle:

1. Check current file size
2. If `currentSize < lastPosition` → file was rotated or truncated → reset position to 0, re-read from beginning
3. If `currentSize == lastPosition` → no new data → skip
4. Otherwise → open `FileInputStream`, `skip(lastPosition)`, wrap in `BufferedReader(UTF-8)`, read new lines to EOF
5. Record `newPosition = currentSize`

This approach correctly handles files written in any encoding (UTF-8) and avoids re-reading already-processed content across service restarts (positions are stored in memory — they reset on restart, causing a full re-scan of each file on first cycle).

### 6.3 Wildcard Pattern Support

If a configured path contains `*` or `?`, it is treated as a glob pattern. On each cycle `refreshMatchingFiles()` re-resolves the pattern to find any new files that have appeared (e.g. a new daily log file created at midnight). Each resolved file is tracked independently with its own byte position.

### 6.4 Keyword Matching Modes

| Mode | Behaviour |
|---|---|
| Literal (default) | `String.contains()` on each line |
| Case-insensitive | Both line and keyword lowercased before comparison |
| Regex | Pre-compiled `Pattern` matched against each line |

Keyword matching is configured per log file — different log files can use different modes.

### 6.5 Alert Behaviour

**Default (`alertOnFirstMatch=false`):** Every cycle that finds new keyword matches sends an email. The alert email contains an HTML table with file name, keyword, and new match count for all matches found in that cycle.

**`alertOnFirstMatch=true`:** Once a keyword in a given file has triggered an alert, it is added to an `alertedKeywords` set and suppressed in all subsequent cycles — even if new occurrences appear. This mode is suited to keywords that represent a known persistent condition where you only need to be notified once.

### 6.6 Keyword Colour Coding

Alert emails colour-code keywords by severity:

| Keyword contains | Badge colour |
|---|---|
| `fatal`, `outofmemory`, `stackoverflow` | Dark red |
| `error`, `severe` | Red |
| `exception` | Orange |
| `warn` | Yellow/orange |
| Anything else | Blue |

### 6.7 Prometheus Metrics

| Metric Name | Labels | Description |
|---|---|---|
| `log_keyword_total_matches` | `file`, `keyword` | Cumulative keyword matches since service start |
| `log_keyword_new_matches` | `file`, `keyword` | New matches found in the most recent cycle |
| `log_keyword_lines_scanned` | `file` | Cumulative lines scanned |
| `log_keyword_read_errors` | `file` | Cumulative read errors per file |

### 6.8 Configuration Reference — `logkeywordmonitor.properties`

| Property | Default | Description |
|---|---|---|
| `email.config.path` / `client.name` / `metrics.port` / `log.*` | (same pattern as other monitors) | |
| `check.interval.minutes` | — | Scan interval in minutes (preferred) |
| `monitor.interval.ms` | `60000` | Scan interval in ms (fallback) |
| `log.file.<code>.path` | — | Absolute path or glob pattern (e.g. `C:\Logs\app-*.log`) |
| `log.file.<code>.name` | — | Display name shown in alert emails |
| `log.file.<code>.keywords` | — | Comma-separated keywords to watch |
| `log.file.<code>.case.sensitive` | `false` | Case-sensitive keyword matching |
| `log.file.<code>.regex.mode` | `false` | Treat keywords as regex patterns |
| `log.file.<code>.alert.on.first.match` | `false` | Suppress repeat alerts for already-alerted keywords |

---

## 7. ServerUpTimeMonitor

**Package:** `serveruptime`  
**Properties file:** `serverinfo.properties`  
**Default port:** 3014

### 7.1 Purpose

Pings one or more servers at a configured interval and sends an email alert whenever a server transitions state — either going DOWN or coming back UP. Unlike the other monitors, this service does not use a breach window: it alerts on every state change.

### 7.2 How Ping Works

`InetAddress.getByName(server).isReachable(5000)` — a 5-second ICMP reachability check. Note: ICMP must be permitted through the Windows firewall on the target host. If ICMP is blocked, the server will always appear DOWN regardless of its actual state.

### 7.3 Alert Logic

On startup (`initializeStatus()`):
- Pings each server once
- Stores result in `currentStatus` (a `ConcurrentHashMap<String, Boolean>`)
- If any server is DOWN at startup, sends an immediate DOWN alert

On each scheduled cycle (`checkAndAlert()`):
- Pings each server
- Compares result to `currentStatus`
- If different → updates `currentStatus`, sends alert (`isNowUp=true` for recovery, `false` for new outage)
- If same → no email

```
Startup:  serverA=UP    → stored, no email
Cycle 1:  serverA=UP    → no change, no email
Cycle 2:  serverA=DOWN  → state change → send DOWN alert
Cycle 3:  serverA=DOWN  → no change, no email
Cycle 4:  serverA=UP    → state change → send UP (recovery) alert
```

### 7.4 Prometheus Metrics

| Metric Name | Labels | Description |
|---|---|---|
| `server_status` | `server` | 1 = reachable, 0 = unreachable |

### 7.5 Configuration Reference — `serverinfo.properties`

| Property | Default | Description |
|---|---|---|
| `servers.list` | — | **Required.** Comma-separated hostnames or IPs to monitor |
| `ping.interval.seconds` | `10` | How often to ping each server |
| `exporter.port` | `9091` | Prometheus HTTP port (installer default: 3014) |
| `monitor.host.name` | `Unknown Host` | Name of the monitoring machine shown in alert emails |
| `log.level` / `log.folder` / `log.retention.days` / `log.purge.interval.hours` | (same as other monitors) | |

---

## 8. Cross-Cutting Design Decisions

### 8.1 Why No Shared Base Class for EmailService

Each service has its own `EmailService.java`. This is intentional: each service is designed as an independent deployable microservice. A shared base class would create a compile-time dependency between services that are otherwise completely isolated JARs. If one service's email needs change (different attachment type, different template structure), the change is local to that service only.

### 8.2 Why ConcurrentHashMap for Shared State

The Prometheus metrics maps (`totalFileCounts`, `newFileCounts`) are accessed from both the scheduler thread (writes during monitoring cycle) and the HTTP server thread (reads when serving `/metrics`). `ConcurrentHashMap` provides lock-free reads, making metrics scraping non-blocking even during an active monitoring cycle.

### 8.3 Why Never `.clear()` on Shared Maps

The per-cycle new-count reset in WinFSCardinalityMonitor and WinFSErrorMonitor updates map values **in-place** rather than calling `.clear()`. Calling `.clear()` on a map shared with the Prometheus scrape thread would cause a window where the map is empty mid-cycle, producing incorrect zero values in scraped metrics. Updating in-place ensures the map always contains the last known valid value.

### 8.4 Why alert.window.size = 3

Three consecutive cycles is the minimum that meaningfully filters transient conditions without creating excessive alert delay. With a 5-minute poll interval this means 15 minutes of sustained breach before the first alert — acceptable for infrastructure monitoring where immediate response is not required for single-cycle spikes, but genuinely abnormal conditions need to be surfaced before they become critical.

### 8.5 State File Pattern (WinFSErrorMonitor, WinFSCardinalityMonitor)

Persisting processed file paths to disk means a service restart does not re-alert on files that were already reported. Without this, every service restart (including routine Windows Update reboots) would flood the operations team with alerts for all existing error files. The state file is append-only during normal operation and is only rebuilt from scratch if manually deleted.

---

## 9. Integration with the Broader Monitoring Stack

```
┌──────────────────────────────────────────────────────────────────┐
│                  Island Pacific Operations Monitor               │
│                                                                  │
│  Windows Agent (this suite)        IBM i Monitors               │
│  ┌─────────────────────┐           ┌─────────────────────┐      │
│  │ WinMonitor    :3022 │           │ IBMIFSError   :3010 │      │
│  │ WinFSError    :3020 │           │ IBMJobQue     :3011 │      │
│  │ WinFSCard     :3021 │           │ ...           :3012+│      │
│  │ LogKeyword    :3023 │           └─────────────────────┘      │
│  │ ServerUpTime  :3014 │                                        │
│  └─────────────────────┘                                        │
│            │  /metrics (Prometheus text format)                  │
│            ▼                                                     │
│  ┌─────────────────────┐                                        │
│  │    Prometheus       │  ← scrapes all :30xx/metrics endpoints  │
│  └──────────┬──────────┘                                        │
│             │                                                    │
│  ┌──────────▼──────────┐    ┌──────────────────────┐           │
│  │       Grafana       │    │    Loki / Promtail    │           │
│  │   (dashboards)      │    │  (log aggregation)   │           │
│  └─────────────────────┘    └──────────────────────┘           │
└──────────────────────────────────────────────────────────────────┘
```

Each service is installed as a Windows service via **WinSW** (Windows Service Wrapper), allowing it to start automatically on system boot, restart on failure, and be managed via standard Windows service tools (`sc.exe`, Services MMC).

The installer (`IslandPacificMonitoringSetup.exe`) manages all five services as part of the WinAgent role. The MonitoringServer role installs Prometheus, Grafana, and Loki on a separate host that scrapes the agent.

---

## 10. Security Considerations

| Concern | Mitigation |
|---|---|
| WinRM credentials in config | Stored in properties file; access restricted by OS file permissions on the install directory (`C:\Program Files\Island Pacific\...`) |
| WinRM password in process arguments | Passed as a `PSCredential` constructed from an OS environment variable — not visible in process listings or audit logs |
| OAuth2 client secret | Stored in `email.properties`; access restricted by OS permissions |
| OAuth2 bearer token | Held in memory only; never written to disk, never logged |
| Service name injection | `sc query` calls pass the service name as a discrete argument array element, not as a concatenated shell string — no command injection possible |
| State file contents | Contain only absolute file paths — no credentials or sensitive data |
