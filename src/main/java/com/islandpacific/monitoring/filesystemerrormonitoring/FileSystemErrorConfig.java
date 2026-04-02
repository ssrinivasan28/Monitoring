package com.islandpacific.monitoring.filesystemerrormonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Configuration class for Windows File System Error Monitor.
 * Monitors local Windows folders for new error files and sends email notifications.
 */
public class FileSystemErrorConfig {

    private final Logger logger;
    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<MonitoringConfig> monitoringConfigs;
    private final ConcurrentHashMap<String, Logger> locationLoggers;
    private String clientName;

    public FileSystemErrorConfig(String emailPropertiesFilePath, String monitorPropertiesFilePath, Logger mainLogger) throws IOException, IllegalArgumentException {
        this.logger = mainLogger;
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.monitoringConfigs = new ArrayList<>();
        this.locationLoggers = new ConcurrentHashMap<>();
        loadProperties(emailPropertiesFilePath, monitorPropertiesFilePath);
    }

    /**
     * Constructor that takes pre-loaded Properties object.
     */
    public FileSystemErrorConfig(Properties monitorProps) {
        this.logger = Logger.getLogger(FileSystemErrorConfig.class.getName());
        this.emailProps = new Properties();
        this.monitorProps = monitorProps;
        this.monitoringConfigs = new ArrayList<>();
        this.locationLoggers = new ConcurrentHashMap<>();
        parseMonitoringConfigs();
    }

    public Properties getEmailProps() {
        return emailProps;
    }

    public Properties getMonitorProps() {
        return monitorProps;
    }

    public List<MonitoringConfig> getMonitoringConfigs() {
        return monitoringConfigs;
    }

    public ConcurrentHashMap<String, Logger> getLocationLoggers() {
        return locationLoggers;
    }

    public String getClientName() {
        return clientName;
    }

    private void loadProperties(String emailPropertiesFilePath, String monitorPropertiesFilePath) throws IOException, IllegalArgumentException {
        try (InputStream emailIn = new FileInputStream(emailPropertiesFilePath);
             InputStream monitorIn = new FileInputStream(monitorPropertiesFilePath)) {
            emailProps.load(emailIn);
            monitorProps.load(monitorIn);
            parseMonitoringConfigs();
        } catch (IOException e) {
            logger.severe(String.format("Failed to load properties due to I/O error: %s", e.getMessage()));
            throw new IllegalArgumentException("Error reading properties files: " + e.getMessage());
        }
    }

    private void parseMonitoringConfigs() {
        Path processedLogsDirectory = Paths.get("logs", "processed");
        if (!Files.exists(processedLogsDirectory)) {
            try {
                Files.createDirectories(processedLogsDirectory);
                logger.info("Created 'logs/processed' directory for state files: " + processedLogsDirectory.toAbsolutePath());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create 'logs/processed' directory: " + processedLogsDirectory.toAbsolutePath() + ". State file tracking might fail.", e);
            }
        }

        // Load client.name property - check email props first, then monitor props
        String clientNameFromEmail = emailProps.getProperty("mail.clientName", "");
        this.clientName = (!clientNameFromEmail.isEmpty()) ? clientNameFromEmail : 
                         monitorProps.getProperty("client.name", "DefaultClient");
        logger.info("Loaded client name: " + this.clientName);

        // Load monitoring configurations
        // Pattern: monitor.location.<locationName>.<property> (same as IBM error monitor)
        Pattern locationPattern = Pattern.compile("^monitor\\.location\\.([^.]+)\\.(path|types|skip\\.to\\.emails|email\\.importance)$");
        Map<String, String> locationPaths = new HashMap<>();
        Map<String, String> locationTypes = new HashMap<>();
        Map<String, String> locationSkipToEmails = new HashMap<>();
        Map<String, String> locationEmailImportance = new HashMap<>();

        for (String key : monitorProps.stringPropertyNames()) {
            Matcher matcher = locationPattern.matcher(key);
            if (matcher.matches()) {
                String locationName = matcher.group(1);
                String propertyType = matcher.group(2);

                if ("path".equals(propertyType)) {
                    locationPaths.put(locationName, monitorProps.getProperty(key));
                } else if ("types".equals(propertyType)) {
                    locationTypes.put(locationName, monitorProps.getProperty(key));
                } else if ("skip.to.emails".equals(propertyType)) {
                    locationSkipToEmails.put(locationName, monitorProps.getProperty(key));
                } else if ("email.importance".equals(propertyType)) {
                    locationEmailImportance.put(locationName, monitorProps.getProperty(key));
                }
            }
        }

        for (String locationName : locationPaths.keySet()) {
            String path = locationPaths.get(locationName);
            String types = locationTypes.getOrDefault(locationName, "");
            String skipToEmails = locationSkipToEmails.getOrDefault(locationName, "");
            String emailImportance = locationEmailImportance.getOrDefault(locationName, "");

            if (path == null || path.trim().isEmpty()) {
                logger.warning("Monitoring location '" + locationName + "' has a missing or empty 'path'. Skipping this configuration.");
                continue;
            }
            if (types.trim().isEmpty()) {
                logger.warning("Monitoring location '" + locationName + "' has a missing or empty 'types'. Skipping this configuration.");
                continue;
            }

            try {
                MonitoringConfig config = new MonitoringConfig(locationName, path, types, skipToEmails, emailImportance, this.logger);
                
                Path locationLogsDirectory = Paths.get("logs", config.getName());
                if (!Files.exists(locationLogsDirectory)) {
                    Files.createDirectories(locationLogsDirectory);
                    logger.info("Created location-specific logs directory: " + locationLogsDirectory.toAbsolutePath());
                }

                Logger perLocationLogger = Logger.getLogger("location." + locationName);
                perLocationLogger.setUseParentHandlers(false);
                String logFileName = String.format("%s/%s_%s.log", locationLogsDirectory, config.getName(), DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()));
                FileHandler locationHandler = new FileHandler(logFileName, true);
                locationHandler.setFormatter(new SimpleFormatter());
                perLocationLogger.addHandler(locationHandler);
                perLocationLogger.setLevel(Level.INFO);

                config.setLocationLogger(perLocationLogger);
                locationLoggers.put(locationName, perLocationLogger);

                loadProcessedFilesForLocation(config);

                monitoringConfigs.add(config);
                logger.info("Loaded Windows error monitoring configuration for: " + config.getName() + ". Path: " + config.getPathString() + ". File Types: " + config.getFileExtensions());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create logs for location " + locationName + ": " + e.getMessage(), e);
            }
        }

