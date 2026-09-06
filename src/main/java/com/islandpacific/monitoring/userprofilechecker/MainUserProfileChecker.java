package com.islandpacific.monitoring.userprofilechecker;

import com.islandpacific.monitoring.common.AppLogger;
import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainUserProfileChecker {

    private static final String SNAPSHOT_FILE = "disabled_snapshot.txt";
    private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Properties userCfg;
    private static Properties emailCfg;

    private static final Logger logger = Logger.getLogger(MainUserProfileChecker.class.getName());

    public static void main(String[] args) throws Exception {
        String emailPropsFile = args.length >= 1 ? args[0] : "email.properties";
        String userPropsFile = args.length >= 2 ? args[1] : "ibmuserprofilechecker.properties";

        loadProperties(emailPropsFile, userPropsFile);

        String logLevel = emailCfg.getProperty("log.level", "INFO");
        String logFolder = emailCfg.getProperty("log.folder", "logs");
        int retentionDays = Integer.parseInt(emailCfg.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailCfg.getProperty("log.purge.interval.hours", "24"));
        AppLogger.setupLogger("userprofilechecker", logLevel, logFolder);
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        String clientName = userCfg.getProperty("client.name", emailCfg.getProperty("mail.clientName", ""));
        String logoPath = userCfg.getProperty("logo.path", "");
        EmailService emailService = new EmailService(emailCfg, clientName, logoPath);

        int intervalMs = Integer.parseInt(userCfg.getProperty("monitor.interval.ms", "300000"));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "userprofile-checker");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkProfiles(emailService);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during scheduled profile check", e);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down User Profile Checker...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        logger.info("User Profile Checker started. Interval: " + intervalMs + " ms.");
        Thread.currentThread().join();
    }

    private static void loadProperties(String emailPropsFile, String userPropsFile) throws IOException {
        emailCfg = new Properties();
        try (FileInputStream fis = new FileInputStream(emailPropsFile)) {
            emailCfg.load(fis);
        }
        userCfg = new Properties();
        try (FileInputStream fis = new FileInputStream(userPropsFile)) {
            userCfg.load(fis);
        }
        logger.info("Configuration loaded.");
    }

    private static void checkProfiles(EmailService emailService) throws Exception {
        Map<String, String> prevDisabled = loadSnapshot();

        String system = userCfg.getProperty("ibmi.host");
        String user = userCfg.getProperty("ibmi.user");
        String pass = CredentialProtector.resolve(userCfg.getProperty("ibmi.password"));

        if (system == null || system.trim().isEmpty()
                || user == null || user.trim().isEmpty()
                || pass == null || pass.trim().isEmpty()) {
            throw new IllegalStateException("Required property ibmi.host, ibmi.user, or ibmi.password missing.");
        }

        Map<String, String> currDisabled = new HashMap<>();
        Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        String url = "jdbc:as400://" + system;
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT AUTHORIZATION_NAME, TEXT_DESCRIPTION FROM QSYS2.USER_INFO WHERE STATUS = '*DISABLED'")) {
            while (rs.next()) {
                currDisabled.put(rs.getString("AUTHORIZATION_NAME"), rs.getString("TEXT_DESCRIPTION"));
            }
        }

        Map<String, String> newlyDisabled = new HashMap<>();
        for (Map.Entry<String, String> entry : currDisabled.entrySet()) {
            if (!prevDisabled.containsKey(entry.getKey())) {
                newlyDisabled.put(entry.getKey(), entry.getValue());
            }
        }

        if (!newlyDisabled.isEmpty()) {
            logger.info("Newly disabled profiles: " + newlyDisabled.keySet());
            emailService.sendUserDisabledAlert(newlyDisabled, TS_FORMAT.format(new Date()), system);
        } else {
            logger.info("[" + TS_FORMAT.format(new Date()) + "] No new disabled profiles found.");
        }

        saveSnapshot(currDisabled);
    }

    private static Map<String, String> loadSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        File f = getSnapshotFile();
        if (!f.exists()) return snapshot;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("b64|")) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) {
                        try {
                            snapshot.put(decodeSnapshotPart(parts[1]), decodeSnapshotPart(parts[2]));
                        } catch (java.io.UncheckedIOException e) {
                            logger.warning("Skipping corrupted snapshot line: " + e.getMessage());
                            return new HashMap<>();
                        }
                    }
                } else {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        snapshot.put(parts[0], parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error loading snapshot file", e);
        }
        return snapshot;
    }

    private static void saveSnapshot(Map<String, String> snapshot) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(getSnapshotFile()))) {
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                bw.write("b64|" + encodeSnapshotPart(entry.getKey()) + "|" + encodeSnapshotPart(entry.getValue()));
                bw.newLine();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error saving snapshot file", e);
        }
    }

    private static String encodeSnapshotPart(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safeValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String decodeSnapshotPart(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new java.io.UncheckedIOException(new java.io.IOException("Corrupted snapshot entry: " + value, e));
        }
    }

    private static File getSnapshotFile() {
        return new File(System.getProperty("userprofilechecker.snapshot.file", SNAPSHOT_FILE));
    }
}
