package com.islandpacific.monitoring.userprofilechecker;

import com.islandpacific.monitoring.common.AppLogger;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainUserProfileChecker {

    private static final String USER_PROPS = "userprofilecheck.properties";
    private static final String EMAIL_PROPS = "email.properties";
    private static final String SNAPSHOT_FILE = "disabled_snapshot.txt";
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Properties userCfg;
    private static Properties emailCfg;

    private static final Logger logger = AppLogger.getLogger();

    public static void main(String[] args) throws Exception {
        // Initialize logger with module name and configurable log level
        String logLevel = System.getProperty("log.level", "INFO");
        String logFolder = System.getProperty("log.folder", "logs");
        AppLogger.setupLogger("userprofilechecker", logLevel, logFolder);
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down User Profile Checker...");
            AppLogger.closeLogger();
        }));
        
        loadProperties();
        
        // Start automatic log purge
        int retentionDays = Integer.parseInt(emailCfg.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailCfg.getProperty("log.purge.interval.hours", "24"));
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        int intervalSec = Integer.parseInt(userCfg.getProperty("monitor.interval.seconds", "300"));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkProfiles();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during scheduled profile check", e);
            }
        }, 0, intervalSec, TimeUnit.SECONDS);
    }

    private static void loadProperties() throws IOException {
        userCfg = new Properties();
        try (FileInputStream fis = new FileInputStream(USER_PROPS)) {
            userCfg.load(fis);
        }

        emailCfg = new Properties();
        try (FileInputStream fis = new FileInputStream(EMAIL_PROPS)) {
            emailCfg.load(fis);
        }

        logger.info("Loaded configuration from properties files.");
    }

    private static void checkProfiles() throws Exception {
        // Load previous snapshot
        Map<String, String> prevDisabled = loadSnapshot();

        // Get current disabled profiles
        Map<String, String> currDisabled = new HashMap<>();

        String system = userCfg.getProperty("system.name");
        String user = userCfg.getProperty("username");
        String pass = userCfg.getProperty("password");
        String url = "jdbc:as400://" + system;

        Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT AUTHORIZATION_NAME, TEXT_DESCRIPTION " +
                         "FROM QSYS2.USER_INFO WHERE STATUS = '*DISABLED'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    currDisabled.put(rs.getString("AUTHORIZATION_NAME"), rs.getString("TEXT_DESCRIPTION"));
                }
            }
        }

        // Compare snapshots
        Map<String, String> newlyDisabled = new HashMap<>();
        for (Map.Entry<String, String> entry : currDisabled.entrySet()) {
            if (!prevDisabled.containsKey(entry.getKey())) {
                newlyDisabled.put(entry.getKey(), entry.getValue());
            }
        }

        if (!newlyDisabled.isEmpty()) {
            logger.info("Newly disabled profiles: " + newlyDisabled);

            // Initialize OAuth2 if needed
            String authMethod = emailCfg.getProperty("mail.auth.method", "SMTP").toUpperCase();
            OAuth2TokenProvider oauth2Provider = null;
            String graphMailUrl = null;
            String fromUser = null;
            
            if ("OAUTH2".equals(authMethod)) {
                String tenantId = emailCfg.getProperty("mail.oauth2.tenant.id");
                String clientId = emailCfg.getProperty("mail.oauth2.client.id");
                String clientSecret = emailCfg.getProperty("mail.oauth2.client.secret");
                String scope = emailCfg.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
                String tokenUrl = emailCfg.getProperty("mail.oauth2.token.url", "");
                
                if (tenantId != null && clientId != null && clientSecret != null) {
                    oauth2Provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                    fromUser = emailCfg.getProperty("mail.oauth2.from.user", emailCfg.getProperty("mail.from").replaceAll(".*<([^>]+)>.*", "$1").trim());
                    String providedGraphUrl = emailCfg.getProperty("mail.oauth2.graph.mail.url", "");
                    if (providedGraphUrl != null && !providedGraphUrl.trim().isEmpty()) {
                        graphMailUrl = providedGraphUrl.trim();
                    } else {
                        graphMailUrl = "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail";
                    }
                    logger.info("OAuth2 authentication configured for email service.");
                }
            }

            EmailService email = new EmailService(emailCfg, authMethod, oauth2Provider, graphMailUrl, fromUser);
            email.sendUserDisabledAlert(newlyDisabled,
                    TS_FORMAT.format(new Date()), system);
        } else {
            logger.info("[" + TS_FORMAT.format(new Date()) + "] No new disabled profiles found.");
        }

        // Save current snapshot for next run
        saveSnapshot(currDisabled);
    }

    private static Map<String, String> loadSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        File f = new File(SNAPSHOT_FILE);
        if (!f.exists()) return snapshot;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    snapshot.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error loading snapshot file", e);
        }
        return snapshot;
    }

    private static void saveSnapshot(Map<String, String> snapshot) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SNAPSHOT_FILE))) {
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                bw.write(entry.getKey() + "|" + entry.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error saving snapshot file", e);
        }
    }
}
