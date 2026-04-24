package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service class for Windows File System Cardinality Monitoring.
 * Monitors local Windows folders for file count thresholds and sends email alerts.
 */
public class FileSystemCardinalityService {

    private final Logger mainLogger;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.([a-zA-Z0-9]{1,5})(?:\\s.*)?$");

    public FileSystemCardinalityService(Logger mainLogger, EmailService emailService,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts) {
        this.mainLogger = mainLogger;
        this.emailService = emailService;
        this.totalFileCounts = totalFileCounts;
        this.newFileCounts = newFileCounts;
    }

    /**
     * Monitors a single Windows folder based on the provided configuration.
     * It counts files, checks against min/max thresholds, and sends email alerts if violated.
     *
     * @param config The MonitoringConfig for the folder to monitor.
     */
    public void monitorFolder(FileSystemCardinalityConfig.MonitoringConfig config) {
        String locationName = config.getName();
        String folderPathString = config.getPathString();
        int minFiles = config.getMinFiles();
        int maxFiles = config.getMaxFiles();
        String monitorServerName = config.getMonitorServerName();

        Logger currentLogger = config.getLocationLogger();
        if (currentLogger == null) {
            mainLogger.severe(String.format(
                    "Critical Error: Location logger is null for monitoring config '%s' (Path: %s). Using main logger.",
                    locationName, folderPathString));
            currentLogger = mainLogger;
        }

        Set<String> processedPathsForLocation = config.getProcessedFilePaths();
        String stateFilePath = config.getProcessedFilesStateFilePath();
        Set<String> configuredFileExtensions = config.getFileExtensions();

        currentLogger.info(
                String.format("Monitoring Windows folder location '%s' at path '%s'. Expected files: min=%d, max=%d. Recursive: %b.",
                        locationName, folderPathString, minFiles, maxFiles, config.isRecursive()));

        totalFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());
        newFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());

        List<String> allMatchingFileNames = new ArrayList<>();

        try {
            Path folderPath = config.getLocalPath();

            if (!Files.exists(folderPath)) {
                currentLogger.warning(String.format("Windows folder does not exist: %s. Cannot monitor.", folderPath));
                FileSystemCardinalityMetrics.setFileCount(locationName, -1);
                FileSystemCardinalityMetrics.incrementTooFewFilesAlert(locationName);
                return;
            }

            if (!Files.isDirectory(folderPath)) {
                currentLogger.warning(String.format("Path is not a directory: %s. Cannot monitor.", folderPath));
                FileSystemCardinalityMetrics.setFileCount(locationName, -1);
                FileSystemCardinalityMetrics.incrementTooFewFilesAlert(locationName);
                return;
            }

            // List files based on recursive setting
            int maxDepth = config.isRecursive() ? Integer.MAX_VALUE : 1;
            try (Stream<Path> files = Files.walk(folderPath, maxDepth)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> configuredFileExtensions.isEmpty()
                                || isFileExtensionMatch(p.getFileName().toString(), configuredFileExtensions))
                        .map(p -> p.getFileName().toString())
                        .forEach(allMatchingFileNames::add);
            }

            // Process collected files and send threshold alerts
            processCollectedFilesAndSendThresholdAlerts(config, allMatchingFileNames, currentLogger,
                    processedPathsForLocation, stateFilePath);

        } catch (IOException e) {
            currentLogger.log(Level.SEVERE,
                    String.format("Error during file listing for %s: %s", folderPathString, e.getMessage()), e);
            handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(), "I/O Error", -2);
        } catch (SecurityException e) {
            currentLogger.log(Level.SEVERE,
                    String.format("Permission denied for %s: %s", folderPathString, e.getMessage()), e);
            handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(), "Permission Denied", -3);
        }
    }

    /**
     * Helper method to check if a file's extension matches any of the configured extensions.
     */
    private boolean isFileExtensionMatch(String fileName, Set<String> configuredExtensions) {
        if (configuredExtensions.isEmpty()) {
            return true;
        }
        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName.toLowerCase());
        return matcher.find() && configuredExtensions.contains("." + matcher.group(1));
    }

    /**
     * Handles common logic for processing collected file names, updating counts,
     * and sending ONLY THRESHOLD alerts.
     */
    private void processCollectedFilesAndSendThresholdAlerts(FileSystemCardinalityConfig.MonitoringConfig config,
            List<String> allMatchingFileNames,
            Logger currentLogger, Set<String> processedPathsForLocation, String stateFilePath) throws IOException {

        String locationName = config.getName();
        String folderPathString = config.getPathString();
        int minFiles = config.getMinFiles();
        int maxFiles = config.getMaxFiles();
        String monitorServerName = config.getMonitorServerName();

        int currentTotal = allMatchingFileNames.size();
        int currentNew = 0;
        List<String> detectedNewFiles = new ArrayList<>();

        for (String fileName : allMatchingFileNames) {
            String fullPathForTracking = config.getLocalPath().resolve(fileName).toAbsolutePath().toString();

            if (!processedPathsForLocation.contains(fullPathForTracking)) {
                detectedNewFiles.add(fileName);
                processedPathsForLocation.add(fullPathForTracking);
                currentNew++;
                currentLogger.info(String.format("New file detected (type '%s'): %s",
                        getFileExtension(fileName), fileName));

                try {
                    Files.write(Paths.get(stateFilePath),
                            (fullPathForTracking + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    currentLogger.fine("Appended processed file path to state file: " + fullPathForTracking);
                } catch (IOException e) {
                    currentLogger.log(Level.SEVERE, "Error writing processed file path to state file for location "
                            + locationName + ": " + fullPathForTracking, e);
                }
            }
        }

        totalFileCounts.get(locationName).put("all", currentTotal);
        newFileCounts.get(locationName).put("all", currentNew);

        // Update Prometheus metrics
        FileSystemCardinalityMetrics.setFileCount(locationName, currentTotal);

        // Email sending logic for threshold breaches ONLY
        if (currentTotal < minFiles) {
            if (currentTotal == 0 && config.isIgnoreZeroFileAlert()) {
                currentLogger.info(String.format(
                        "File count for '%s' is 0, but alerts are suppressed for this condition. No email sent.",
                        locationName));
            } else {
                String subject = String.format(config.getAlertTooFewSubject(), monitorServerName, locationName);
                String body = String.format(config.getAlertTooFewBodyPrefix(), monitorServerName, folderPathString,
                        currentTotal, minFiles);
                currentLogger.warning(
                        String.format("Alert for '%s': File count (%d) is below minimum threshold (%d). Sending email.",
                                locationName, currentTotal, minFiles));
                emailService.sendEmail(locationName, folderPathString, subject, body, config.getEmailImportance());
                FileSystemCardinalityMetrics.incrementTooFewFilesAlert(locationName);
            }
        } else if (currentTotal > maxFiles) {
            String subject = String.format(config.getAlertTooManySubject(), monitorServerName, locationName);
            String body = String.format(config.getAlertTooManyBodyPrefix(), monitorServerName, folderPathString,
                    currentTotal, maxFiles);
            currentLogger.warning(
                    String.format("Alert for '%s': File count (%d) is above maximum threshold (%d). Sending email.",
                            locationName, currentTotal, maxFiles));
            emailService.sendEmail(locationName, folderPathString, subject, body, config.getEmailImportance());
            FileSystemCardinalityMetrics.incrementTooManyFilesAlert(locationName);
        } else {
            currentLogger.info(String.format("File count for '%s' (%d) is within acceptable range [%d, %d]. No alert sent.",
                            locationName, currentTotal, minFiles, maxFiles));
        }
    }

    /**
     * Helper to extract file extension for logging.
     */
    private String getFileExtension(String fileName) {
        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    /**
     * Handles common error scenarios by logging and updating metrics.
     */
    private void handleAccessError(String locationName, String folderPathString, String monitorServerName,
            String errorMessage, String errorType, int metricCode) {
        FileSystemCardinalityMetrics.setFileCount(locationName, metricCode);
        FileSystemCardinalityMetrics.incrementTooFewFilesAlert(locationName);
    }
}
