package com.islandpacific.monitoring.apiurlmonitoring;

import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class ApiUrlMonitorConfig {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<UrlConfig> urlConfigs;
    private final String clientName;

    public ApiUrlMonitorConfig(String emailPropertiesPath, String monitorPropertiesPath) throws IOException {
        this.emailProps = loadProperties(emailPropertiesPath);
        this.monitorProps = loadProperties(monitorPropertiesPath);
        this.clientName = monitorProps.getProperty("client.name",
                emailProps.getProperty("mail.clientName", "API URL Monitor"));
        this.urlConfigs = parseUrlConfigs();
    }

    private Properties loadProperties(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }
        return props;
    }

    private List<UrlConfig> parseUrlConfigs() {
        List<UrlConfig> configs = new ArrayList<>();
        // Find all api.url.<CODE>.url keys
        for (String key : monitorProps.stringPropertyNames()) {
            if (key.startsWith("api.url.") && key.endsWith(".url")) {
                String code = key.substring("api.url.".length(), key.length() - ".url".length());
                String url = monitorProps.getProperty(key, "").trim();
                if (url.isEmpty()) continue;

                String name = monitorProps.getProperty("api.url." + code + ".name", code);
                int timeoutMs = Integer.parseInt(monitorProps.getProperty("api.url." + code + ".timeout.ms",
                        monitorProps.getProperty("api.default.timeout.ms", "5000")));
                int breachCount = Integer.parseInt(monitorProps.getProperty("api.url." + code + ".breach.count",
                        monitorProps.getProperty("api.default.breach.count", "2")));
                String bearerToken = CredentialProtector.resolve(
                        monitorProps.getProperty("api.url." + code + ".bearer.token", ""));

                configs.add(new UrlConfig(code, name, url, timeoutMs, breachCount, bearerToken));
                logger.info("Loaded URL config: [" + code + "] " + name + " -> " + url);
            }
        }
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("No URL configs found. Add api.url.<CODE>.url entries to properties.");
        }
        return configs;
    }

    public Properties getEmailProps() { return emailProps; }
    public Properties getMonitorProps() { return monitorProps; }
    public List<UrlConfig> getUrlConfigs() { return urlConfigs; }
    public String getClientName() { return clientName; }

    public static class UrlConfig {
        public final String code;
        public final String name;
        public final String url;
        public final int timeoutMs;
        public final int breachCount;
        public final String bearerToken;

        public UrlConfig(String code, String name, String url, int timeoutMs, int breachCount, String bearerToken) {
            this.code = code;
            this.name = name;
            this.url = url;
            this.timeoutMs = timeoutMs;
            this.breachCount = breachCount;
            this.bearerToken = bearerToken;
        }
    }
}
