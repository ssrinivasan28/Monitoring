# Log Purge Guide

This guide explains how to handle log file cleanup to prevent disk space issues.

## Overview

The monitoring system creates daily log files with the pattern: `{module}_YYYY-MM-DD.log`

Without cleanup, these files can accumulate and consume disk space. The `LogPurgeUtility` provides several options for managing old log files.

## Options for Log Purge

### Option 1: Automatic Scheduled Purge (Recommended)

The `AppLogger` can automatically purge old logs on a schedule. Add this to your main application after `setupLogger()`:

```java
// After AppLogger.setupLogger(...)
int retentionDays = 30; // Keep logs for 30 days
int purgeIntervalHours = 24; // Run purge once per day
AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
```

**Example Integration:**

```java
public static void main(String[] args) {
    // Setup logger
    AppLogger.setupLogger("ibmperformancemonitoring", logLevel, logFolder);
    
    // Start automatic log purge (keep 30 days, run daily)
    AppLogger.startScheduledLogPurge(30, 24);
    
    // ... rest of your application code ...
    
    // Shutdown hook (already calls AppLogger.closeLogger() which stops purge)
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        AppLogger.closeLogger(); // This also stops the purge scheduler
    }));
}
```

**Configuration via Properties:**

You can make it configurable:

```properties
# Log retention (days)
log.retention.days=30

# Log purge interval (hours)
log.purge.interval.hours=24
```

Then in your code:
```java
int retentionDays = Integer.parseInt(props.getProperty("log.retention.days", "30"));
int purgeInterval = Integer.parseInt(props.getProperty("log.purge.interval.hours", "24"));
AppLogger.startScheduledLogPurge(retentionDays, purgeInterval);
```

### Option 2: Manual Purge via Utility Class

Use `LogPurgeUtility` programmatically:

```java
import com.islandpacific.monitoring.common.LogPurgeUtility;

// Purge logs for a specific module
int deleted = LogPurgeUtility.purgeModuleLogs("/app/logs", 30, "ibmperformancemonitoring");

// Purge all logs in a directory
int deleted = LogPurgeUtility.purgeAllLogs("/app/logs", 30);
```

### Option 3: Standalone Execution (Cron/Windows Task Scheduler)

The `LogPurgeUtility` has a `main` method for standalone execution:

```bash
# Purge all logs in a directory (keep 30 days)
java -cp your-app.jar com.islandpacific.monitoring.common.LogPurgeUtility /app/logs 30

# Purge logs for a specific module
java -cp your-app.jar com.islandpacific.monitoring.common.LogPurgeUtility /app/logs 30 ibmperformancemonitoring
```

**For Docker:**

Add a cron job or scheduled task in your Docker container, or run it as a separate container:

```yaml
# In docker-compose.yml
services:
  log-purge:
    image: your-java-image
    volumes:
      - ./logs:/app/logs
    command: >
      sh -c "while true; do
        java -cp /app/your-app.jar com.islandpacific.monitoring.common.LogPurgeUtility /app/logs 30;
        sleep 86400;
      done"
```

### Option 4: External Script (Windows/Linux)

**Windows Batch Script (`purge-logs.bat`):**

```batch
@echo off
set RETENTION_DAYS=30
set LOG_DIR=C:\FContainer\clients\macc\logs

for /d %%d in ("%LOG_DIR%\*") do (
    echo Purging logs in %%d
    forfiles /p "%%d" /m *.log /d -%RETENTION_DAYS% /c "cmd /c del @path" 2>nul
)
```

**Linux Shell Script (`purge-logs.sh`):**

```bash
#!/bin/bash
RETENTION_DAYS=30
LOG_DIR="/app/logs"

find "$LOG_DIR" -name "*.log" -type f -mtime +$RETENTION_DAYS -delete
```

## Recommended Configuration

### For Docker Containers

**Best Practice:** Use automatic scheduled purge integrated into each application:

1. Add to properties file:
```properties
log.retention.days=30
log.purge.interval.hours=24
```

2. Add to main application:
```java
// After logger setup
int retentionDays = Integer.parseInt(props.getProperty("log.retention.days", "30"));
AppLogger.startScheduledLogPurge(retentionDays, 24);
```

### For Local JAR Execution

Use the same automatic purge, or run a scheduled task externally.

## Retention Policy Recommendations

- **Development:** 7-14 days
- **Production:** 30-90 days
- **Compliance/Regulatory:** As required (may need longer retention)

## Monitoring Log Purge

The purge utility logs its activities:

```
INFO: Deleted old log file: /app/logs/ibmperformancemonitoring_2024-12-01.log (Date: 2024-12-01, Size: 2.5 MB)
INFO: Log purge completed: Deleted 5 file(s), freed 12.3 MB
```

## Troubleshooting

**Logs not being purged:**
- Check that `AppLogger.startScheduledLogPurge()` is called
- Verify retention days is not too high
- Check file permissions on log directory
- Review application logs for purge errors

**Disk space still growing:**
- Verify purge is actually running (check logs)
- Consider reducing retention days
- Check for other files in log directories
- Verify volume mounts in Docker are working correctly

