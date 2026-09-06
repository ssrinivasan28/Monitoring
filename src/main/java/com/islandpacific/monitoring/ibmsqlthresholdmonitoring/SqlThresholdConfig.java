package com.islandpacific.monitoring.ibmsqlthresholdmonitoring;

import com.islandpacific.monitoring.common.CredentialProtector;

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

/**
 * Configuration for IBM SQL Threshold Monitor.
 * Loads one or more named SQL row-count checks against IBM i DB2 via JDBC.
 */
public class SqlThresholdConfig {

    private final Logger logger;
    private final Properties emailProps;
    private final Properties monitorProps;
    private final List<SqlCheckConfig> sqlChecks;
    private String clientName;
    private String ibmiHost;
    private String ibmiUser;
    private String ibmiPassword;

    public SqlThresholdConfig(String emailPropertiesFilePath, String monitorPropertiesFilePath, Logger mainLogger) throws IOException, IllegalArgumentException {
        this.logger = mainLogger;
        this.emailProps = new Properties();
        this.monitorProps = new Properties();
        this.sqlChecks = new ArrayList<>();
        loadProperties(emailPropertiesFilePath, monitorPropertiesFilePath);
    }

    public Properties getEmailProps() { return emailProps; }
    public Properties getMonitorProps() { return monitorProps; }
    public List<SqlCheckConfig> getSqlChecks() { return sqlChecks; }
    public String getClientName() { return clientName; }
    public String getIbmiHost() { return ibmiHost; }
    public String getIbmiUser() { return ibmiUser; }
    public String getIbmiPassword() { return ibmiPassword; }

    private void loadProperties(String emailPropertiesFilePath, String monitorPropertiesFilePath) throws IOException, IllegalArgumentException {
        try (InputStream emailIn = new FileInputStream(emailPropertiesFilePath);
             InputStream monitorIn = new FileInputStream(monitorPropertiesFilePath)) {
            emailProps.load(emailIn);
            monitorProps.load(monitorIn);

            String clientNameFromEmail = emailProps.getProperty("mail.clientName", "");
            this.clientName = (!clientNameFromEmail.isEmpty()) ? clientNameFromEmail
                    : monitorProps.getProperty("client.name", "DefaultClient");

            this.ibmiHost = monitorProps.getProperty("ibmi.server");
            this.ibmiUser = monitorProps.getProperty("ibmi.user");
            this.ibmiPassword = CredentialProtector.resolve(monitorProps.getProperty("ibmi.password"));

            if (ibmiHost == null || ibmiHost.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing required property 'ibmi.server' in " + monitorPropertiesFilePath);
            }

            // Pattern: sql.<checkCode>.<property>
            Pattern sqlPattern = Pattern.compile("^sql\\.([^.]+)\\.(name|query|threshold|comparison|email\\.importance|alert\\.subject|alert\\.body\\.prefix)$");
            Map<String, Map<String, String>> checksConfig = new HashMap<>();

            for (String key : monitorProps.stringPropertyNames()) {
                Matcher matcher = sqlPattern.matcher(key);
                if (matcher.matches()) {
                    String checkCode = matcher.group(1);
                    String propertyType = matcher.group(2);
                    checksConfig.computeIfAbsent(checkCode, k -> new HashMap<>()).put(propertyType, monitorProps.getProperty(key));
                }
            }

            for (Map.Entry<String, Map<String, String>> entry : checksConfig.entrySet()) {
                String checkCode = entry.getKey();
                Map<String, String> props = entry.getValue();

                String name = props.get("name");
                String query = props.get("query");
                if (name == null || name.trim().isEmpty() || query == null || query.trim().isEmpty()) {
                    logger.warning("Skipping SQL check '" + checkCode + "' due to missing 'name' or 'query' property.");
                    continue;
                }

                int threshold;
                try {
                    threshold = Integer.parseInt(props.getOrDefault("threshold", "1"));
                } catch (NumberFormatException e) {
                    logger.warning("Invalid threshold for SQL check '" + checkCode + "', defaulting to 1.");
                    threshold = 1;
                }

                String comparisonStr = props.getOrDefault("comparison", "gt").trim().toLowerCase();
                boolean atLeast = comparisonStr.equals("gte") || comparisonStr.equals(">=");
                if (!atLeast && !comparisonStr.equals("gt") && !comparisonStr.equals(">")) {
                    logger.warning("Invalid comparison '" + comparisonStr + "' for SQL check '" + checkCode + "', defaulting to 'gt'.");
                }

                String emailImportance = props.getOrDefault("email.importance", "Normal");
                String alertSubject = props.getOrDefault("alert.subject", "[%s] SQL Threshold Breach - %s");
                String alertBodyPrefix = props.getOrDefault("alert.body.prefix",
                        atLeast
                            ? "Observation from server %s: SQL check '%s' returned %d row(s), meeting or exceeding threshold of %d."
                            : "Observation from server %s: SQL check '%s' returned %d row(s), exceeding threshold of %d.");

                sqlChecks.add(new SqlCheckConfig(checkCode, name, query, threshold, atLeast, emailImportance, alertSubject, alertBodyPrefix));
                logger.info("Loaded SQL threshold check: " + checkCode + " (name=" + name + ", threshold=" + threshold
                        + ", comparison=" + (atLeast ? "gte" : "gt") + ")");
            }

            if (sqlChecks.isEmpty()) {
                logger.warning("No valid SQL checks found in " + monitorPropertiesFilePath + ". The monitor will not run any checks.");
            }

        } catch (IOException e) {
            logger.severe("Failed to load properties due to I/O error: " + e.getMessage());
            throw new IllegalArgumentException("Error reading properties files: " + e.getMessage());
        }
    }

    /**
     * Represents a single configured SQL row-count threshold check.
     */
    public static class SqlCheckConfig {
        private final String code;
        private final String name;
        private final String query;
        private final int threshold;
        private final boolean atLeast;
        private final String emailImportance;
        private final String alertSubject;
        private final String alertBodyPrefix;

        public SqlCheckConfig(String code, String name, String query, int threshold, boolean atLeast,
                               String emailImportance, String alertSubject, String alertBodyPrefix) {
            this.code = code;
            this.name = name;
            this.query = query;
            this.threshold = threshold;
            this.atLeast = atLeast;
            this.emailImportance = emailImportance;
            this.alertSubject = alertSubject;
            this.alertBodyPrefix = alertBodyPrefix;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getQuery() { return query; }
        public int getThreshold() { return threshold; }
        public boolean isAtLeast() { return atLeast; }
        public String getEmailImportance() { return emailImportance; }
        public String getAlertSubject() { return alertSubject; }
        public String getAlertBodyPrefix() { return alertBodyPrefix; }
    }
}
