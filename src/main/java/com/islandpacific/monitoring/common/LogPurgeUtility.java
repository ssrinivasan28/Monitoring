package com.islandpacific.monitoring.common;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for purging old log files based on retention policy.
 * Supports both programmatic cleanup and scheduled cleanup.
 */
public class LogPurgeUtility {
    
    private static final Logger logger = Logger.getLogger(LogPurgeUtility.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Purges old log files from a directory based on retention days.
     * 
     * @param logDirectory The directory containing log files
     * @param retentionDays Number of days to retain logs (files older than this will be deleted)
     * @param logFilePattern Pattern to match log files (e.g., "ibmperformancemonitoring_", or null for all .log files)
     * @return Number of files deleted
     */
    public static int purgeOldLogs(String logDirectory, int retentionDays, String logFilePattern) {
        if (retentionDays < 0) {
            logger.warning("Invalid retention days: " + retentionDays + ". Must be >= 0. Skipping purge.");
            return 0;
        }
        
        File logDir = new File(logDirectory);
        if (!logDir.exists() || !logDir.isDirectory()) {
            logger.warning("Log directory does not exist or is not a directory: " + logDirectory);
            return 0;
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        int deletedCount = 0;
        long totalBytesFreed = 0;
        
        File[] logFiles = logDir.listFiles((dir, name) -> {
            if (!name.endsWith(".log")) {
                return false;
            }
            if (logFilePattern != null && !name.startsWith(logFilePattern)) {
                return false;
            }
            return true;
        });
        
        if (logFiles == null) {
            logger.warning("Could not list files in log directory: " + logDirectory);
            return 0;
        }
        
        for (File logFile : logFiles) {
            try {
                // Extract date from filename: {module}_YYYY-MM-DD.log
                String fileName = logFile.getName();
                String datePart = extractDateFromFileName(fileName);
                
                if (datePart != null) {
                    LocalDate fileDate = LocalDate.parse(datePart, DATE_FORMATTER);
                    
                    if (fileDate.isBefore(cutoffDate)) {
                        long fileSize = logFile.length();
                        if (logFile.delete()) {
                            deletedCount++;
                            totalBytesFreed += fileSize;
                            logger.info("Deleted old log file: " + logFile.getAbsolutePath() + 
                                      " (Date: " + datePart + ", Size: " + formatBytes(fileSize) + ")");
                        } else {
                            logger.warning("Failed to delete log file: " + logFile.getAbsolutePath());
                        }
                    }
                } else {
                    // If we can't parse the date, check file modification time as fallback
                    long fileAgeDays = (System.currentTimeMillis() - logFile.lastModified()) / (1000 * 60 * 60 * 24);
                    if (fileAgeDays > retentionDays) {
                        long fileSize = logFile.length();
                        if (logFile.delete()) {
                            deletedCount++;
                            totalBytesFreed += fileSize;
                            logger.info("Deleted old log file (by modification date): " + logFile.getAbsolutePath() + 
                                      " (Age: " + fileAgeDays + " days, Size: " + formatBytes(fileSize) + ")");
                        } else {
                            logger.warning("Failed to delete log file: " + logFile.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing log file: " + logFile.getAbsolutePath(), e);
            }
        }
        
        if (deletedCount > 0) {
            logger.info("Log purge completed: Deleted " + deletedCount + " file(s), freed " + formatBytes(totalBytesFreed));
        } else {
            logger.info("Log purge completed: No files to delete (retention: " + retentionDays + " days)");
        }
        
        return deletedCount;
    }
    
    /**
     * Purges old log files for a specific module.
     * 
     * @param logDirectory The directory containing log files
     * @param retentionDays Number of days to retain logs
     * @param moduleName The module name (e.g., "ibmperformancemonitoring")
     * @return Number of files deleted
     */
    public static int purgeModuleLogs(String logDirectory, int retentionDays, String moduleName) {
        String pattern = moduleName + "_";
        return purgeOldLogs(logDirectory, retentionDays, pattern);
    }
    
    /**
     * Purges all old log files in a directory (any .log file matching the date pattern).
     * 
     * @param logDirectory The directory containing log files
     * @param retentionDays Number of days to retain logs
     * @return Number of files deleted
     */
    public static int purgeAllLogs(String logDirectory, int retentionDays) {
        return purgeOldLogs(logDirectory, retentionDays, null);
    }
    
    /**
     * Extracts date from log filename.
     * Expected format: {module}_YYYY-MM-DD.log
     * 
     * @param fileName The log file name
     * @return Date string in YYYY-MM-DD format, or null if not found
     */
    private static String extractDateFromFileName(String fileName) {
        // Remove .log extension
        String nameWithoutExt = fileName.replace(".log", "");
        
        // Try to find date pattern YYYY-MM-DD
        // Look for pattern: _YYYY-MM-DD at the end
        int lastUnderscore = nameWithoutExt.lastIndexOf('_');
        if (lastUnderscore >= 0 && lastUnderscore < nameWithoutExt.length() - 1) {
            String datePart = nameWithoutExt.substring(lastUnderscore + 1);
            // Validate it's a date (YYYY-MM-DD format, 10 characters)
            if (datePart.length() == 10 && datePart.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return datePart;
            }
        }
        
        return null;
    }
    
    /**
     * Formats bytes to human-readable string.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Main method for standalone execution (useful for cron jobs or scheduled tasks).
     * Usage: java LogPurgeUtility <logDirectory> <retentionDays> [moduleName]
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: LogPurgeUtility <logDirectory> <retentionDays> [moduleName]");
            System.err.println("  logDirectory: Path to log directory");
            System.err.println("  retentionDays: Number of days to retain logs (files older will be deleted)");
            System.err.println("  moduleName: (Optional) Specific module name to purge (e.g., 'ibmperformancemonitoring')");
            System.exit(1);
        }
        
        String logDirectory = args[0];
        int retentionDays;
        try {
            retentionDays = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid retention days: " + args[1]);
            System.exit(1);
            return;
        }
        
        if (args.length >= 3) {
            String moduleName = args[2];
            System.out.println("Purging logs for module: " + moduleName);
            purgeModuleLogs(logDirectory, retentionDays, moduleName);
        } else {
            System.out.println("Purging all logs in directory: " + logDirectory);
            purgeAllLogs(logDirectory, retentionDays);
        }
    }
}

