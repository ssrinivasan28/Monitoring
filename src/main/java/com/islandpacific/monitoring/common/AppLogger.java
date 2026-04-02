package com.islandpacific.monitoring.common;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Standardized logging utility for all monitoring modules.
 * Provides consistent logging configuration with daily log rotation,
 * configurable log levels, and proper cleanup.
 */
public class AppLogger {
    
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private static Logger sharedLogger;
    private static FileHandler fileHandler;
    private static String currentModuleName;
    private static String currentLogFolder;
    private static boolean isInitialized = false;
    private static ScheduledExecutorService purgeScheduler;
    
    /**
     * Initializes the logger for a specific module.
     * Should be called once at application startup.
     * 
     * For Docker containers: Set log.folder=/app/logs in your properties file
     * For local JAR execution: Use log.folder=logs (relative to working directory)
     * 
     * @param moduleName The name of the module (e.g., "userprofilechecker", "ibmsubsystemmonitoring")
     * @param logLevel The log level (e.g., "INFO", "WARNING", "SEVERE"). Defaults to "INFO" if null or empty.
     * @param logFolder The folder for log files. Defaults to "logs" if null or empty.
     *                   For Docker: use "/app/logs" to match volume mounts.
     */
    public static void setupLogger(String moduleName, String logLevel, String logFolder) {
        if (isInitialized && moduleName.equals(currentModuleName)) {
            // Already initialized for this module
            return;
        }
        
        // Close previous handler if switching modules
        if (fileHandler != null) {
            fileHandler.close();
            fileHandler = null;
        }
        
        currentModuleName = moduleName;
        String effectiveLogLevel = (logLevel != null && !logLevel.trim().isEmpty()) ? logLevel.trim() : DEFAULT_LOG_LEVEL;
        
        // Determine log folder: use provided, or default
        // For Docker: typically /app/logs (absolute path)
        // For local: typically logs (relative path)
        String effectiveLogFolder;
        if (logFolder != null && !logFolder.trim().isEmpty()) {
            effectiveLogFolder = logFolder.trim();
        } else {
            // Auto-detect: if /app exists, we're likely in Docker, use /app/logs
            // Otherwise use default "logs" relative path
            File appDir = new File("/app");
            if (appDir.exists() && appDir.isDirectory()) {
                effectiveLogFolder = "/app/logs";
            } else {
                effectiveLogFolder = DEFAULT_LOG_DIR;
            }
        }
        currentLogFolder = effectiveLogFolder;
        
        // Disable default console handler to prevent duplicate console output
        LogManager.getLogManager().reset();
        
        // Create logger with module-specific name
        sharedLogger = Logger.getLogger("Monitoring." + moduleName);
        sharedLogger.setLevel(Level.parse(effectiveLogLevel));
        sharedLogger.setUseParentHandlers(false); // Prevent logs from going to root logger's handlers
        
        // Create log directory if it doesn't exist
        // Note: When running from JAR, this creates logs relative to the current working directory
        // (where you run 'java -jar'), not relative to the JAR file location
        File logDir = new File(effectiveLogFolder);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created && !logDir.exists()) {
                System.err.println("WARNING: Could not create log directory: " + logDir.getAbsolutePath());
            }
        }
        
        try {
            // Create daily log file: logs/{module}_YYYY-MM-DD.log
            String dateStr = LocalDate.now().format(DATE_FORMATTER);
            String logFileName = effectiveLogFolder + File.separator + moduleName + "_" + dateStr + ".log";
            File logFile = new File(logFileName);
            String absoluteLogPath = logFile.getAbsolutePath();
            
            fileHandler = new FileHandler(logFileName, true); // true for append mode
            fileHandler.setFormatter(new LogFormatter());
            sharedLogger.addHandler(fileHandler);
            
            // Console Handler for real-time console output
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            consoleHandler.setLevel(Level.parse(effectiveLogLevel));
            sharedLogger.addHandler(consoleHandler);
            
            isInitialized = true;
            sharedLogger.info("Logger initialized for module: " + moduleName + " (Level: " + effectiveLogLevel + ")");
            sharedLogger.info("Log file location: " + absoluteLogPath);
            sharedLogger.info("Current working directory: " + System.getProperty("user.dir"));
            sharedLogger.info("Log folder (configured): " + effectiveLogFolder);
            
            // Check if running in Docker
            File appDir = new File("/app");
            if (appDir.exists() && appDir.isDirectory()) {
                sharedLogger.info("Docker environment detected (/app directory exists)");
            }
            
        } catch (IOException e) {
            System.err.println("Could not set up logger file handler: " + e.getMessage());
            System.err.println("Attempted log path: " + new File(effectiveLogFolder + File.separator + moduleName + "_" + LocalDate.now().format(DATE_FORMATTER) + ".log").getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the logger instance for the current module.
     * setupLogger() must be called first.
     * 
     * @return The logger instance
     */
    public static Logger getLogger() {
        if (sharedLogger == null) {
            // Fallback: create a basic logger if setupLogger wasn't called
            sharedLogger = Logger.getLogger("Monitoring.Default");
            sharedLogger.setLevel(Level.INFO);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            sharedLogger.addHandler(consoleHandler);
            sharedLogger.warning("AppLogger.getLogger() called before setupLogger(). Using default configuration.");
        }
        return sharedLogger;
    }
    
    /**
     * Starts automatic log purging on a schedule.
     * 
     * @param retentionDays Number of days to retain logs (files older will be deleted)
     * @param purgeIntervalHours How often to run purge (in hours, e.g., 24 for daily)
     */
    public static void startScheduledLogPurge(int retentionDays, int purgeIntervalHours) {
        if (purgeScheduler != null && !purgeScheduler.isShutdown()) {
            sharedLogger.warning("Log purge scheduler already running. Ignoring new schedule.");
            return;
        }
        
        if (currentLogFolder == null) {
            sharedLogger.warning("Cannot start log purge: logger not initialized. Call setupLogger() first.");
            return;
        }
        
        purgeScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogPurgeScheduler");
            t.setDaemon(true);
            return t;
        });
        
        purgeScheduler.scheduleAtFixedRate(() -> {
            try {
                LogPurgeUtility.purgeModuleLogs(currentLogFolder, retentionDays, currentModuleName);
            } catch (Exception e) {
                if (sharedLogger != null) {
                    sharedLogger.log(Level.WARNING, "Error during scheduled log purge", e);
                } else {
                    System.err.println("Error during scheduled log purge: " + e.getMessage());
                }
            }
        }, purgeIntervalHours, purgeIntervalHours, TimeUnit.HOURS);
        
        if (sharedLogger != null) {
            sharedLogger.info("Scheduled log purge started: Retention=" + retentionDays + " days, Interval=" + purgeIntervalHours + " hours");
        }
    }
    
    /**
     * Stops the automatic log purge scheduler.
     */
    public static void stopScheduledLogPurge() {
        if (purgeScheduler != null && !purgeScheduler.isShutdown()) {
            purgeScheduler.shutdown();
            try {
                if (!purgeScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    purgeScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                purgeScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            purgeScheduler = null;
            if (sharedLogger != null) {
                sharedLogger.info("Scheduled log purge stopped");
            }
        }
    }
    
    /**
     * Closes the file handler and cleans up resources.
     * Should be called during application shutdown.
     */
    public static void closeLogger() {
        stopScheduledLogPurge();
        if (fileHandler != null) {
            fileHandler.close();
            fileHandler = null;
        }
        isInitialized = false;
    }
    
    /**
     * Custom log formatter with consistent timestamp and level formatting.
     */
    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("[%1$tF %1$tT] [%2$-7s] %3$s %n",
                    new Date(record.getMillis()),
                    record.getLevel().getName(),
                    formatMessage(record));
        }
    }
}

