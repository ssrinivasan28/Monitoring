# Island Pacific Windows Monitoring Suite — Functional Specification

**Product:** Island Pacific Operations Monitor — Windows Agent  
**Delivery:** `IslandPacificMonitoringSetup.exe`  
**Version:** 1.0  
**Author:** Island Pacific Retail Systems  
**Audience:** Operations teams, IT managers, site administrators

---

## 1. What Is This Product?

The Island Pacific Windows Monitoring Suite is a set of monitoring agents installed on your Windows server environment. Once installed, the agents run silently in the background as Windows services, continuously watching your systems and sending email alerts when something needs attention.

There are five monitoring agents in this suite, each focused on a different area of your Windows infrastructure:

| Agent | What It Watches |
|---|---|
| **WinMonitor** | CPU, memory, disk usage and Windows services across one or more servers |
| **WinFSErrorMonitor** | Folders that should not contain error files — alerts the moment a new one appears |
| **WinFSCardinalityMonitor** | Folders where the number of files should stay within expected limits |
| **LogKeywordMonitor** | Application log files — watches for error keywords written into them |
| **ServerUpTimeMonitor** | Whether your servers are reachable on the network |

Each agent sends email alerts directly to your configured recipients and also publishes live metrics to a Grafana dashboard (if the monitoring server role is installed).

---

## 2. WinMonitor — Server Health Monitor

### What It Does

WinMonitor continuously checks the health of your Windows servers. It monitors CPU usage, memory usage, disk space, and the running state of critical Windows services. If any of these metrics stays in an unhealthy state for a sustained period, it sends you an email alert.

It can monitor your local server and any number of remote Windows servers from a single installation.

### What Triggers an Alert

WinMonitor is designed to avoid false alarms from brief spikes. A threshold must be breached for **three consecutive check cycles** before an alert is sent. With the default 5-minute check interval, this means a real sustained problem triggers an alert after approximately 15 minutes.

| Condition | Alert Sent When |
|---|---|
| CPU usage is too high | CPU stays above the configured threshold for 3 consecutive checks |
| Memory usage is too high | Memory stays above the configured threshold for 3 consecutive checks |
| Disk space is running low | Disk utilisation stays above the threshold for 3 consecutive checks |
| Disk filling up rapidly | A drive grows by more than 5% in a single check cycle, sustained for 3 cycles |
| A Windows service is stopped | A configured service stays non-Running for 3 consecutive checks |
| A monitoring error occurs | Immediately — no delay |

Once the condition recovers (e.g. CPU drops back below threshold), the counter resets. If the problem returns, the three-cycle window starts again from the beginning.

### What the Alert Email Contains

**For CPU/Memory/Disk alerts:**
- Which server is affected
- A table of each breached metric with its current value and configured threshold
- Any Windows services that are currently stopped or not running
- The top processes by CPU usage at the time of the alert
- How long the server has been running since last reboot

**For a service stopped alert:**
- Which server and which service
- The current state of the service (Stopped, Paused, Start Pending, etc.)

**For a disk growth alert:**
- Which drive is growing rapidly
- The growth percentage observed

### Recovery Notification

When a breached metric returns to a healthy state, WinMonitor resets its internal counter silently — it does not send a "recovered" email. The next alert will only fire if the condition is sustained again for three consecutive cycles.

### What You Configure

- The list of servers to monitor (local and/or remote)
- Remote server credentials (username and password) if monitoring remote machines
- CPU, memory, and disk threshold percentages (default: 90%)
- How often to check (default: every 5 minutes)
- Which Windows services to watch
- How many top processes to include in alert emails (default: 5)
- Email recipients and delivery method (SMTP or Microsoft 365)

---

## 3. WinFSErrorMonitor — Error File Monitor

### What It Does

WinFSErrorMonitor watches folders on your Windows server for the appearance of error files — files with extensions that indicate a problem, such as `.err`, `.wrn`, or `.dmp`. The moment a new error file appears in a monitored folder, an alert email is sent.

This monitor is designed for situations where your business applications write error files to a specific folder when something goes wrong. Instead of manually checking those folders, WinFSErrorMonitor watches them for you and notifies your team immediately.

### What Triggers an Alert

An alert is sent **immediately** the first time a new file (matching the configured extension) is detected in a monitored folder. There is no delay and no consecutive-cycle window — the philosophy is that an error file appearing at all is significant and should be reported straight away.

Each file is only ever reported once. If the same error file is still there on the next check cycle, no second alert is sent. Only genuinely new files trigger new alerts.

This behaviour persists across service restarts: the list of already-reported files is saved to disk, so rebooting the server or restarting the monitoring service will not flood your inbox with re-alerts for existing files.

### What the Alert Email Contains

- Which monitored location the file appeared in
- The folder path being watched
- The name of the new file (or files, if multiple appeared since the last check)
- The file extension type detected
- Timestamp of detection

### What You Configure

- One or more folder paths to watch
- For each folder: the file extensions that indicate an error (e.g. `err, wrn, dmp`)
- How often to scan (default: every 5 minutes)
- Email recipients — can be configured per folder if different locations should alert different teams

