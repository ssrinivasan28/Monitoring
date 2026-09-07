package com.islandpacific.monitoring.ibmnetworkenabler;

import com.ibm.as400.access.*;
import com.islandpacific.monitoring.common.AppLogger;
import com.islandpacific.monitoring.common.CredentialProtector;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainNetworkEnabler {

    private static final Logger logger = Logger.getLogger(MainNetworkEnabler.class.getName());

    private static String SYSTEM_NAME;
    private static String USERNAME;
    private static String PASSWORD;
    private static int MONITOR_INTERVAL_MS;

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        String emailPropsFile = args.length >= 1 ? args[0] : "email.properties";
        String monitorPropsFile = args.length >= 2 ? args[1] : "ibmnetworkenabler.properties";

        ScheduledExecutorService scheduler = null;

        try {
            loadConfiguration(emailPropsFile, monitorPropsFile);

            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "network-enabler");
                t.setDaemon(true);
                return t;
            });

            logger.info("IBM i NetServer User Enabler service starting. Checking every "
                    + (MONITOR_INTERVAL_MS / 1000 / 60) + " minutes.");

            scheduler.scheduleAtFixedRate(() -> {
                AS400 system = null;
                try {
                    logger.info("--- Starting new scan for disabled NetServer users ---");
                    system = new AS400(SYSTEM_NAME, USERNAME, PASSWORD);
                    system.connectService(AS400.COMMAND);
                    logger.info("Connected to IBM i system: " + SYSTEM_NAME);

                    List<String> disabledUsers = getDisabledNetServerUsers();

                    if (disabledUsers.isEmpty()) {
                        logger.info("No NetServer users found with disabled access.");
                    } else {
                        logger.info("Found " + disabledUsers.size() + " disabled NetServer user(s).");
                        for (String userProfile : disabledUsers) {
                            logger.info("Processing user: " + userProfile);
                            try {
                                callQZLSCHSIAPI(system, userProfile);
                                logger.info("QZLSCHSI called for user '" + userProfile + "'.");
                                if (!isNetServerDisabled(userProfile)) {
                                    logger.info("Verified: NetServer access for '" + userProfile + "' is now ENABLED.");
                                } else {
                                    logger.warning("Verification: QSYS2.USER_INFO still reports '" + userProfile + "' as DISABLED after API call.");
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error enabling NetServer for '" + userProfile + "': " + e.getMessage(), e);
                            }
                        }
                        logger.info("--- Finished processing disabled NetServer users ---");
                    }

                } catch (AS400SecurityException e) {
                    logger.log(Level.SEVERE, "Security error connecting to IBM i: " + e.getMessage(), e);
                } catch (ErrorCompletingRequestException e) {
                    logger.log(Level.SEVERE, "Error completing request: " + e.getMessage(), e);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Operation interrupted: " + e.getMessage(), e);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "I/O error during scan: " + e.getMessage(), e);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected error during scan: " + e.getMessage(), e);
                } finally {
                    if (system != null) {
                        try {
                            system.disconnectAllServices();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error during system disconnect: " + e.getMessage(), e);
                        }
                    }
                    logger.info("--- Scan completed ---");
                }
            }, 0, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);

            final ScheduledExecutorService finalScheduler = scheduler;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down IBM i NetServer User Enabler...");
                finalScheduler.shutdown();
                try {
                    if (!finalScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                        finalScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    finalScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                logger.info("Shutdown complete.");
            }));

            Thread.currentThread().join();

        } catch (IOException e) {
            logger.severe("Failed to start due to configuration error: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during startup: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void loadConfiguration(String emailPropsFile, String monitorPropsFile) throws IOException {
        Properties emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream(emailPropsFile)) {
            emailProps.load(fis);
        }

        Properties monitorProps = new Properties();
        try (FileInputStream fis = new FileInputStream(monitorPropsFile)) {
            monitorProps.load(fis);
        }

        String logLevel = emailProps.getProperty("log.level", "INFO");
        String logFolder = emailProps.getProperty("log.folder", "logs");
        int retentionDays = Integer.parseInt(emailProps.getProperty("log.retention.days", "30"));
        int purgeIntervalHours = Integer.parseInt(emailProps.getProperty("log.purge.interval.hours", "24"));
        AppLogger.setupLogger("ibmnetworkenabler", logLevel, logFolder);
        AppLogger.startScheduledLogPurge(retentionDays, purgeIntervalHours);

        SYSTEM_NAME = monitorProps.getProperty("ibmi.host");
        USERNAME = monitorProps.getProperty("ibmi.user");
        String passwordRaw = monitorProps.getProperty("ibmi.password");

        if (SYSTEM_NAME == null || SYSTEM_NAME.trim().isEmpty()
                || USERNAME == null || USERNAME.trim().isEmpty()
                || passwordRaw == null || passwordRaw.trim().isEmpty()) {
            throw new IOException("Missing required property: ibmi.host, ibmi.user, or ibmi.password in " + monitorPropsFile);
        }
        PASSWORD = CredentialProtector.resolve(passwordRaw);
        MONITOR_INTERVAL_MS = Integer.parseInt(monitorProps.getProperty("monitor.interval.ms", "300000"));

        logger.info("Configuration loaded. System: " + SYSTEM_NAME + ", interval: " + MONITOR_INTERVAL_MS + "ms");
    }

    private static List<String> getDisabledNetServerUsers() throws Exception {
        List<String> disabledUsers = new ArrayList<>();
        Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        String url = "jdbc:as400://" + SYSTEM_NAME;
        try (Connection conn = DriverManager.getConnection(url, USERNAME, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT AUTHORIZATION_NAME FROM QSYS2.USER_INFO WHERE NETSERVER_DISABLED = 'YES'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                disabledUsers.add(rs.getString("AUTHORIZATION_NAME"));
            }
        }
        logger.info("Found " + disabledUsers.size() + " user(s) with disabled NetServer access.");
        return disabledUsers;
    }

    private static boolean isNetServerDisabled(String userProfile) throws Exception {
        Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        String url = "jdbc:as400://" + SYSTEM_NAME;
        try (Connection conn = DriverManager.getConnection(url, USERNAME, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT NETSERVER_DISABLED FROM QSYS2.USER_INFO WHERE AUTHORIZATION_NAME = ?")) {
            ps.setString(1, userProfile.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "YES".equalsIgnoreCase(rs.getString("NETSERVER_DISABLED"));
                }
            }
        }
        return false;
    }

    private static void callQZLSCHSIAPI(AS400 system, String userProfile) throws Exception {
        ProgramCall programCall = new ProgramCall(system);
        ProgramParameter[] parmList = new ProgramParameter[4];

        int requestVarLength = 14;
        ByteBuffer requestVarBuffer = ByteBuffer.allocate(requestVarLength);
        requestVarBuffer.order(ByteOrder.BIG_ENDIAN);
        requestVarBuffer.putInt(10);
        String paddedUserProfile = String.format("%-10s", userProfile.toUpperCase());
        requestVarBuffer.put(new AS400Text(paddedUserProfile.length(), system.getCcsid(), system).toBytes(paddedUserProfile));

        parmList[0] = new ProgramParameter(requestVarBuffer.array());
        parmList[1] = new ProgramParameter(new AS400Bin4().toBytes(requestVarLength));
        parmList[2] = new ProgramParameter(new AS400Text(8, system.getCcsid(), system).toBytes("ZLSS0200"));
        parmList[3] = new ProgramParameter(new byte[8]);

        programCall.setProgram("/QSYS.LIB/QZLSCHSI.PGM", parmList);
        logger.info("Calling QZLSCHSI for user: " + userProfile);

        if (programCall.run()) {
            for (AS400Message msg : programCall.getMessageList()) {
                logger.info("API message: " + msg.getText());
            }
        } else {
            StringBuilder sb = new StringBuilder("QZLSCHSI failed for '" + userProfile + "':");
            for (AS400Message msg : programCall.getMessageList()) {
                logger.severe("API error: " + msg.getText());
                sb.append(" ").append(msg.getText());
            }
            throw new Exception(sb.toString());
        }
    }
}
