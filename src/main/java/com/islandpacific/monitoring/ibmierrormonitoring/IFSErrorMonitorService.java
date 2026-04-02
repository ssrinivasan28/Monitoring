package com.islandpacific.monitoring.ibmierrormonitoring;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
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


public class IFSErrorMonitorService {

    private final Logger mainLogger;
    private final List<IFSErrorMonitorConfig.MonitoringConfig> monitoringConfigs;
    private final ConcurrentHashMap<String, IFSErrorMonitorConfig.SmbCredentials> globalSmbCredentials;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts;
    private final IFSErrorMonitorMetrics metricsService;

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.([a-zA-Z0-9]{1,5})(?:\\s.*)?$");
    private static final String LAST_RUN_LOG = "logs/last_run_timestamp.txt";


    public IFSErrorMonitorService(Logger mainLogger,
                                  List<IFSErrorMonitorConfig.MonitoringConfig> monitoringConfigs,
                                  ConcurrentHashMap<String, IFSErrorMonitorConfig.SmbCredentials> globalSmbCredentials,
                                  EmailService emailService,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> totalFileCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> newFileCounts,
                                  IFSErrorMonitorMetrics metricsService) {
        this.mainLogger = mainLogger;
        this.monitoringConfigs = monitoringConfigs;
        this.globalSmbCredentials = globalSmbCredentials;
        this.emailService = emailService;
        this.totalFileCounts = totalFileCounts;
        this.newFileCounts = newFileCounts;
        this.metricsService = metricsService;
    }

  
    public void checkNewFilesAndSendEmail() {
        newFileCounts.clear();
        totalFileCounts.clear();

        Map<String, Map<String, List<String>>> newFilesForEmail = new ConcurrentHashMap<>();

        for (IFSErrorMonitorConfig.MonitoringConfig config : monitoringConfigs) {
            String locationName = config.getName();
            Logger currentLogger = config.getLocationLogger();
            Set<String> processedPathsForLocation = config.getProcessedFilePaths();
            String stateFilePath = config.getProcessedFilesStateFilePath();

            currentLogger.info(String.format("Scanning location '%s' at path '%s' for extensions %s.",
                                locationName, config.getPathString(), config.getFileExtensions()));

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
                        IFSErrorMonitorConfig.SmbCredentials creds = globalSmbCredentials.get(server);
                        if (creds != null) {
                            ac = creds.toAuthenticationContext();
                            currentLogger.info(String.format("Using provided SMB credentials for server %s for location '%s'.", server, locationName));
                        } else {
                            currentLogger.warning(String.format("SMB path '%s' for location '%s' has no global credentials defined for server %s. Attempting anonymous access, which may fail.", config.getPathString(), locationName, server));
                        }

                        com.hierynomus.smbj.session.Session smbjSession = connection.authenticate(ac);
                        try (DiskShare diskShare = (DiskShare) smbjSession.connectShare(share)) {
                            for (FileIdBothDirectoryInformation fileInfo : diskShare.list(pathInShare)) {
                                // FILE_ATTRIBUTE_DIRECTORY has a value of 16 (0x10)
                                if ((fileInfo.getFileAttributes() & 16L) == 0) { // If the directory bit is NOT set, it's a file
                                    String fileName = fileInfo.getFileName();
                                    Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName.toLowerCase());
                                    if (matcher.find()) {
                                        String detectedExt = "." + matcher.group(1);
                                        if (config.getFileExtensions().contains(detectedExt)) {
                                            allMatchingFileNames.add(fileName);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SMBException e) {
                        currentLogger.log(Level.SEVERE, String.format("SMB access denied or error for location '%s' (%s): %s. Please check credentials and network access.", locationName, config.getPathString(), e.getMessage()), e);
                        config.getFileExtensions().forEach(ext -> {
                            totalFileCounts.get(locationName).put(ext, 0);
                            newFileCounts.get(locationName).put(ext, 0);
                        });
                        continue;
                    } catch (IOException e) {
                        currentLogger.log(Level.SEVERE, String.format("Error during SMB client operation for location '%s' (%s): %s", locationName, config.getPathString(), e.getMessage()), e);
                        config.getFileExtensions().forEach(ext -> {
                            totalFileCounts.get(locationName).put(ext, 0);
                            newFileCounts.get(locationName).put(ext, 0);
                        });
                        continue;
                    } finally {
                        client.close();
                    }

                } else {
                    // Handle local/OS-managed path using java.nio.file
                    Path folderPath = config.getLocalPath();
                    if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                        currentLogger.warning("Local monitoring folder does not exist or is not a directory: " + folderPath);
                        config.getFileExtensions().forEach(ext -> {
                            totalFileCounts.get(locationName).put(ext, 0);
                            newFileCounts.get(locationName).put(ext, 0);
                        });
                        continue;
                    }

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
                            .map(p -> p.getFileName().toString())
                            .forEach(allMatchingFileNames::add);
                    }
                }

                Map<String, List<String>> filesByExtension = allMatchingFileNames.stream()
                    .collect(Collectors.groupingBy(fileName -> {
                        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(fileName.toLowerCase());
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

                    for (String fileName : filesForExt) {
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
                                            configuredExt.substring(1), fileName));
                            
                            try {
                                Files.write(Paths.get(stateFilePath),
                                                (fullPathForTracking + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                currentLogger.fine("Appended processed file path to state file: " + fullPathForTracking);
                            } catch (IOException e) {
                                currentLogger.log(Level.SEVERE, "Error writing processed file path to state file for location " + locationName + ": " + fullPathForTracking, e);
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
        metricsService.setLastScanTimestamp(currentScanTimestamp); // Update metrics service
        try {
            Files.writeString(Paths.get(LAST_RUN_LOG), String.valueOf(currentScanTimestamp), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            mainLogger.fine("Updated last run timestamp log.");
        } catch (IOException e) {
            mainLogger.warning("Failed to write last run timestamp to " + LAST_RUN_LOG + ": " + e.getMessage());
        }

        if (newFilesForEmail.isEmpty()) {
            mainLogger.info("No new files detected across all monitored locations, no emails sent.");
        } else {
            for (Map.Entry<String, Map<String, List<String>>> locationEntry : newFilesForEmail.entrySet()) {
                String locationName = locationEntry.getKey();
                Map<String, List<String>> newFilesForThisLocation = locationEntry.getValue();

                IFSErrorMonitorConfig.MonitoringConfig currentConfig = monitoringConfigs.stream()
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