        if (monitoringConfigs.isEmpty()) {
            logger.warning("No valid Windows error monitoring locations found. The monitor will not scan any folders.");
        }

        logger.info("Configuration parsing completed.");
    }

    private void loadProcessedFilesForLocation(MonitoringConfig config) {
        Path stateFilePath = Paths.get(config.getProcessedFilesStateFilePath());

        if (Files.exists(stateFilePath) && Files.isRegularFile(stateFilePath)) {
            try {
                List<String> lines = Files.readAllLines(stateFilePath, StandardCharsets.UTF_8);
                config.getProcessedFilePaths().addAll(lines);
                config.getLocationLogger().info(String.format("Loaded %d previously processed files for location '%s' from state file: %s",
                                                config.getProcessedFilePaths().size(), config.getName(), stateFilePath.getFileName()));
            } catch (IOException e) {
                config.getLocationLogger().log(Level.WARNING, String.format("Error reading processed files state file '%s' for location '%s': %s",
                                                     stateFilePath.getFileName(), config.getName(), e.getMessage()), e);
            }
        } else {
            config.getLocationLogger().info(String.format("No existing processed files state file found for location '%s' at '%s'. Starting with an empty processed file list.",
                                            config.getName(), stateFilePath.getFileName()));
        }
    }

    /**
     * Represents a single monitoring configuration for Windows error file monitoring.
     */
    public static class MonitoringConfig {
        private final String name;
        private final String pathStr;
        private final Path localPath;
        private final Set<String> fileExtensions;
        private final Set<String> skipToEmails;
        private final String emailImportance;
        private transient Logger locationLogger;
        private transient Set<String> processedFilePaths;
        private final String processedFilesStateFilePath;
        private final Logger parentLogger;

        public MonitoringConfig(String name, String pathStr, String fileTypesStr, String skipToEmailsStr, String emailImportanceStr, Logger parentLogger) {
            this.name = Objects.requireNonNull(name, "Monitoring config name cannot be null");
            this.pathStr = Objects.requireNonNull(pathStr, "Monitoring config path cannot be null");
            this.parentLogger = parentLogger;
            this.localPath = Paths.get(pathStr);

            this.fileExtensions = Arrays.stream(fileTypesStr.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .map(s -> "." + s.toLowerCase())
                                        .collect(Collectors.toSet());
            if (this.fileExtensions.isEmpty()) {
                this.parentLogger.warning("Monitoring config '" + name + "' has no file types specified. It will not monitor any files.");
            }
            this.skipToEmails = Arrays.stream(skipToEmailsStr.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toSet());
            this.emailImportance = emailImportanceStr != null ? emailImportanceStr.trim() : "";
            this.processedFilePaths = Collections.synchronizedSet(new HashSet<>());
            this.processedFilesStateFilePath = "logs/processed/fs_error_" + this.name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".txt";
        }

        public String getName() { return name; }
        public String getPathString() { return pathStr; }
        public Path getLocalPath() { return localPath; }
        public Set<String> getFileExtensions() { return fileExtensions; }
        public Set<String> getSkipToEmails() { return skipToEmails; }
        public String getEmailImportance() { return emailImportance; }
        public Logger getLocationLogger() { return locationLogger; }
        public void setLocationLogger(Logger locationLogger) { this.locationLogger = locationLogger; }
        public Set<String> getProcessedFilePaths() { return processedFilePaths; }
        public String getProcessedFilesStateFilePath() { return processedFilesStateFilePath; }

        @Override
        public String toString() {
            return "MonitoringConfig{" +
                    "name='" + name + '\'' +
                    ", pathStr='" + pathStr + '\'' +
                    ", fileExtensions=" + fileExtensions +
                    ", skipToEmails=" + skipToEmails +
                    ", emailImportance='" + emailImportance + '\'' +
                    ", processedFilesStateFilePath='" + processedFilesStateFilePath + '\'' +
                    '}';
        }
    }
}
