package com.islandpacific.monitoring.filesystemerrormonitoring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service class for Windows File System Error Monitoring.
 * Monitors local Windows folders for new error files and sends email notifications.
 */
public class FileSystemErrorService {

    private final Logger mainLogger;
    private final List<FileSystemErrorConfig.MonitoringConfig> monitoringConfigs;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private final FileSystemErrorMetrics metricsService;

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.([a-zA-Z0-9]{1,5})(?:\\s.*)?$");
    private static final String LAST_RUN_LOG = "logs/fs_error_last_run_timestamp.txt";

    // Index for O(1) lookup by name — avoids linear search per email send
    private final Map<String, FileSystemErrorConfig.MonitoringConfig> configByName;

    public FileSystemErrorService(Logger mainLogger,
                                  List<FileSystemErrorConfig.MonitoringConfig> monitoringConfigs,
                                  EmailService emailService,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts,
                                  FileSystemErrorMetrics metricsService) {
        this.mainLogger = mainLogger;
        this.monitoringConfigs = monitoringConfigs;
        this.emailService = emailService;
        this.totalFileCounts = totalFileCounts;
        this.newFileCounts = newFileCounts;
        this.metricsService = metricsService;
        this.configByName = new HashMap<>();
        for (FileSystemErrorConfig.MonitoringConfig cfg : monitoringConfigs) {
            configByName.put(cfg.getName(), cfg);
        }
    }

    /**
     * Checks for new error files across all configured locations and sends email notifications.
     */
    public void checkNewFilesAndSendEmail() {
        // Do not clear — update in-place so Prometheus metrics are never zeroed mid-scan

        Map<String, Map<String, List<String>>> newFilesForEmail = new ConcurrentHashMap<>();

        for (FileSystemErrorConfig.MonitoringConfig config : monitoringConfigs) {
            String locationName = config.getName();
            Logger currentLogger = config.getLocationLogger();
            Set<String> processedPathsForLocation = config.getProcessedFilePaths();
            String stateFilePath = config.getProcessedFilesStateFilePath();

            currentLogger.info(String.format("Scanning Windows location '%s' at path '%s' for extensions %s.",
                                locationName, config.getPathString(), config.getFileExtensions()));

            totalFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());
            newFileCounts.putIfAbsent(locationName, new ConcurrentHashMap<>());

            List<String> allMatchingFileNames = new ArrayList<>();
            
            try {
                Path folderPath = config.getLocalPath();
                if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                    currentLogger.warning("Windows monitoring folder does not exist or is not a directory: " + folderPath);
                    config.getFileExtensions().forEach(ext -> {
                        totalFileCounts.get(locationName).put(ext, 0);
                        newFileCounts.get(locationName).put(ext, 0);
                    });
                    continue;
                }

                // Scan only immediate directory (no recursive)
                try (Stream<Path> walk = Files.walk(folderPath, 1)) {
                    walk.filter(Files::isRegularFile)
                        .filter(p -> {
                            String fileName = p.getFileName().toString().toLowerCase();
                            Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName);
                            if (matcher.find()) {
                                String detectedExt = "." + matcher.group(1);
                                return config.getFileExtensions().contains(detectedExt);
                            }
                            return false;
                        })
                        .map(p -> p.toAbsolutePath().toString())
                        .forEach(allMatchingFileNames::add);
                }

                Map<String, List<String>> filesByExtension = allMatchingFileNames.stream()
                    .collect(Collectors.groupingBy(filePath -> {
                        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
                        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName);
                        if (matcher.find()) {
                            return "." + matcher.group(1);
                        }
                        return "unknown";
                    }));

                for (String configuredExt : config.getFileExtensions()) {
                    List<String> filesForExt = filesByExtension.getOrDefault(configuredExt, Collections.emptyList());

                    int currentTotal = filesForExt.size();
                    int currentNew = 0;
                    List<String> detectedNewFiles = new ArrayList<>();

                    for (String fullFilePath : filesForExt) {
                        if (!processedPathsForLocation.contains(fullFilePath)) {
                            String fileName = Paths.get(fullFilePath).getFileName().toString();
                            detectedNewFiles.add(fileName);
                            processedPathsForLocation.add(fullFilePath);
                            currentNew++;
                            currentLogger.info(String.format("New error file detected (type '%s'): %s",
                                            configuredExt.substring(1), fileName));
                            
                            try {
                                Files.write(Paths.get(stateFilePath),
                                                (fullFilePath + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                currentLogger.fine("Appended processed file path to state file: " + fullFilePath);
                            } catch (IOException e) {
                                currentLogger.log(Level.SEVERE, "Error writing processed file path to state file for location " + locationName + ": " + fullFilePath, e);
                            }
                        }
                    }

                    totalFileCounts.get(locationName).put(configuredExt, currentTotal);
                    newFileCounts.get(locationName).put(configuredExt, currentNew);

                    if (!detectedNewFiles.isEmpty()) {
                        newFilesForEmail.computeIfAbsent(locationName, k -> new ConcurrentHashMap<>())
                                         .computeIfAbsent(configuredExt, k -> new ArrayList<>())
                                         .addAll(detectedNewFiles);
                    }
                }
                currentLogger.info(String.format("Scan summary: Total matching files = %d, New files = %d",
                                allMatchingFileNames.size(),
                                newFilesForEmail.getOrDefault(locationName, Collections.emptyMap()).values().stream().mapToInt(List::size).sum()));

            } catch (IOException e) {
                currentLogger.log(Level.SEVERE, "Error during file listing for " + config.getPathString() + ": " + e.getMessage(), e);
                config.getFileExtensions().forEach(ext -> {
                    totalFileCounts.get(locationName).put(ext, 0);
                    newFileCounts.get(locationName).put(ext, 0);
                });
            }
        }

        long currentScanTimestamp = Instant.now().getEpochSecond();
        metricsService.setLastScanTimestamp(currentScanTimestamp);
        try {
            Files.writeString(Paths.get(LAST_RUN_LOG), String.valueOf(currentScanTimestamp), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            mainLogger.fine("Updated last run timestamp log.");
        } catch (IOException e) {
            mainLogger.warning("Failed to write last run timestamp to " + LAST_RUN_LOG + ": " + e.getMessage());
        }

        if (newFilesForEmail.isEmpty()) {
            mainLogger.info("No new error files detected across all monitored Windows locations, no emails sent.");
        } else {
            for (Map.Entry<String, Map<String, List<String>>> locationEntry : newFilesForEmail.entrySet()) {
                String locationName = locationEntry.getKey();
                Map<String, List<String>> newFilesForThisLocation = locationEntry.getValue();

                FileSystemErrorConfig.MonitoringConfig currentConfig = monitoringConfigs.stream()
                                                                  .filter(cfg -> cfg.getName().equals(locationName))
                                                                  .findFirst()
                                                                  .orElse(null);

                if (currentConfig != null) {
                    emailService.sendEmail(currentConfig, newFilesForThisLocation);
                } else {
                    mainLogger.warning("MonitoringConfig not found for location: " + locationName + ". Cannot send email.");
                }
            }
        }
    }
}