---

## 4. WinFSCardinalityMonitor — File Count Monitor

### What It Does

WinFSCardinalityMonitor watches folders on your Windows server and checks that the number of files inside stays within expected limits — a configured minimum and maximum. If the file count falls below the minimum or rises above the maximum for a sustained period, an alert is sent.

This monitor is useful for situations such as:
- An incoming order folder that should always contain between 0 and 500 files — more than 500 might indicate a processing backlog
- A staging folder that should never be empty during business hours — zero files might mean a feed has stopped
- An archive folder where file count should only ever grow — a sudden drop might mean files were accidentally deleted

### What Triggers an Alert

Like WinMonitor, this monitor uses a **three consecutive cycle** window before sending an alert, to avoid noise from momentary fluctuations.

| Condition | Alert Sent When |
|---|---|
| Too few files | File count stays below the minimum for 3 consecutive checks |
| Too many files | File count stays above the maximum for 3 consecutive checks |

Once the count returns to within the expected range, the counter resets silently.

**Special case — Zero file suppression:** For folders where zero files is a legitimate state at certain times (e.g. overnight when no orders are flowing), you can enable the "ignore zero file alert" option per folder. When enabled, the too-few alert is suppressed specifically when the count is exactly zero. It will still fire if the count is between one and the minimum threshold.

### New File Tracking

In addition to threshold alerting, the monitor tracks which individual files have appeared since the last check cycle. This is published as a metric (new files detected per cycle) and can be viewed in the Grafana dashboard to observe file arrival rates over time.

### What the Alert Email Contains

- Which monitored location is affected
- The folder path being watched
- The current file count
- The configured minimum or maximum threshold that was breached
- The name of the monitoring server

### What You Configure

- One or more folder paths to watch
- For each folder: minimum and maximum acceptable file counts
- For each folder: whether to count all files or only specific file types (e.g. only `.xml` files)
- For each folder: whether to scan sub-folders recursively or only the top level
- For each folder: whether to suppress alerts when the count is exactly zero
- How often to scan (default: every 5 minutes)
- Email recipients and importance level per folder

---

## 5. LogKeywordMonitor — Log File Keyword Monitor

### What It Does

LogKeywordMonitor reads your application log files and watches for specific words or phrases being written into them. When a configured keyword appears in a log file, an alert email is sent summarising what was found.

This monitor is designed for operations teams who need to know when application errors are being logged, without having to manually read through log files. It supports watching multiple log files simultaneously, each with its own list of keywords to look for.

### How It Reads Log Files

The monitor reads log files **incrementally** — on each check cycle it reads only the new content written since the last check. It remembers exactly where it left off in each file (by byte position), so it never re-reads content it has already processed. This means it works correctly even on high-volume log files that are being written to continuously.

It also handles **log file rotation**: if your application creates a new log file each day (e.g. `app-2024-01-01.log`, `app-2024-01-02.log`), you can configure a wildcard pattern (e.g. `app-*.log`) and the monitor will automatically detect and begin watching each new log file as it appears.

If a log file is truncated or replaced (file rotation that overwrites rather than renames), the monitor detects this automatically and re-reads the file from the beginning.

### What Triggers an Alert

An alert email is sent when one or more configured keywords are found in new log content during a check cycle. All matches from that cycle are consolidated into a single email — you will not receive one email per keyword or one email per line.

**Alert on first match only (optional):** If you configure a keyword with "alert on first match" mode, the monitor will send one alert the first time that keyword appears in a given log file and then suppress further alerts for the same keyword. This is useful for keywords that indicate a known persistent condition where you only need initial notification.

By default (without this option), every check cycle that finds new keyword matches sends a fresh alert.

### What the Alert Email Contains

- A summary table listing each log file and keyword where matches were found
- The number of new matches found in that cycle
- Keywords are colour-coded by severity:
  - **Dark red** — `FATAL`, `OutOfMemory`, `StackOverflow`
  - **Red** — `ERROR`, `SEVERE`
  - **Orange** — `Exception`
  - **Yellow/Orange** — `WARN`
  - **Blue** — all other keywords

### What You Configure

- One or more log file paths to watch (can use wildcards for rotated logs, e.g. `C:\Logs\app-*.log`)
- For each log file: the list of keywords or phrases to watch for
- For each log file: whether matching is case-sensitive (default: case-insensitive)
- For each log file: whether keywords are plain text (default) or regular expressions
- For each log file: whether to alert only on the first occurrence of each keyword
- How often to check (default: every 1 minute)
- Email recipients

---

## 6. ServerUpTimeMonitor — Server Availability Monitor

### What It Does

ServerUpTimeMonitor periodically pings one or more servers to check whether they are reachable on the network. When a server goes down, an alert is sent immediately. When that server comes back up, a recovery alert is sent.

This monitor provides early warning of server outages so your team can respond before end users are affected.

### What Triggers an Alert

Unlike other monitors in this suite, ServerUpTimeMonitor does **not** use a consecutive-cycle window. A state change triggers an alert immediately:

