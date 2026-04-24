package com.islandpacific.monitoring.ibmiifsmonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class IFSMonitorConfig {

    private final Logger logger;
    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<MonitoringConfig> monitorConfigs; // Renamed from monitoringConfigs for consistency
    private final ConcurrentHashMap<String, Logger> locationLoggers;
    private final ConcurrentHashMap<String, SmbCredentials> globalSmbCredentials;

    private static final Pattern UNC_PATH_PATTERN = Pattern.compile("^//([^/]+)/([^/]+)(?:/(.*))?$"); // Updated pattern to correctly capture share and path within share

    public IFSMonitorConfig(String emailPropertiesFilePath, String monitorPropertiesFilePath, Logger mainLogger) throws IOException, IllegalArgumentException {
        this.logger = mainLogger;
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.monitorConfigs = new ArrayList<>();
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

    public List<MonitoringConfig> getMonitorConfigs() {
        return monitorConfigs;
    }

    public ConcurrentHashMap<String, Logger> getLocationLoggers() {
        return locationLoggers;
    }

    public ConcurrentHashMap<String, SmbCredentials> getGlobalSmbCredentials() {
        return globalSmbCredentials;
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

            // Load global IBM i (SMB) credentials
            String ibmiServerIp = monitorProps.getProperty("ibmi.server");
            String ibmiUser = monitorProps.getProperty("ibmi.user");
            String ibmiPassword = monitorProps.getProperty("ibmi.password");
            String ibmiDomain = monitorProps.getProperty("ibmi.domain", ""); // Optional domain for NTLM

            if (ibmiServerIp != null && !ibmiServerIp.isEmpty() && ibmiUser != null && !ibmiUser.isEmpty()) {
                globalSmbCredentials.put(ibmiServerIp, new SmbCredentials(ibmiUser, ibmiPassword, ibmiDomain)); 
                logger.info(String.format("Loaded global IBM i credentials for server: %s (User: %s, Domain: %s)", ibmiServerIp, ibmiUser, ibmiDomain.isEmpty() ? "N/A" : ibmiDomain));
            } else {
                logger.warning("Global IBM i server credentials (ibmi.server, ibmi.user, ibmi.password) are incomplete or missing. SMB access might fail for paths on this server.");
            }

            // Load monitoring configurations
            Pattern locationPattern = Pattern.compile("^ifs\\.location\\.([^.]+)\\.(name|path|min\\.files|max\\.files|alert\\.too\\.few\\.subject|alert\\.too\\.few\\.body\\.prefix|alert\\.too\\.many\\.subject|alert\\.too\\.many\\.body\\.prefix|email\\.importance|ignore\\.zero\\.file\\.alert|file\\.types)$"); // Added file.types
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
                    String alertTooFewSubject = props.get("alert.too.few.subject");
                    String alertTooFewBodyPrefix = props.get("alert.too.few.body.prefix");
                    String alertTooManySubject = props.get("alert.too.many.subject");
                    String alertTooManyBodyPrefix = props.get("alert.too.many.body.prefix");
                    String emailImportance = props.getOrDefault("email.importance", "Normal");
                    boolean ignoreZeroFileAlert = Boolean.parseBoolean(props.getOrDefault("ignore.zero.file.alert", "false"));
                    // Use client.name for consistency with other monitors (fallback to monitor.server for backward compatibility)
                    String clientName = monitorProps.getProperty("client.name", monitorProps.getProperty("monitor.server", "DefaultClient"));
                    String fileTypesStr = props.getOrDefault("file.types", ""); // Get file types string

                    if (name == null || name.trim().isEmpty() || pathStr == null || pathStr.trim().isEmpty()) {
                        logger.warning("Skipping IFS location '" + locationCode + "' due to missing 'name' or 'path' property.");
                        continue;
                    }
                    if (minFiles < 0 || maxFiles < 0 || minFiles > maxFiles) {
                         logger.warning(String.format("Skipping IFS location '%s' due to invalid min/max file configuration (min: %d, max: %d).", locationCode, minFiles, maxFiles));
                         continue;
                    }
                    if (alertTooFewSubject == null || alertTooFewSubject.trim().isEmpty() ||
                        alertTooFewBodyPrefix == null || alertTooFewBodyPrefix.trim().isEmpty() ||
                        alertTooManySubject == null || alertTooManySubject.trim().isEmpty() ||
                        alertTooManyBodyPrefix == null || alertTooManyBodyPrefix.trim().isEmpty()) {
                        logger.warning("Skipping IFS location '" + locationCode + "' due to missing alert email subject/body properties.");
                        continue;
                    }

                    // Pass the logger and fileTypesStr to the MonitoringConfig constructor
                    MonitoringConfig config = new MonitoringConfig(
                        name, pathStr, minFiles, maxFiles,
                        alertTooFewSubject, alertTooFewBodyPrefix,
                        alertTooManySubject, alertTooManyBodyPrefix,
                        emailImportance, ignoreZeroFileAlert, clientName, this.logger, fileTypesStr
                    );
                    monitorConfigs.add(config);
                    logger.info("Loaded IFS monitoring configuration for: " + config.getName() + " (Path: " + config.getPathString() + ", Min: " + config.getMinFiles() + ", Max: " + config.getMaxFiles() + ", Ignore 0-file alert: " + ignoreZeroFileAlert + ", Is SMB: " + config.isSmbPath() + ", File Types: " + config.getFileExtensions() + ")");

                } catch (NumberFormatException e) {
                    logger.warning("Invalid number format for min/max files in IFS location '" + locationCode + "': " + e.getMessage());
                } catch (Exception e) {
                    logger.warning("Error processing IFS location '" + locationCode + "': " + e.getMessage());
                }
            }

            if (monitorConfigs.isEmpty()) {
                logger.warning("No valid IFS monitoring locations found in " + monitorPropertiesFilePath + ". The monitor will not scan any folders.");
            }

            logger.info("Properties loaded successfully.");

        } catch (IOException e) {
            logger.severe(String.format("Failed to load properties due to I/O error: %s", e.getMessage()));
            throw new IllegalArgumentException("Error reading properties files: " + e.getMessage());
        }
    }

    /**
     * Represents SMB (Server Message Block) authentication configuration.
     * This class holds the username, password, and domain for SMB access.
     */
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
        private final Set<String> fileExtensions; // Using Set for efficiency
        private final int minFiles; // Minimum number of files expected
        private final int maxFiles; // Maximum number of files allowed
        private final String alertTooFewSubject;
        private final String alertTooFewBodyPrefix;
        private final String alertTooManySubject;
        private final String alertTooManyBodyPrefix;
        private final String emailImportance;
        private final boolean ignoreZeroFileAlert;
        private final String monitorServerName;
        private transient Logger locationLogger;
        private transient Set<String> processedFilePaths;
        private final String processedFilesStateFilePath;
        private final Logger parentLogger; // To log warnings from this constructor

        // Primary constructor
        public MonitoringConfig(String name, String pathStr, int minFiles, int maxFiles,
                                String alertTooFewSubject, String alertTooFewBodyPrefix,
                                String alertTooManySubject, String alertTooManyBodyPrefix,
                                String emailImportance, boolean ignoreZeroFileAlert,
                                String monitorServerName, Logger parentLogger, String fileTypesStr) {
            this.name = Objects.requireNonNull(name, "Monitoring config name cannot be null");
            this.pathStr = Objects.requireNonNull(pathStr, "Monitoring config path cannot be null");
            this.parentLogger = parentLogger;

            Matcher uncMatcher = UNC_PATH_PATTERN.matcher(pathStr);
            if (uncMatcher.matches()) {
                this.localPath = null;
                this.serverIp = uncMatcher.group(1);
                this.shareName = uncMatcher.group(2);
                String fullPathInShare = uncMatcher.group(3);
                this.sharePath = (fullPathInShare != null) ? fullPathInShare : "";
            } else {
                this.localPath = Paths.get(pathStr);
                this.serverIp = null;
                this.shareName = null;
                this.sharePath = null;
            }

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
            this.processedFilesStateFilePath = "logs/processed/processed_files_" + this.name.toLowerCase().replaceAll("[^a-z0-9]", "") + ".txt";

            this.fileExtensions = new HashSet<>(); // Initialize here
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
        public String getServerIp() { return serverIp; }
        public String getShareName() { return shareName; }
        public String getSharePath() { return sharePath; }
        public boolean isSmbPath() { return serverIp != null; }
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
                   ", minFiles=" + minFiles +
                   ", maxFiles=" + maxFiles +
                   ", emailImportance='" + emailImportance + '\'' +
                   ", ignoreZeroFileAlert=" + ignoreZeroFileAlert +
                   ", monitorServerName='" + monitorServerName + '\'' +
                   ", fileExtensions=" + fileExtensions +
                   '}';
        }
    }
}
