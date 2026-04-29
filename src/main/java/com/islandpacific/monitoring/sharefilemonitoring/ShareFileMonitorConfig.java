package com.islandpacific.monitoring.sharefilemonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShareFileMonitorConfig {

    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<FolderConfig> folderConfigs;
    private final String clientName;

    public ShareFileMonitorConfig(String emailPropsPath, String monitorPropsPath, Logger logger) throws IOException {
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.folderConfigs = new ArrayList<>();

        try (InputStream e = new FileInputStream(emailPropsPath);
             InputStream m = new FileInputStream(monitorPropsPath)) {
            emailProps.load(e);
            monitorProps.load(m);
        }

        String cnMonitor = monitorProps.getProperty("client.name", "").trim();
        String cnEmail = emailProps.getProperty("mail.clientName", "").trim();
        this.clientName = !cnMonitor.isEmpty() ? cnMonitor : (!cnEmail.isEmpty() ? cnEmail : "ShareFile Monitor");

        // Pattern: sf.folder.<code>.<property>
        Pattern p = Pattern.compile("^sf\\.folder\\.([^.]+)\\.(name|path|min\\.files|max\\.files|ignore\\.zero\\.file\\.alert)$");
        Map<String, Map<String, String>> raw = new HashMap<>();
        for (String key : monitorProps.stringPropertyNames()) {
            Matcher m2 = p.matcher(key);
            if (m2.matches()) {
                raw.computeIfAbsent(m2.group(1), k -> new HashMap<>()).put(m2.group(2), monitorProps.getProperty(key));
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : raw.entrySet()) {
            Map<String, String> props = entry.getValue();
            String name = props.get("name");
            String path = props.get("path");
            if (name == null || name.isBlank() || path == null || path.isBlank()) {
                logger.warning("Skipping folder '" + entry.getKey() + "': missing name or path");
                continue;
            }
            int min = Integer.parseInt(props.getOrDefault("min.files", "0"));
            int max = Integer.parseInt(props.getOrDefault("max.files", String.valueOf(Integer.MAX_VALUE)));
            boolean ignoreZero = Boolean.parseBoolean(props.getOrDefault("ignore.zero.file.alert", "false"));
            folderConfigs.add(new FolderConfig(name, path, min, max, ignoreZero, clientName));
        }

        if (folderConfigs.isEmpty()) {
            logger.warning("No valid ShareFile folder configs found in " + monitorPropsPath);
        }
    }

    public Properties getEmailProps() { return emailProps; }
    public Properties getMonitorProps() { return monitorProps; }
    public List<FolderConfig> getFolderConfigs() { return folderConfigs; }
    public String getClientName() { return clientName; }

    public static class FolderConfig {
        private final String name;
        private final String remotePath;
        private final int minFiles;
        private final int maxFiles;
        private final boolean ignoreZeroFileAlert;
        private final String clientName;

        public FolderConfig(String name, String remotePath, int minFiles, int maxFiles,
                            boolean ignoreZeroFileAlert, String clientName) {
            this.name = name;
            this.remotePath = remotePath;
            this.minFiles = minFiles;
            this.maxFiles = maxFiles;
            this.ignoreZeroFileAlert = ignoreZeroFileAlert;
            this.clientName = clientName;
        }

        public String getName() { return name; }
        public String getRemotePath() { return remotePath; }
        public int getMinFiles() { return minFiles; }
        public int getMaxFiles() { return maxFiles; }
        public boolean isIgnoreZeroFileAlert() { return ignoreZeroFileAlert; }
        public String getClientName() { return clientName; }
    }
}
