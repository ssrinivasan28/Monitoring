package com.islandpacific.monitoring.ibmierrormonitoring;

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


public class IFSErrorMonitorConfig {

    private final Logger logger;
    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<MonitoringConfig> monitoringConfigs;
    private final ConcurrentHashMap<String, Logger> locationLoggers;
    private final ConcurrentHashMap<String, SmbCredentials> globalSmbCredentials;
    private String clientName; // Add this field

    private static final Pattern UNC_PATH_PATTERN = Pattern.compile("^//([^/]+)/(.+)$");

    public IFSErrorMonitorConfig(String emailPropertiesFilePath, String monitorPropertiesFilePath, Logger mainLogger) throws IOException, IllegalArgumentException {
        this.logger = mainLogger;
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.monitoringConfigs = new ArrayList<>();
        this.locationLoggers = new ConcurrentHashMap<>();
        this.globalSmbCredentials = new ConcurrentHashMap<>();
        loadProperties(emailPropertiesFilePath, monitorPropertiesFilePath);
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

    public ConcurrentHashMap<String, SmbCredentials> getGlobalSmbCredentials() {
        return globalSmbCredentials;
    }

    // Add this getter
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

            // Load global IBM i credentials
            String ibmiServerIp = monitorProps.getProperty("ibmi.server");
            String ibmiUser = monitorProps.getProperty("ibmi.user");
            String ibmiPassword = monitorProps.getProperty("ibmi.password");
            String ibmiDomain = monitorProps.getProperty("ibmi.domain", "");

            if (ibmiServerIp != null && !ibmiServerIp.isEmpty() && ibmiUser != null && !ibmiUser.isEmpty()) {
                globalSmbCredentials.put(ibmiServerIp, new SmbCredentials(ibmiUser, ibmiPassword, ibmiDomain)); 
                logger.info(String.format("Loaded global IBM i credentials for server: %s (User: %s, Domain: %s)", ibmiServerIp, ibmiUser, ibmiDomain.isEmpty() ? "N/A" : ibmiDomain));
            } else {
                logger.warning("Global IBM i server credentials (ibmi.server, ibmi.user, ibmi.password) are incomplete or missing. SMB access might fail for paths on this server.");
            }

            // Load client.name property
            this.clientName = monitorProps.getProperty("client.name", "DefaultClient"); // Default value if not found
            logger.info("Loaded client name: " + this.clientName);

            // Load monitoring configurations
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

                // Pass the logger to the MonitoringConfig constructor
                MonitoringConfig config = new MonitoringConfig(locationName, path, types, skipToEmails, emailImportance, this.logger);
                
                Path locationLogsDirectory = Paths.get("logs", config.getName());
                if (!Files.exists(locationLogsDirectory)) {
                    try {
                        Files.createDirectories(locationLogsDirectory);
                        logger.info("Created location-specific logs directory: " + locationLogsDirectory.toAbsolutePath());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Failed to create location-specific logs directory: " + locationLogsDirectory.toAbsolutePath() + ". Logging might fail.", e);
                    }
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
                logger.info("Loaded monitoring configuration for: " + config.getName() + ". Path: " + config.getPathString() + ". SMB: " + config.isSmbPath() + (config.isSmbPath() ? " (Server: " + config.getServerIp() + ", Share: " + config.getShareName() + ", Path: " + config.getSharePath() + ")" : ""));
            }

            if (monitoringConfigs.isEmpty()) {
                logger.warning("No valid monitoring locations found in " + monitorPropertiesFilePath + ". The monitor will not scan any folders.");
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
            config.getLocationLogger().info(String.format("No existing processed files state file found for location '%s' at '%s'. Starting with an empty processed file list for this location (a new file will be created if new files are detected).",
                                            config.getName(), stateFilePath.getFileName()));
        }
    }

  
    public static class SmbCredentials {
        private final String username;
        private final String password;
        private final String domain; // Optional domain for NTLM authentication

