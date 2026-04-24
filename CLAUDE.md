# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Behavioral Guidelines

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

## Build Commands

```powershell
# Build all 16 monitoring JARs
mvn clean package

# Build and create Windows installer (runs mvn + Inno Setup)
.\build-installer.ps1

# Build without rerunning Maven (just recompile installer)
.\build-installer.ps1 -SkipMaven

# Build without validating external binaries (Prometheus, Grafana, etc.)
.\build-installer.ps1 -SkipValidation

# Clean build (removes target/ and output directories first)
.\build-installer.ps1 -Clean
```

Built JARs land in `target/monitors/*.jar`. There are no automated tests — testing is manual (`curl http://localhost:<port>/metrics` after running a JAR).

ProGuard runs as part of the `package` phase; there is no way to skip it per-JAR without editing `pom.xml`.

## Architecture

### Single-Module, Multi-Output Build

This is **not** a multi-module Maven project. It is one Maven module (`src/main/java/com/islandpacific/monitoring/`) that produces 16 independent shaded JARs via the Maven Shade plugin. Each `<execution>` block in `pom.xml` specifies a `mainClass` to produce a different self-contained executable JAR.

**Build pipeline order within `package` phase:**
1. **Maven Antrun** — pre-copies JAR to work around a Windows file-rename limitation with ProGuard
2. **ProGuard** (v7.3.2) — obfuscates the compiled classes; config lives in `proguard.conf` at the repo root
3. **Maven Shade** (v3.6.0, 17 executions) — produces fat JARs in `target/monitors/`, each with a `Main-Class` manifest entry

**Key runtime dependencies:**
- `jt400` 21.0.4 — IBM i connectivity (AS400 API + JDBC)
- `smbj` 0.14.0 — SMB/CIFS for IFS folder access
- `simpleclient*` 0.16.0 — Prometheus Java client
- `oshi-core` 6.4.10 — Windows CPU/memory/disk metrics (WinMonitor only)
- `gson` 2.10.1 — JSON for OAuth2 token parsing
- `javax.mail` 1.6.2 — SMTP email
- `selenium-java` 4.21.0 + `webdrivermanager` 5.8.0 — Browser screenshot capture (ServiceScheduler only)

Java 17 (compiler source/target).

### 16 Monitoring Services

| JAR Name | Package | What it monitors |
|---|---|---|
| IBMIFSErrorMonitor | `ibmiifsmonitoring` | IBM i IFS folder file counts via SMB |
| IBMRealTimeIFSMonitor | `filesystemerrormonitoring` | IFS error detection |
| IBMJobQueCountMonitor | `ibmjobquecountmonitoring` | IBM i job queue waiting-job counts |
| IBMJobQueStatusMonitor | `ibmjobquestatusmonitoring` | IBM i job queue status changes |
| IBMSubSystemMonitor | `ibmsubsystemmonitoring` | IBM i subsystem status |
| IBMMatrixMonitor | `ibmssystemmatrix` | IBM i system matrix/performance |
| IBMQSYSOPRMonitor | `ibmqsysoprmonitoring` | QSYSOPR operator message queue |
| IBMFileMemberMonitor | `ibmifilemembermonitor` | IBM i physical file member record counts |
| IBMJobDurationMonitor | `ibmjobdurationmonitoring` | IBM i job run duration tracking |
| IBMNetworkEnabler | `ibmnetworkenabler` | IBM i network connectivity |
| IBMUserProfileChecker | `ibmuserprofilechecker` | IBM i user profile validation |
| ServerUpTimeMonitor | `serveruptime` | Server ping/uptime |
| WinMonitor | `winmonitor` | Windows CPU, memory, disk, processes |
| WinFSErrorMonitor | `filesystemerrormonitoring` (Win variant) | Windows file system error detection |
| WinFSCardinalityMonitor | `filesystemcardinalitymonitoring` | Windows folder file-count thresholds |
| LogKeywordMonitor | `logkeywordmonitoring` | Log file keyword scanning |
| WinServiceMonitor | `winservicemonitor` | Windows service run-state monitoring |
| ServiceScheduler | `servicescheduler` | Scheduled job runner: URL polling, PowerShell scripts, Selenium screenshots, email alerts |

### Per-Monitor Package Structure

Every monitor package follows the same internal structure:

