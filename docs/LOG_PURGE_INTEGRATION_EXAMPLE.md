# Log Purge Integration Example

This document shows exactly how to integrate automatic log purge into your monitoring modules.

## Step 1: Add Properties to Configuration Files

Add these lines to your properties files (e.g., `email.properties`, `subsystem.properties`, etc.):

```properties
# Log retention (days) - files older than this will be purged
log.retention.days=30

# Log purge interval (hours) - how often to run purge (24 = daily)
log.purge.interval.hours=24
```

## Step 2: Add Integration Code to Main Application

Add the log purge startup code **right after** `setupLogger()` in your main method.

### Pattern 1: Using appProps (e.g., ibmsubsystemmonitoring)

```java
public static void main(String[] args) {
    try {
        loadProperties(); // Load properties first
        setupLogger(); // Initialize logger
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));
        com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        
        // ... rest of your application code ...
    }
}
```

### Pattern 2: Using emailProperties (e.g., serveruptime)

```java
public static void main(String[] args) {
    try {
        loadConfiguration(args);
        setupLogger();
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(emailProperties.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailProperties.getProperty("log.purge.interval.hours", "24"));
        com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        
        // ... rest of your application code ...
    }
}
```

### Pattern 3: Using both properties (fallback)

```java
// Start automatic log purge (check both properties files)
int retentionDays = Integer.parseInt(emailProperties.getProperty("log.retention.days", 
    appProps.getProperty("log.retention.days", "30")));
int purgeIntervalHours = Integer.parseInt(emailProperties.getProperty("log.purge.interval.hours",
    appProps.getProperty("log.purge.interval.hours", "24")));
com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
```

## Step 3: Verify Shutdown Hook

Make sure your shutdown hook calls `AppLogger.closeLogger()` (which automatically stops the purge scheduler):

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    logger.info("Shutting down...");
    com.islandpacific.monitoring.common.AppLogger.closeLogger(); // This stops purge too
}));
```

## Complete Example: ibmsubsystemmonitoring

```java
public static void main(String[] args) {
    if (args.length >= 1) appPropertiesFilePath = args[0];
    if (args.length >= 2) emailPropertiesFilePath = args[1];

    try {
        loadProperties();
        setupLogger();
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(appProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(appProps.getProperty("log.purge.interval.hours", "24"));
        com.islandpacific.monitoring.common.AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);
        
        // ... rest of initialization ...
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down subsystem monitor...");
            scheduler.shutdown();
            metricsExporter.stop();
            com.islandpacific.monitoring.common.AppLogger.closeLogger(); // Stops purge automatically
        }));
        
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Startup failure: " + e.getMessage(), e);
        System.exit(1);
    }
}
```

## Configuration Options

### Retention Days
- **7 days**: For development/testing
- **30 days**: Recommended for production
- **90 days**: For compliance/regulatory requirements

### Purge Interval
- **24 hours**: Daily purge (recommended)
- **168 hours**: Weekly purge (7 days)
- **720 hours**: Monthly purge (30 days)

## Verification

After integration, you should see log messages like:

```
INFO: Scheduled log purge started: Retention=30 days, Interval=24 hours
INFO: Deleted old log file: /app/logs/ibmsubsystemmonitoring_2024-12-01.log (Date: 2024-12-01, Size: 2.5 MB)
INFO: Log purge completed: Deleted 5 file(s), freed 12.3 MB
```

## Disabling Log Purge

To disable automatic purge, either:
1. Don't call `startScheduledLogPurge()`
2. Set `log.purge.interval.hours` to a very high value (e.g., 999999)
3. Set `log.retention.days` to a very high value (e.g., 9999)