        public SmbCredentials(String username, String password, String domain) {
            this.username = username;
            this.password = password;
            this.domain = domain;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDomain() { return domain; }

        public com.hierynomus.smbj.auth.AuthenticationContext toAuthenticationContext() {
            return new com.hierynomus.smbj.auth.AuthenticationContext(username, password.toCharArray(), domain);
        }
    }

    /**
     * Represents a single monitoring configuration, including path, file types,
     * email settings, and whether it's an SMB path.
     */
    public static class MonitoringConfig {
        private final String name;
        private final String pathStr; // Original path string from properties
        private final Path localPath; // For local/OS-managed paths (if not SMB)
        private final String serverIp; // Extracted server IP for SMB paths
        private final String shareName; // Extracted share name for SMB paths
        private final String sharePath; // Path within the share for SMB paths
        private final Set<String> fileExtensions;
        private final Set<String> skipToEmails;
        private final String emailImportance;
        private transient Logger locationLogger;
        private transient Set<String> processedFilePaths;
        private final String processedFilesStateFilePath;
        private final Logger parentLogger; // Added to hold the logger from the outer class

        // Modified constructor to accept a Logger instance
        public MonitoringConfig(String name, String pathStr, String fileTypesStr, String skipToEmailsStr, String emailImportanceStr, Logger parentLogger) {
            this.name = Objects.requireNonNull(name, "Monitoring config name cannot be null");
            this.pathStr = Objects.requireNonNull(pathStr, "Monitoring config path cannot be null");
            this.parentLogger = parentLogger; // Initialize the parentLogger

            Matcher uncMatcher = UNC_PATH_PATTERN.matcher(pathStr);
            if (uncMatcher.matches()) {
                this.localPath = null; // Not a local path
                this.serverIp = uncMatcher.group(1); // Capture the server/IP
                String fullSharePath = uncMatcher.group(2); // e.g., root/IPTSFIL8/WI/DLX/DISTRO
                int firstSlash = fullSharePath.indexOf('/');
                if (firstSlash != -1) {
                    this.shareName = fullSharePath.substring(0, firstSlash); // e.g., root
                    this.sharePath = fullSharePath.substring(firstSlash + 1); // e.g., IPTSFIL8/WI/DLX/DISTRO
                } else {
                    this.shareName = fullSharePath; // Path is just the share name
                    this.sharePath = "";
                }
            } else {
                this.localPath = Paths.get(pathStr); // Treat as local/OS-managed path
                this.serverIp = null; // Not an SMB path
                this.shareName = null;
                this.sharePath = null;
            }

            this.fileExtensions = Arrays.stream(fileTypesStr.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .map(s -> "." + s.toLowerCase())
                                        .collect(Collectors.toSet());
            if (this.fileExtensions.isEmpty()) {
                // Use the passed parentLogger here
                this.parentLogger.warning("Monitoring config '" + name + "' has no file types specified. It will not monitor any files.");
            }
            this.skipToEmails = Arrays.stream(skipToEmailsStr.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toSet());
            this.emailImportance = emailImportanceStr != null ? emailImportanceStr.trim() : "";
            this.processedFilePaths = Collections.synchronizedSet(new HashSet<>());
            this.processedFilesStateFilePath = "logs/processed/processed_files_" + this.name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".txt";
        }

        public String getName() { return name; }
        public String getPathString() { return pathStr; }
        public Path getLocalPath() { return localPath; }
        public String getServerIp() { return serverIp; }
        public String getShareName() { return shareName; }
        public String getSharePath() { return sharePath; }
        public boolean isSmbPath() { return serverIp != null; }
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
                    ", isSmbPath=" + isSmbPath() +
                    ", serverIp='" + (serverIp != null ? serverIp : "N/A") + '\'' +
                    ", shareName='" + (shareName != null ? shareName : "N/A") + '\'' +
                    ", sharePath='" + (sharePath != null ? sharePath : "N/A") + '\'' +
                    ", fileExtensions=" + fileExtensions +
                    ", skipToEmails=" + skipToEmails +
                    ", emailImportance='" + emailImportance + '\'' +
                    ", processedFilesStateFilePath='" + processedFilesStateFilePath + '\'' +
                    '}';
        }
    }
}