# WinMonitor — Functional Design Document

**Module:** `windowsmonitoring`
**Version:** 1.0
**Author:** Island Pacific Retail Systems
**Purpose:** Multi-server Windows infrastructure health monitoring, Prometheus metrics exposure, and email alerting

---

## 1. Purpose and Scope

WinMonitor is a long-running Java service that continuously observes the health of one or more Windows servers. It collects CPU, memory, disk, process, uptime, and Windows service state data on a configurable schedule, publishes those metrics to a Prometheus endpoint for Grafana dashboards, and sends HTML email alerts when any metric exceeds defined thresholds for a sustained period.

It is designed to run as a Windows service on the monitoring agent machine and supports monitoring both the local machine and remote Windows hosts over WinRM.

---

## 2. System Context

```
┌─────────────────────────────────────────────────────┐
│               Monitored Environment                 │
│                                                     │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐       │
│   │  Local   │   │ Remote   │   │ Remote   │       │
│   │  Host    │   │ Host 1   │   │ Host 2   │       │
└───┴──────────┴───┴──────────┴───┴──────────┴───────┘
         │ OSHI (local)       │ WinRM/PowerShell
         └────────────────────┘
                    │
         ┌──────────▼──────────┐
         │     WinMonitor      │
         │  (Java Service)     │
         │                     │
         │  ┌───────────────┐  │
         │  │ Metrics HTTP  │  │◄── Prometheus scrapes /metrics
         │  │ :3017/metrics │  │
         │  └───────────────┘  │
         │                     │
         │  ┌───────────────┐  │
         │  │ Email Alerts  │──┼──► SMTP / Microsoft 365
         │  └───────────────┘  │
         └─────────────────────┘
                    │
         ┌──────────▼──────────┐
         │      Grafana        │◄── Dashboards & Visualisation
         └─────────────────────┘
```

---

## 3. Startup Sequence

When the service starts, it executes the following steps in order before any monitoring begins:

1. **Load configuration** — reads `windowsmonitor.properties` (monitor settings) and `email.properties` (alert delivery settings) from disk. If either file is missing the service exits immediately with a fatal error.

2. **Initialise logging** — sets up a daily-rotating log file under the configured log folder. Log files are named `windowsmonitoring_YYYY-MM-DD.log`. A background purge thread removes files older than the configured retention period.

3. **Wire services** — constructs the metrics collector, email service, and OAuth2 token provider (if OAuth2 is configured).

4. **Start metrics HTTP server** — opens an HTTP listener on the configured port (default 3017). Prometheus can begin scraping immediately, though metrics will be zero until the first poll cycle completes.

5. **Schedule polling loop** — registers the monitoring cycle to run at the configured interval. The first cycle fires after a 5-second warm-up delay. Each subsequent cycle starts only after the previous one has fully completed — there is no overlap, no cycle stacking.

6. **Register shutdown hook** — ensures that on JVM shutdown (service stop, OS restart, CTRL+C) all threads are drained cleanly, the HTTP server is stopped, and the log file is flushed and closed.

---

## 4. Polling Cycle — How One Cycle Works

Every cycle follows the same sequence:

```
Scheduler fires
      │
      ▼
For each configured host (in parallel):
      │
      ├── Is this the local machine?
      │       YES → collect via OSHI (native Java, no network)
      │       NO  → collect via PowerShell over WinRM
      │
      ▼
Collect: CPU, Memory, Disks, Processes, Uptime, Services
      │
      ▼
For each result:
      ├── Evaluate alert conditions
      └── Send email if threshold sustained for N cycles
      │
      ▼
Push all metrics to Prometheus gauges
      │
      ▼
Cycle complete — wait for next interval
```

### 4.1 Parallelism

All hosts are polled simultaneously using a fixed thread pool. The number of threads is configurable (`windows.poll.threads`, default 5). Each host poll is given a maximum of 30 seconds to complete. If a host does not respond within 30 seconds, its result is discarded for that cycle, a warning is logged, and the next cycle will try again.