- **`Main*.java`** — Entry point: loads properties, wires services, starts scheduler + metrics HTTP server + shutdown hook
- **`*Config.java`** — Parses `.properties` files into typed config objects; throws `IllegalArgumentException` on missing required fields
- **`*Service.java`** — Core business logic (polls IBM i, walks folders, scans logs, etc.)
- **`*Metrics.java` / `*MetricsExporter.java`** — Prometheus metrics (static Gauge/Counter fields do the real work; constructor params are wiring only)
- **`*AppServer.java` / `MetricsServer.java`** — Tiny `com.sun.net.httpserver.HttpServer` on a configured port serving `/metrics`
- **`EmailService.java`** — Sends alerts via SMTP or OAuth2 (Microsoft Graph API). **Keep each module's EmailService separate — do not consolidate into a shared base class.** Each monitor is an independent microservice.
- **`OAuth2TokenProvider.java`** — Fetches Azure AD bearer tokens; caches them with a 5-minute expiry buffer; `getAccessToken()` is `synchronized`

### Threading Model

All monitors use `ScheduledExecutorService.newSingleThreadScheduledExecutor()` for the monitoring loop — single-threaded by design. The metrics HTTP server also uses a single-threaded executor. Scheduler threads are daemon threads so they don't block JVM shutdown. Shutdown hooks call `awaitTermination()` with a timeout, falling back to `shutdownNow()`.

### Shared Common Code

`com.islandpacific.monitoring.common`:
- `AppLogger` — Configures a single static logger per JVM. Call `AppLogger.setupLogger(moduleName, logLevel, logFolder)` then `AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours)` at startup. Produces daily-rotated files `{moduleName}_{YYYY-MM-DD}.log`. Auto-detects Docker: logs to `/app/logs` if that directory exists, otherwise `logs/` relative to CWD.
- `LogPurgeUtility` — Daemon-thread scheduled deletion of old log files.

### Configuration Pattern

Every monitor is configured by **two** `.properties` files: `args[0]` = email/logging config, `args[1]` = monitor-specific config (both have hardcoded defaults if args are absent).

**`email.properties`** — Shared across all monitors:
```
mail.auth.method=SMTP|OAUTH2
mail.smtp.host / mail.smtp.port / mail.from / mail.to / mail.bcc
mail.oauth2.tenant.id / mail.oauth2.client.id / mail.oauth2.client.secret
log.level / log.folder / log.retention.days / log.purge.interval.hours
```

**Monitor-specific properties** (e.g., `jobqueuemonitor.properties`):
```
ibmi.server / ibmi.user / ibmi.password
metrics.port
monitor.interval.ms
# Plus monitor-specific thresholds, locations, keywords, etc.
```

### Metrics and Ports

All monitors expose Prometheus metrics at `http://localhost:<port>/metrics`. Ports **3010–3025** are assigned per monitor in installer config. Each additional client installation gets a 100-port offset (client 2: 3110–3125). Prometheus scrapes these; Grafana dashboards visualize them; Loki/Promtail handles log aggregation.

### Prometheus Metrics Pattern

Static fields (e.g., `private static final Gauge FOO = Gauge.build()...register()`) are the actual Prometheus metrics. Constructor parameters for maps/loggers passed into Metrics classes are for wiring only — do not add instance fields that shadow them.

### IBM i Connectivity

IBM i monitors use either:
- **jt400 `AS400` API** — For job queues, subsystems, QSYSOPR (`com.ibm.as400.access.*`)
- **jt400 JDBC** — `AS400JDBCDriver` via `DriverManager.getConnection("jdbc:as400://host;user=...;password=...")` for SQL queries against QSYS2 views

SMB-mounted IFS paths use the **smbj** library (`SMBClient`, `DiskShare`).

### Installer

`installer.iss` (Inno Setup 6) creates `IslandPacificMonitoringSetup.exe`. Key concepts:
- **ClientInstanceId** suffix on service names supports multiple client installations on one machine. AppId is dynamic: `{IP-MonitorSuite-{ClientInstanceId}}`. A `/CLIENT=CLIENTNAME` command-line param can set it at install time.
- **Three roles** controlled by checkboxes: MonitoringServer (Prometheus + Grafana + Loki), WinAgent, LogAgent (Promtail only)
- Services installed as Windows services via **WinSW** (Windows Service Wrapper); service names follow pattern `IPMonitoring_{MonitorName}`
- Install root: `C:\Program Files\Island Pacific\Operations Monitor\{ClientId}\`
- Property files use `onlyifdoesntexist` flag — preserved on upgrade, never overwritten
- `build-installer.ps1` orchestrates: `mvn clean package` → copy JARs to `installer/resources/` → validate external binaries (WinSW, Prometheus, Grafana, Loki, Promtail) → compile `.iss`
- If `ServiceScheduler.iss` exists at the repo root, it is compiled as a standalone installer (`ServiceSchedulerSetup.exe`) after the main installer — failures are non-fatal
### ProGuard Obfuscation

All output JARs are obfuscated by ProGuard before bundling into the installer. Config is in `proguard.conf`. This is intentional — do not remove it. ProGuard explicitly keeps `public static void main()` methods and enums.