| Event | Alert Sent |
|---|---|
| Server becomes unreachable | Immediately — DOWN alert |
| Server becomes reachable again | Immediately — UP (recovery) alert |
| Server is still unreachable on next check | No further alert — only one DOWN alert per outage |
| Server is still reachable on next check | No alert |

On service startup, each server is pinged once. If any server is already down at startup, an immediate DOWN alert is sent.

### What the Alert Email Contains

**DOWN alert:**
- Which server is unreachable
- The time the outage was detected
- The name of the monitoring host

**UP (recovery) alert:**
- Which server has come back online
- The time the recovery was detected

### Important Note on Ping Behaviour

The ping check uses ICMP (the same protocol as the Windows `ping` command). For this to work, ICMP must be permitted through the Windows Firewall on the target server. If your network blocks ICMP, the server will always appear as DOWN regardless of its actual availability. In this case, coordinate with your network team to allow ICMP from the monitoring server, or use WinMonitor's remote collection capability as an alternative availability check.

### What You Configure

- The list of server hostnames or IP addresses to monitor
- How often to ping each server (default: every 10 seconds)
- The name of the monitoring host (shown in alert emails)
- Email recipients and delivery method

---

## 7. Alert Email Delivery

All five monitors use the same email delivery configuration. Two delivery methods are supported:

### SMTP

Standard email sending via an SMTP mail server. Suitable for:
- Internal mail relays within your network
- Any standard SMTP service

Supports plain (unauthenticated), authenticated (username and password), and STARTTLS connections.

### Microsoft 365 (OAuth2)

For organisations using Microsoft 365. The monitoring suite authenticates with your Azure Active Directory as an application (no user account or password required) and sends email via the Microsoft Graph API. This method:
- Does not require storing a user password
- Works even when multi-factor authentication is enforced for user accounts
- Sends email on behalf of a configured mailbox (e.g. `monitoring@yourcompany.com`)

### Email Format

All alert emails share a consistent HTML format:
- Island Pacific branding with logo
- Colour-coded alert badge (red for alerts, green for recovery)
- Details table relevant to the specific alert type
- Timestamp and monitoring host name in the footer

---

## 8. Installation and Operation

### What the Installer Does

The `IslandPacificMonitoringSetup.exe` installer:

1. Installs the selected monitoring agents as Windows services that start automatically on boot
2. Creates default configuration files for each agent (these are never overwritten by future upgrades — your settings are preserved)
3. Optionally installs the Prometheus + Grafana monitoring server stack (if this machine is the designated monitoring server)
4. Assigns a unique port to each agent for Prometheus metrics collection

### Multi-Client Support

If you need to monitor multiple independent client environments from the same monitoring server, the installer supports a client instance ID. Each client installation uses a unique port range offset (e.g. client 1 on ports 3010–3025, client 2 on ports 3110–3125) so all agents can run simultaneously without conflict. Each client's services and configuration files are stored in their own named directory.

### Configuration Files

Each monitoring agent reads two configuration files:

- **`email.properties`** — shared across all agents, controls how alerts are delivered (email server, recipients, authentication)
- **`<agentname>.properties`** — specific to each agent, controls what is monitored (folders, servers, keywords, thresholds)

Both files are plain text and can be edited with any text editor. Changes take effect on the next service restart.

### Checking Agent Status

Each agent's current status can be verified using the Windows Services panel (`services.msc`) or via the command line:

```
sc query IPMonitoring_WinMonitor
sc query IPMonitoring_WinFSErrorMonitor
sc query IPMonitoring_WinFSCardinalityMonitor
sc query IPMonitoring_LogKeywordMonitor
sc query IPMonitoring_ServerUpTimeMonitor
```

Live metrics for each agent are available at:

```
http://localhost:3022/metrics   (WinMonitor)
http://localhost:3020/metrics   (WinFSErrorMonitor)
http://localhost:3021/metrics   (WinFSCardinalityMonitor)
http://localhost:3023/metrics   (LogKeywordMonitor)
http://localhost:3014/metrics   (ServerUpTimeMonitor)
```

### Log Files

Each agent writes daily log files to its configured log folder (default: `logs\` within the agent's installation directory). Log files are named `<agentname>_YYYY-MM-DD.log` and are automatically deleted after the configured retention period (default: 30 days).

---

## 9. Summary — At a Glance

| Monitor | Watches | Alert Trigger | Alert Timing |
|---|---|---|---|
| WinMonitor | CPU, memory, disk, services | Threshold breached | After 3 consecutive breaches (~15 min at default interval) |
| WinFSErrorMonitor | Error files appearing in folders | New file with matching extension | Immediately |
| WinFSCardinalityMonitor | File count in folders vs min/max | Count outside expected range | After 3 consecutive breaches |
| LogKeywordMonitor | Keywords written into log files | Keyword found in new log content | Immediately (each cycle with matches) |
| ServerUpTimeMonitor | Server reachability (ping) | Server goes DOWN or comes back UP | Immediately on state change |
