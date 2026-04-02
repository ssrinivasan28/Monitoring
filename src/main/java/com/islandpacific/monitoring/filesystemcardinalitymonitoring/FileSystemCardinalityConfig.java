package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

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
 * Configuration class for Windows File System Cardinality Monitor.
 * Monitors local Windows folders for file count thresholds.
 */
public class FileSystemCardinalityConfig {

    private final Logger logger;
    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<MonitoringConfig> monitorConfigs;
    private final ConcurrentHashMap<String, Logger> locationLoggers;
    private String clientName;

    /**
     * Constructor that loads from two separate properties files.
     */
    public FileSystemCardinalityConfig(String emailPropertiesFilePath, String monitorPropertiesFilePath, Logger mainLogger) throws IOException, IllegalArgumentException {
        this.logger = mainLogger;
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.monitorConfigs = new ArrayList<>();
        this.locationLoggers = new ConcurrentHashMap<>();
        loadProperties(emailPropertiesFilePath, monitorPropertiesFilePath);
    }

    public Properties getEmailProps() {
        return emailProps;
    }

    public Properties getMonitorProps() {
        return monitorProps;
    }

    public List<MonitoringConfig> getMonitorConfigs() {
        return monitorConfigs;
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
            // Pattern: fs.location.<locationCode>.<property>
            Pattern locationPattern = Pattern.compile("^fs\\.location\\.([^.]+)\\.(name|path|min\\.files|max\\.files|alert\\.too\\.few\\.subject|alert\\.too\\.few\\.body\\.prefix|alert\\.too\\.many\\.subject|alert\\.too\\.many\\.body\\.prefix|email\\.importance|ignore\\.zero\\.file\\.alert|file\\.types|recursive)$");
            Map<String, Map<String, String>> locationsConfig = new HashMap<>();

            for (String key : monitorProps.stringPropertyNames()) {
                Matcher matcher = locationPattern.matcher(key);
                if (matcher.matches()) {
                    String locationCode = matcher.group(1);
                    String propertyType = matcher.group(2);
                    locationsConfig.computeIfAbsent(locationCode, k -> new HashMap<>()).put(propertyType, monitorProps.getProperty(key));
                }
            }

            for (Map.Entry<String, Map<String, String>> entry : locationsConfig.entrySet()) {
                String locationCode = entry.getKey();
                Map<String, String> props = entry.getValue();

                try {
                    String name = props.get("name");
                    String pathStr = props.get("path");
                    int minFiles = Integer.parseInt(props.getOrDefault("min.files", "0"));
                    int maxFiles = Integer.parseInt(props.getOrDefault("max.files", String.valueOf(Integer.MAX_VALUE)));
                    String alertTooFewSubject = props.getOrDefault("alert.too.few.subject", "[%s] File Count Below Minimum - %s");
                    String alertTooFewBodyPrefix = props.getOrDefault("alert.too.few.body.prefix", "Observation from server %s: Folder %s has %d files, below minimum threshold of %d.");
                    String alertTooManySubject = props.getOrDefault("alert.too.many.subject", "[%s] File Count Above Maximum - %s");
                    String alertTooManyBodyPrefix = props.getOrDefault("alert.too.many.body.prefix", "Observation from server %s: Folder %s has %d files, above maximum threshold of %d.");
                    String emailImportance = props.getOrDefault("email.importance", "Normal");
                    boolean ignoreZeroFileAlert = Boolean.parseBoolean(props.getOrDefault("ignore.zero.file.alert", "false"));
                    // Use clientName which is loaded from email.properties or monitor.properties
                    String monitorServerName = this.clientName;
                    String fileTypesStr = props.getOrDefault("file.types", "");
                    boolean recursive = Boolean.parseBoolean(props.getOrDefault("recursive", "false"));

                    if (name == null || name.trim().isEmpty() || pathStr == null || pathStr.trim().isEmpty()) {
                        logger.warning("Skipping folder location '" + locationCode + "' due to missing 'name' or 'path' property.");
                        continue;
                    }
                    if (minFiles < 0 || maxFiles < 0 || minFiles > maxFiles) {
                        logger.warning(String.format("Skipping folder location '%s' due to invalid min/max file configuration (min: %d, max: %d).", locationCode, minFiles, maxFiles));
                        continue;
                    }

                    MonitoringConfig config = new MonitoringConfig(
                        name, pathStr, minFiles, maxFiles,
                        alertTooFewSubject, alertTooFewBodyPrefix,
                        alertTooManySubject, alertTooManyBodyPrefix,
                        emailImportance, ignoreZeroFileAlert, monitorServerName, this.logger, fileTypesStr, recursive
                    );

                    // Setup per-location logger
                    Path locationLogsDirectory = Paths.get("logs", config.getName());
                    if (!Files.exists(locationLogsDirectory)) {
                        try {
                            Files.createDirectories(locationLogsDirectory);
                            logger.info("Created location-specific logs directory: " + locationLogsDirectory.toAbsolutePath());
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Failed to create location-specific logs directory: " + locationLogsDirectory.toAbsolutePath(), e);
                        }
                    }

                    Logger perLocationLogger = Logger.getLogger("location." + name);
                    perLocationLogger.setUseParentHandlers(false);
                    String logFileName = String.format("%s/%s_%s.log", locationLogsDirectory, config.getName(), DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()));
                    FileHandler locationHandler = new FileHandler(logFileName, true);
                    locationHandler.setFormatter(new SimpleFormatter());
                    perLocationLogger.addHandler(locationHandler);
                    perLocationLogger.setLevel(Level.INFO);

                    config.setLocationLogger(perLocationLogger);
                    locationLoggers.put(name, perLocationLogger);

                    loadProcessedFilesForLocation(config);

                    monitorConfigs.add(config);
                    logger.info("Loaded Windows folder monitoring configuration for: " + config.getName() + " (Path: " + config.getPathString() + ", Min: " + config.getMinFiles() + ", Max: " + config.getMaxFiles() + ", Ignore 0-file alert: " + ignoreZeroFileAlert + ", Recursive: " + recursive + ", File Types: " + config.getFileExtensions() + ")");

                } catch (NumberFormatException e) {
                    logger.warning("Invalid number format for min/max files in folder location '" + locationCode + "': " + e.getMessage());
                } catch (Exception e) {
                    logger.warning("Error processing folder location '" + locationCode + "': " + e.getMessage());
                }
            }

