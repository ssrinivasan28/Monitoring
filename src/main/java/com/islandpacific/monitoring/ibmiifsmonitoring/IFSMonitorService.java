package com.islandpacific.monitoring.ibmiifsmonitoring;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session; // smbj Session
import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IFSMonitorService {

    private final Logger mainLogger; // Main application logger
    private final EmailService emailService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private final ConcurrentHashMap<String, IFSMonitorConfig.SmbCredentials> globalSmbCredentials;

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.([a-zA-Z0-9]{1,5})(?:\\s.*)?$");
    private static final String LAST_RUN_LOG = "logs/last_run_timestamp.txt";

    /**
     * Constructs an IFSMonitorService.
     *
     * @param mainLogger           The main application logger.
     * @param emailService         The EmailService instance to use for sending
     *                             alerts.
     * @param totalFileCounts      A map to store total file counts per location and
     *                             extension.
     * @param newFileCounts        A map to store new file counts per location and
     *                             extension.
     * @param globalSmbCredentials A map of SMB server IPs to their credentials.
     */
    public IFSMonitorService(Logger mainLogger, EmailService emailService,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
            ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts,
            ConcurrentHashMap<String, IFSMonitorConfig.SmbCredentials> globalSmbCredentials) {
        this.mainLogger = mainLogger;
        this.emailService = emailService;
        this.totalFileCounts = totalFileCounts;
        this.newFileCounts = newFileCounts;
        this.globalSmbCredentials = globalSmbCredentials;
    }

    /**
     * Monitors a single IFS folder based on the provided configuration.
     * It counts files, checks against min/max thresholds, and sends email alerts if
     * violated.
     *
     * @param config The IFSMonitorConfig for the folder to monitor.
     */
    public void monitorFolder(IFSMonitorConfig.MonitoringConfig config) {
        String locationName = config.getName();
        String folderPathString = config.getPathString(); // Use the original string for logging
        int minFiles = config.getMinFiles();
        int maxFiles = config.getMaxFiles();
        String monitorServerName = config.getMonitorServerName();

        Logger currentLogger = config.getLocationLogger();
        // Defensive null check for currentLogger
        if (currentLogger == null) {
            mainLogger.severe(String.format(
                    "Critical Error: Location logger is null for monitoring config '%s' (Path: %s). Using main logger for this scan cycle.",
                    locationName, folderPathString));
            currentLogger = mainLogger; // Fallback to main logger to avoid NPE
        }

        Set<String> processedPathsForLocation = config.getProcessedFilePaths();
        String stateFilePath = config.getProcessedFilesStateFilePath();
        Set<String> configuredFileExtensions = config.getFileExtensions();

        currentLogger.info(
                String.format("Monitoring IFS location '%s' at path '%s'. Expected files: min=%d, max=%d. Is SMB: %b.",
                        locationName, folderPathString, minFiles, maxFiles, config.isSmbPath()));

        totalFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());
        newFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());

        List<String> allMatchingFileNames = new ArrayList<>();

        try {
            if (config.isSmbPath()) {
                // SMB path handling using smbj
                String server = config.getServerIp();
                String share = config.getShareName();
                String pathInShare = config.getSharePath();

                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(server)) {
                    AuthenticationContext ac = null;
                    IFSMonitorConfig.SmbCredentials creds = globalSmbCredentials.get(server);
                    if (creds != null) {
                        ac = creds.toAuthenticationContext();
                        currentLogger
                                .info(String.format("Using provided SMB credentials for server %s for location '%s'.",
                                        server, locationName));
                    } else {
                        currentLogger.warning(String.format(
                                "SMB path '%s' for location '%s' has no global credentials defined for server %s. Attempting anonymous access, which may fail.",
                                folderPathString, locationName, server));
                    }

                    Session smbjSession = connection.authenticate(ac);
                    try (DiskShare diskShare = (DiskShare) smbjSession.connectShare(share)) {
                        // Check if the share path exists and is a directory within the share
                        if (!diskShare.folderExists(pathInShare)) {
                            currentLogger.warning(String.format(
                                    "SMB folder does not exist or is not a directory: smb://%s/%s/%s. Cannot monitor.",
                                    server, share, pathInShare));
                            // IFSMonitorMetrics.setFileCount(locationName, -1);
                            // String subject = String.format("CRITICAL ALERT on %s: SMB Folder Inaccessible
                            // - %s", monitorServerName, locationName);
                            // String body = String.format("Observation from server %s: The SMB folder at
                            // smb://%s/%s/%s does not exist or is inaccessible. Please verify the path and
                            // permissions.", monitorServerName, server, share, pathInShare);
                            // emailService.sendEmail(locationName, folderPathString, subject, body,
                            // "High");
                            IFSMonitorMetrics.incrementTooFewFilesAlert(locationName);
                            return;
                        }

                        for (FileIdBothDirectoryInformation fileInfo : diskShare.list(pathInShare)) {
                            // FILE_ATTRIBUTE_DIRECTORY has a value of 16 (0x10)
                            if ((fileInfo.getFileAttributes() & 16L) == 0) { // If the directory bit is NOT set, it's a
                                                                             // regular file
                                String fileName = fileInfo.getFileName();
                                // Apply file extension filtering if configured
                                if (configuredFileExtensions.isEmpty()
                                        || isFileExtensionMatch(fileName, configuredFileExtensions)) {
                                    allMatchingFileNames.add(fileName);
                                }
                            }
                        }
                    }
                } catch (SMBException e) {
                    currentLogger.log(Level.SEVERE, String.format(
                            "SMB access denied or error for location '%s' (%s): %s. Please check credentials and network access.",
                            locationName, folderPathString, e.getMessage()), e);
                    handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(),
                            "Permission Denied", -3);
                    return;
                } catch (IOException e) {
                    currentLogger.log(Level.SEVERE,
                            String.format("Error during SMB client operation for location '%s' (%s): %s", locationName,
                                    folderPathString, e.getMessage()),
                            e);
                    handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(), "Access Error",
                            -2);
                    return;
                } finally {
                    client.close(); // Ensure SMBClient is closed
                }

            } else {
                // Local/OS-managed path handling using java.nio.file
                Path folderPath = config.getLocalPath();

                if (!Files.exists(folderPath)) {
                    currentLogger
                            .warning(String.format("Local folder does not exist: %s. Cannot monitor.", folderPath));
                    IFSMonitorMetrics.setFileCount(locationName, -1);
                    // String subject = String.format("CRITICAL ALERT on %s: Local Folder
                    // Inaccessible - %s", monitorServerName, locationName);
                    // String body = String.format("Observation from server %s: The local folder at
                    // %s does not exist or is inaccessible. Please verify the path and
                    // permissions.", monitorServerName, folderPath);
                    // emailService.sendEmail(locationName, folderPathString, subject, body,
                    // "High");
                    IFSMonitorMetrics.incrementTooFewFilesAlert(locationName);
                    return;
                }

                if (!Files.isDirectory(folderPath)) {
                    currentLogger
                            .warning(String.format("Local path is not a directory: %s. Cannot monitor.", folderPath));
                    IFSMonitorMetrics.setFileCount(locationName, -1);
                    // String subject = String.format("CRITICAL ALERT on %s: Local Path Not a
                    // Directory - %s", monitorServerName, locationName);
                    // String body = String.format("Observation from server %s: The path %s is not a
                    // directory. Please verify the path.", monitorServerName, folderPath);
                    // emailService.sendEmail(locationName, folderPathString, subject, body,
                    // "High");
                    IFSMonitorMetrics.incrementTooFewFilesAlert(locationName);
                    return;
                }

                try (Stream<Path> files = Files.list(folderPath)) {
                    files.filter(Files::isRegularFile)
                            .filter(p -> configuredFileExtensions.isEmpty()
                                    || isFileExtensionMatch(p.getFileName().toString(), configuredFileExtensions))
                            .map(p -> p.getFileName().toString())
                            .forEach(allMatchingFileNames::add);
                }
            }

            // Common logic for processing collected file names and sending threshold alerts
            processCollectedFilesAndSendThresholdAlerts(config, allMatchingFileNames, currentLogger,
                    processedPathsForLocation, stateFilePath);

        } catch (IOException e) {
            currentLogger.log(Level.SEVERE,
                    String.format("Error during file listing for %s: %s", folderPathString, e.getMessage()), e);
            handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(), "I/O Error", -2);
        } catch (SecurityException e) {
            currentLogger.log(Level.SEVERE,
                    String.format("Permission denied for %s: %s", folderPathString, e.getMessage()), e);
            handleAccessError(locationName, folderPathString, monitorServerName, e.getMessage(), "Permission Denied",
                    -3);
        }
    }

    /**
     * Helper method to check if a file's extension matches any of the configured
     * extensions.
     *
     * @param fileName             The name of the file.
     * @param configuredExtensions The set of configured extensions (e.g., ".log",
     *                             ".txt").
     * @return true if the file matches one of the extensions or if
     *         configuredExtensions is empty, false otherwise.
     */
    private boolean isFileExtensionMatch(String fileName, Set<String> configuredExtensions) {
        if (configuredExtensions.isEmpty()) {
            return true; // If no extensions are configured, consider all files as matching
        }
        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName.toLowerCase());
        return matcher.find() && configuredExtensions.contains("." + matcher.group(1));
    }

    /**
     * Handles common logic for processing collected file names, updating counts,
     * and sending ONLY THRESHOLD alerts.
     */
    private void processCollectedFilesAndSendThresholdAlerts(IFSMonitorConfig.MonitoringConfig config,
            List<String> allMatchingFileNames,
            Logger currentLogger, Set<String> processedPathsForLocation, String stateFilePath) throws IOException {

        String locationName = config.getName();
        String folderPathString = config.getPathString();
        int minFiles = config.getMinFiles();
        int maxFiles = config.getMaxFiles();
        String monitorServerName = config.getMonitorServerName();

        int currentTotal = allMatchingFileNames.size();
        // This logic for new files and processedPathsForLocation is still needed for
        // metrics and state tracking,
        // even if we don't email about them.
        int currentNew = 0;
        List<String> detectedNewFiles = new ArrayList<>();

        for (String fileName : allMatchingFileNames) {
            String fullPathForTracking;
            if (config.isSmbPath()) {
                fullPathForTracking = String.format("smb://%s/%s/%s",
                        config.getServerIp(), config.getShareName(),
                        config.getSharePath().isEmpty() ? fileName : config.getSharePath() + "/" + fileName);
            } else {
                fullPathForTracking = config.getLocalPath().resolve(fileName).toAbsolutePath().toString();
            }

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

        totalFileCounts.get(locationName).put("all", currentTotal); // Store total count for "all" types
        newFileCounts.get(locationName).put("all", currentNew); // Store new count for "all" types

        // Update Prometheus metrics
        IFSMonitorMetrics.setFileCount(locationName, currentTotal);

        // --- Email sending logic for threshold breaches ONLY ---
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
                IFSMonitorMetrics.incrementTooFewFilesAlert(locationName);
            }
        } else if (currentTotal > maxFiles) {
            String subject = String.format(config.getAlertTooManySubject(), monitorServerName, locationName);
            String body = String.format(config.getAlertTooManyBodyPrefix(), monitorServerName, folderPathString,
                    currentTotal, maxFiles);
            currentLogger.warning(
                    String.format("Alert for '%s': File count (%d) is above maximum threshold (%d). Sending email.",
                            locationName, currentTotal, maxFiles));
            emailService.sendEmail(locationName, folderPathString, subject, body, config.getEmailImportance());
            IFSMonitorMetrics.incrementTooManyFilesAlert(locationName);
        } else {
            currentLogger
                    .info(String.format("File count for '%s' (%d) is within acceptable range [%d, %d]. No alert sent.",
                            locationName, currentTotal, minFiles, maxFiles));
        }

        // Removed the separate email sending logic for detectedNewFiles here.
        // This ensures emails are ONLY sent for threshold breaches or access errors.
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
     * Handles common error scenarios (inaccessible folder, permission denied, I/O
     * errors)
     * by logging and sending a critical email alert.
     */
    private void handleAccessError(String locationName, String folderPathString, String monitorServerName,
            String errorMessage, String errorType, int metricCode) {
        // String subject = String.format("CRITICAL ALERT on %s: IFS Folder %s - %s",
        // monitorServerName, errorType, locationName);
        // String body = String.format("Observation from server %s: A critical error
        // occurred while trying to access the IFS folder at %s: %s. Please investigate
        // immediately.", monitorServerName, folderPathString, errorMessage);

        // emailService.sendEmail(
        // locationName,
        // folderPathString,
        // subject,
        // body,
        // "High" // Always send high importance for access errors
        // );
        IFSMonitorMetrics.setFileCount(locationName, metricCode); // Indicate error state
        IFSMonitorMetrics.incrementTooFewFilesAlert(locationName); // Log as a "too few" type alert for monitoring
                                                                   // dashboards
    }
}