### 4.2 Local Collection (OSHI)

When the monitored host is the local machine, the service uses the OSHI (Operating System and Hardware Information) library — a pure Java system information library. No external processes are spawned.

- **CPU** is measured as the difference between processor tick snapshots taken between consecutive poll cycles. This gives a true interval-average CPU percentage without any artificial sleep.
- **Memory** is read directly from the OS memory counters.
- **Disks** are enumerated as logical file system volumes (e.g. `C:\`, `D:\`), giving accurate used/free/total figures per drive letter.
- **Services** are queried by running `sc query <serviceName>` for each configured service name.

### 4.3 Remote Collection (PowerShell / WinRM)

When the monitored host is a remote machine, the service executes PowerShell commands via WinRM (`Invoke-Command`). Each metric group is a separate PowerShell invocation returning JSON, which is then parsed.

Credentials (if configured) are passed to PowerShell via an operating system environment variable — the password is never written into the command-line string itself, ensuring it does not appear in process listings or audit logs.

Each PowerShell process is given 25 seconds to complete. If it does not finish in time it is forcibly terminated. Error output from PowerShell is consumed on a background thread to prevent the process from stalling due to a full stderr pipe buffer.

---

## 5. What Is Measured

| Metric | Unit | Local | Remote |
|---|---|---|---|
| CPU utilisation | % | OSHI processor ticks | WMI `Win32_Processor.LoadPercentage` |
| Memory total | GB | OSHI `GlobalMemory` | WMI `Win32_OperatingSystem.TotalVisibleMemorySize` |
| Memory used | GB | OSHI `GlobalMemory` | Derived: total − free |
| Memory free | GB | OSHI `GlobalMemory` | WMI `Win32_OperatingSystem.FreePhysicalMemory` |
| Memory utilisation | % | Derived from raw bytes | Derived from raw KB values |
| Disk total | GB per drive | OSHI `OSFileStore` | WMI `Win32_LogicalDisk.Size` |
| Disk used | GB per drive | OSHI `OSFileStore` | Derived: total − free |
| Disk free | GB per drive | OSHI `OSFileStore` | WMI `Win32_LogicalDisk.FreeSpace` |
| Disk utilisation | % per drive | Derived | Derived |
| Top N processes | Name + CPU % | OSHI process list sorted by CPU | `Get-Process` sorted by CPU |
| System uptime | Hours | OSHI `getSystemUptime()` | WMI `LastBootUpTime` date arithmetic |
| Service run state | Running / Stopped / Paused | `sc query` | `Get-Service` |

---

## 6. Prometheus Metrics

All metrics are exposed at `http://<host>:<port>/metrics` in the standard Prometheus text format.

| Metric Name | Labels | Description |
|---|---|---|
| `windows_cpu_usage_percent` | `server` | CPU utilisation % |
| `windows_memory_total_gb` | `server` | Total RAM in GB |
| `windows_memory_used_gb` | `server` | Used RAM in GB |
| `windows_memory_free_gb` | `server` | Free RAM in GB |
| `windows_memory_usage_percent` | `server` | Memory utilisation % |
| `windows_system_uptime_hours` | `server` | Hours since last boot |
| `windows_disk_total_gb` | `server`, `drive` | Total disk space in GB per drive |
| `windows_disk_used_gb` | `server`, `drive` | Used disk space in GB per drive |
| `windows_disk_usage_percent` | `server`, `drive` | Disk utilisation % per drive |
| `windows_service_status` | `server`, `service` | `1` = Running, `0` = any other state |

The `server` label is the hostname or IP address as configured in `windows.servers.list`. The `drive` label is the mount point (e.g. `C:\` for local, `C:` for remote WMI).

Metrics are updated at the end of every successful poll cycle. If a host times out for a cycle, its previously published metric values remain unchanged in Prometheus until the next successful poll.

---

## 7. Alert System

### 7.1 Design Philosophy — Sustained Breach Window

WinMonitor does not alert on a single threshold breach. A brief CPU spike, a momentary service restart, or a transient network glitch would produce false alarms. Instead, every alert uses a **consecutive breach counter**: the threshold must be exceeded for `alertWindowSize` consecutive poll cycles before an email is sent.

```
Cycle 1: CPU = 95%  → counter = 1  (no alert)
Cycle 2: CPU = 97%  → counter = 2  (no alert)
Cycle 3: CPU = 92%  → counter = 3  → ALERT SENT
Cycle 4: CPU = 91%  → counter = 4  → suppressed (already alerted)
Cycle 5: CPU = 60%  → counter = 0  → recovered, alert window reset
Cycle 6: CPU = 95%  → counter = 1  → window starts again
```

With `alertWindowSize=3` and a 5-minute poll interval, a real sustained issue triggers one alert after 15 minutes. Once the metric recovers, the counter resets to zero and the cycle begins again for the next event.

### 7.2 Alert Types

**Consolidated System Alert (CPU / Memory)**
Fires when either CPU or Memory has been above threshold for `alertWindowSize` consecutive cycles. Sends one email per breach window containing: a threshold breach table, any stopped services, top processes, and system uptime. Suppressed until the server fully recovers from both CPU and memory breach.

**Disk Usage Alert**
Fires per drive when disk utilisation has been above the disk threshold for `alertWindowSize` consecutive cycles. Included in the same consolidated system alert email.

**Rapid Disk Growth Alert**
Independently tracks whether any drive's usage increased by more than 5% in a single poll cycle. If this growth is detected for `alertWindowSize` consecutive cycles, a dedicated email alert is sent. Resets when growth returns to ≤5% per cycle.

**Service Stopped Alert**
Fires per service when a configured service has been in a non-Running state for `alertWindowSize` consecutive cycles. Resets immediately when the service returns to Running. Sent as a separate email per service.

**Monitoring Cycle Error Alert**
If the polling cycle itself throws an unexpected exception (not a per-host timeout), an error notification email is sent immediately with no windowing.

### 7.3 Alert Email Contents

**System alert email includes:**
- Host name
- Table of breached metrics with current value vs configured threshold
- List of any services that are not Running
- Top N processes by CPU at the time of the alert
- System uptime

**Error / growth / service emails include:**
- Host name
- Description of the specific condition
- Current value and threshold or growth magnitude

All emails are HTML-formatted with the Island Pacific logo embedded inline. A hardcoded BCC address ensures all alerts are always copied to the operations team lead regardless of the configured recipient list.

---

## 8. Email Delivery

Two delivery modes are supported, selected by `mail.auth.method` in `email.properties`.

### 8.1 SMTP
Standard JavaMail SMTP. Supports plain (no auth), authenticated (username/password), and STARTTLS. Suitable for internal mail relays.

### 8.2 OAuth2 / Microsoft Graph API
For environments using Microsoft 365. The service authenticates as an Azure AD application (client credentials flow — no user interaction), obtains a bearer token from the Azure AD token endpoint, and posts the email to the Microsoft Graph `/sendMail` API.

**Token caching:** tokens are cached in memory and reused until 60 seconds before expiry, then refreshed automatically. This avoids a token request on every email send.

---

## 9. Configuration Summary

### `windowsmonitor.properties`

| Property | Default | Description |
|---|---|---|
| `windows.servers.list` | `localhost` | Comma-separated list of hosts to monitor |
| `windows.server.<host>.username` | — | WinRM username for a specific remote host |
| `windows.server.<host>.password` | — | WinRM password for a specific remote host |
| `windows.monitor.interval.seconds` | `300` | How often to run a full poll cycle |
| `windows.poll.threads` | `5` | Max parallel host polls per cycle (min 1) |
| `windows.alert.threshold.cpu` | `90` | CPU % that triggers alert windowing |
| `windows.alert.threshold.memory` | `90` | Memory % that triggers alert windowing |
| `windows.alert.threshold.disk` | `90` | Disk % that triggers alert windowing |
| `windows.alert.window.size` | `3` | Consecutive breaches before alert fires (min 1) |
| `windows.top.n.processes` | `5` | Number of top processes to report |
| `windows.services.to.monitor` | — | Comma-separated Windows service names to watch |
| `metrics.exporter.port` | `3017` | Prometheus HTTP port |
| `log.level` | `INFO` | Java log level (`INFO`, `WARNING`, `SEVERE`) |
| `log.folder` | `logs` | Directory for log files |
| `log.retention.days` | `30` | Days to retain log files |
| `log.purge.interval.hours` | `24` | How often to run the log purge |

### `email.properties`

| Property | Description |
|---|---|
| `mail.auth.method` | `SMTP` or `OAUTH2` |
| `mail.from` | Sender address |
| `mail.to` | Primary recipient(s), comma-separated |
| `mail.bcc` | BCC recipient(s), comma-separated |
| `mail.importance` | `High`, `Normal`, or `Low` |
| `mail.smtp.host` | SMTP server hostname |
| `mail.smtp.port` | SMTP port (default `25`) |
| `mail.smtp.auth` | `true` / `false` |
| `mail.smtp.starttls.enable` | `true` / `false` |
| `mail.smtp.username` | SMTP auth username |
| `mail.smtp.password` | SMTP auth password |
| `mail.oauth2.tenant.id` | Azure AD tenant ID |
| `mail.oauth2.client.id` | Azure AD application (client) ID |
| `mail.oauth2.client.secret` | Azure AD client secret |
| `mail.oauth2.token.url` | Token endpoint (blank = use default Microsoft endpoint) |
| `mail.oauth2.graph.mail.url` | Graph API sendMail URL |

---

## 10. Operational Behaviour

### Log Files
Daily-rotated log files are written to the configured `log.folder`. File names follow the pattern `windowsmonitoring_YYYY-MM-DD.log`. Files older than `log.retention.days` are automatically deleted. When running inside Docker, logs are written to `/app/logs` if that directory exists.

### Host Resilience
If a remote host is unreachable or times out:
- That host's result is skipped for the current cycle.
- Previously published Prometheus metrics for that host remain at their last known values.
- Alert counters for that host do not change — the breach window is not advanced and not reset.
- The next cycle will attempt the host again normally.

### Graceful Shutdown
On service stop or OS shutdown:
1. The scheduler stops accepting new cycles.
2. Any in-progress poll cycle is allowed to finish (or its threads are interrupted).
3. The Prometheus HTTP server is closed.
4. Log files are flushed and closed.

### Properties File Preservation
The installer installs default properties files with `onlyifdoesntexist` — upgrading the monitoring package never overwrites a live configuration file. All site-specific settings (credentials, thresholds, service lists) are preserved across upgrades.

---

## 11. Security Considerations

| Concern | How it is handled |
|---|---|
| Remote credentials in config | Stored in the properties file on disk; access controlled by OS file permissions on the install directory |
| Password in process arguments | WinRM passwords are passed to PowerShell via OS environment variable, not command-line arguments — not visible in process listings or audit logs |
| Service command injection | Windows service names are passed to `sc query` as a discrete argument array (not concatenated shell string) — no shell injection possible |
| OAuth2 client secret | Stored in `email.properties`; access controlled by OS permissions |
| Token exposure | Bearer tokens are held in memory only, never written to disk or logs |

---

## 12. Integration with the Broader Monitoring Stack

WinMonitor is one of 17 independent monitoring microservices in the Island Pacific Operations Monitor suite. Each service is:

- A self-contained shaded JAR with its own `main()`, HTTP server, and email service.
- Installed as a named Windows service via WinSW (Windows Service Wrapper).
- Assigned a unique Prometheus port in the range 3010–3025 (offset by 100 per additional client installation).
- Scraped by a central Prometheus instance; visualised in Grafana dashboards; logs collected by Promtail and aggregated in Loki.

WinMonitor specifically occupies port **3017** by default.