            if (monitorConfigs.isEmpty()) {
                logger.warning("No valid folder monitoring locations found in " + monitorPropertiesFilePath + ". The monitor will not scan any folders.");
            }

            logger.info("Properties loaded successfully.");

        } catch (IOException e) {
            logger.severe(String.format("Failed to load properties due to I/O error: %s", e.getMessage()));
            throw new IllegalArgumentException("Error reading properties files: " + e.getMessage());
        }
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
     * Represents a single monitoring configuration for Windows folder monitoring.
     */
    public static class MonitoringConfig {
        private final String name;
        private final String pathStr;
        private final Path localPath;
        private final Set<String> fileExtensions;
        private final int minFiles;
        private final int maxFiles;
        private final String alertTooFewSubject;
        private final String alertTooFewBodyPrefix;
        private final String alertTooManySubject;
        private final String alertTooManyBodyPrefix;
        private final String emailImportance;
        private final boolean ignoreZeroFileAlert;
        private final String monitorServerName;
        private final boolean recursive;
        private transient Logger locationLogger;
        private transient Set<String> processedFilePaths;
        private final String processedFilesStateFilePath;
        private final Logger parentLogger;

        public MonitoringConfig(String name, String pathStr, int minFiles, int maxFiles,
                                String alertTooFewSubject, String alertTooFewBodyPrefix,
                                String alertTooManySubject, String alertTooManyBodyPrefix,
                                String emailImportance, boolean ignoreZeroFileAlert,
                                String monitorServerName, Logger parentLogger, String fileTypesStr, boolean recursive) {
            this.name = Objects.requireNonNull(name, "Monitoring config name cannot be null");
            this.pathStr = Objects.requireNonNull(pathStr, "Monitoring config path cannot be null");
            this.parentLogger = parentLogger;
            this.localPath = Paths.get(pathStr);
            this.recursive = recursive;

            // Validate and assign min/max files
            if (minFiles < 0 || maxFiles < 0 || minFiles > maxFiles) {
                this.parentLogger.warning("Invalid min/max files for config '" + name + "': min=" + minFiles + ", max=" + maxFiles + ". Setting to default [0, Integer.MAX_VALUE].");
                this.minFiles = 0;
                this.maxFiles = Integer.MAX_VALUE;
            } else {
                this.minFiles = minFiles;
                this.maxFiles = maxFiles;
            }

            this.alertTooFewSubject = Objects.requireNonNull(alertTooFewSubject, "Too few subject cannot be null");
            this.alertTooFewBodyPrefix = Objects.requireNonNull(alertTooFewBodyPrefix, "Too few body prefix cannot be null");
            this.alertTooManySubject = Objects.requireNonNull(alertTooManySubject, "Too many subject cannot be null");
            this.alertTooManyBodyPrefix = Objects.requireNonNull(alertTooManyBodyPrefix, "Too many body prefix cannot be null");
            this.emailImportance = emailImportance != null ? emailImportance.trim() : "Normal";
            this.ignoreZeroFileAlert = ignoreZeroFileAlert;
            this.monitorServerName = Objects.requireNonNull(monitorServerName, "Monitor server name cannot be null");
            this.processedFilePaths = Collections.synchronizedSet(new HashSet<>());
            this.processedFilesStateFilePath = "logs/processed/fs_cardinality_" + this.name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".txt";

            this.fileExtensions = new HashSet<>();
            if (fileTypesStr != null && !fileTypesStr.trim().isEmpty()) {
                this.fileExtensions.addAll(Arrays.stream(fileTypesStr.split(","))
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .map(s -> "." + s.toLowerCase())
                                                .collect(Collectors.toSet()));
            }
            if (this.fileExtensions.isEmpty()) {
                this.parentLogger.info("Monitoring config '" + name + "' has no specific file types. It will monitor all regular files.");
            }
        }

        public String getName() { return name; }
        public String getPathString() { return pathStr; }
        public Path getLocalPath() { return localPath; }
        public Set<String> getFileExtensions() { return fileExtensions; }
        public int getMinFiles() { return minFiles; }
        public int getMaxFiles() { return maxFiles; }
        public String getAlertTooFewSubject() { return alertTooFewSubject; }
        public String getAlertTooFewBodyPrefix() { return alertTooFewBodyPrefix; }
        public String getAlertTooManySubject() { return alertTooManySubject; }
        public String getAlertTooManyBodyPrefix() { return alertTooManyBodyPrefix; }
        public String getEmailImportance() { return emailImportance; }
        public boolean isIgnoreZeroFileAlert() { return ignoreZeroFileAlert; }
        public String getMonitorServerName() { return monitorServerName; }
        public boolean isRecursive() { return recursive; }
        public Logger getLocationLogger() { return locationLogger; }
        public void setLocationLogger(Logger locationLogger) { this.locationLogger = locationLogger; }
        public Set<String> getProcessedFilePaths() { return processedFilePaths; }
        public String getProcessedFilesStateFilePath() { return processedFilesStateFilePath; }

        @Override
        public String toString() {
            return "MonitoringConfig{" +
                   "name='" + name + '\'' +
                   ", pathStr='" + pathStr + '\'' +
                   ", minFiles=" + minFiles +
                   ", maxFiles=" + maxFiles +
                   ", emailImportance='" + emailImportance + '\'' +
                   ", ignoreZeroFileAlert=" + ignoreZeroFileAlert +
                   ", monitorServerName='" + monitorServerName + '\'' +
                   ", recursive=" + recursive +
                   ", fileExtensions=" + fileExtensions +
                   '}';
        }
    }
}
