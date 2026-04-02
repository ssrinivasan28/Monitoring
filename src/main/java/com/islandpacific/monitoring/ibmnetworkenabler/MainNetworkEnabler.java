package com.islandpacific.monitoring.ibmnetworkenabler;

import com.ibm.as400.access.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainNetworkEnabler {

    // These will now be loaded from a properties file
    private static String SYSTEM_NAME;
    private static String USERNAME;
    private static String PASSWORD;
    private static int MONITOR_INTERVAL_MS; // Changed to milliseconds

    private static final String PROPERTIES_FILE_NAME = "networkenable.properties"; // Centralized properties file name

    public static void main(String[] args) {
        // Ensure the custom LogManager is initialized
        // The static block in LogManager will handle its setup.

        ScheduledExecutorService scheduler = null;

        try {
            // Load configuration from properties file
            loadConfiguration();

            // Initialize the scheduler
            scheduler = Executors.newSingleThreadScheduledExecutor();

            LogManager.info("IBM i NetServer User Enabler service starting. Checking every " + (MONITOR_INTERVAL_MS / 1000 / 60) + " minutes (" + MONITOR_INTERVAL_MS + " ms).");

            // Schedule the task to run periodically
            scheduler.scheduleAtFixedRate(() -> {
                AS400 system = null; // AS400 system object for the current task run
                try {
                    LogManager.info("\n--- Starting new scan for disabled NetServer users ---");

                    // 1. Establish connection to the IBM i system
                    LogManager.info("Attempting to connect to IBM i system: " + SYSTEM_NAME + "...");
                    system = new AS400(SYSTEM_NAME, USERNAME, PASSWORD);
                    system.connectService(AS400.COMMAND); // Connect to the command service
                    LogManager.info("Successfully connected to IBM i system.");

                    // 2. Get all disabled NetServer users
                    List<String> disabledUsers = getDisabledNetServerUsers(system);

                    if (disabledUsers.isEmpty()) {
                        LogManager.info("No NetServer users found with disabled access. No action needed.");
                    } else {
                        LogManager.info("\n--- Found " + disabledUsers.size() + " disabled NetServer users ---");
                        for (String userProfile : disabledUsers) {
                            LogManager.info("\nProcessing user: " + userProfile);
                            LogManager.info("NetServer access for user '" + userProfile + "' is currently DISABLED. Attempting to enable using QZLSCHSI API...");

                            // 3. Enable NetServer access using the QZLSCHSI API for the current user
                            try {
                                callQZLSCHSIAPI(system, userProfile);
                                LogManager.info("Attempted to enable NetServer access for user '" + userProfile + "' using QZLSCHSI API.");

                                // Optional: Verify the change after attempting to enable
                                LogManager.info("Verifying status after attempting to enable for " + userProfile + "...");
                                if (!isNetServerDisabled(system, userProfile)) {
                                    LogManager.info("Verification successful: NetServer access for '" + userProfile + "' is now ENABLED.");
                                } else {
                                    LogManager.warning("Verification note: QSYS2.USER_INFO still reports NetServer access for '" + userProfile + "' as DISABLED. However, QZLSCHSI API has been executed, which should grant access. Please verify connectivity manually.");
                                }
                            } catch (Exception e) {
                                LogManager.severe("Error enabling NetServer access for user '" + userProfile + "': " + e.getMessage(), e);
                            }
                        }
                        LogManager.info("\n--- Finished processing all disabled NetServer users ---");
                    }

                } catch (AS400SecurityException e) {
                    LogManager.severe("Security error connecting to IBM i: " + e.getMessage(), e);
                } catch (ErrorCompletingRequestException e) {
                    LogManager.severe("Error completing request: " + e.getMessage(), e);
                } catch (InterruptedException e) {
                    LogManager.severe("Operation interrupted: " + e.getMessage(), e);
                } catch (java.io.IOException e) {
                    LogManager.severe("I/O error during scan: " + e.getMessage(), e);
                } catch (Exception e) {
                    LogManager.severe("An unexpected error occurred during scan: " + e.getMessage(), e);
                } finally {
                    // Disconnect from the IBM i system for the current task run
                    if (system != null) {
                        try {
                            system.disconnectAllServices();
                            LogManager.info("Disconnected from IBM i system for current scan.");
                        } catch (Exception e) {
                            LogManager.severe("Error during system disconnect: " + e.getMessage(), e);
                        }
                    }
                    LogManager.info("--- Scan completed ---");
                }
            }, 0, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS); // Changed to MILLISECONDS

            // Add a shutdown hook to ensure graceful termination
            final ScheduledExecutorService finalScheduler = scheduler; // Need final reference for lambda
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LogManager.info("\nShutting down IBM i NetServer User Enabler gracefully...");
                finalScheduler.shutdown(); // Initiate shutdown
                try {
                    if (!finalScheduler.awaitTermination(30, TimeUnit.SECONDS)) { // Wait for tasks to finish
                        LogManager.warning("Scheduler did not terminate in time, forcing shutdown.");
                        finalScheduler.shutdownNow(); // Force shutdown if not terminated
                    }
                } catch (InterruptedException e) {
                    LogManager.warning("Shutdown interrupted.");
                    finalScheduler.shutdownNow();
                }
                LogManager.info("IBM i NetServer User Enabler shutdown complete.");
            }));

        } catch (IOException e) {
            LogManager.severe("Application failed to start due to configuration error: " + e.getMessage(), e);
            System.exit(1); // Exit if configuration cannot be loaded
        } catch (Exception e) {
            LogManager.severe("An unexpected error occurred during application startup: " + e.getMessage(), e);
            System.exit(1);
        }
    }


    private static void loadConfiguration() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE_NAME)) {
            properties.load(fis);
            SYSTEM_NAME = properties.getProperty("system.name");
            USERNAME = properties.getProperty("username");
            PASSWORD = properties.getProperty("password");
            MONITOR_INTERVAL_MS = Integer.parseInt(properties.getProperty("monitor.interval.ms", "300000")); // Default to 300000 ms (5 minutes)

            if (SYSTEM_NAME == null || USERNAME == null || PASSWORD == null) {
                throw new IOException("Missing one or more required properties (system.name, username, password) in " + PROPERTIES_FILE_NAME);
            }
            LogManager.info("Configuration loaded from " + PROPERTIES_FILE_NAME + ".");
        }
    }


    private static boolean isNetServerDisabled(AS400 system, String userProfile) throws Exception {
        Connection jdbcConnection = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean isDisabled = false;

        try {
            // Register the JTOpen JDBC driver
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
            // Use static SYSTEM_NAME, USERNAME, and PASSWORD for JDBC connection URL
            String url = "jdbc:as400://" + SYSTEM_NAME + ";user=" + USERNAME + ";password=" + PASSWORD;
            jdbcConnection = DriverManager.getConnection(url);

            stmt = jdbcConnection.createStatement();

            // SQL query to get the NETSERVER_DISABLED status
            String sql = "SELECT NETSERVER_DISABLED FROM QSYS2.USER_INFO WHERE AUTHORIZATION_NAME = '" + userProfile.toUpperCase() + "'";
            LogManager.fine("Executing SQL: " + sql);
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                String netServerStatus = rs.getString("NETSERVER_DISABLED");
                isDisabled = "YES".equalsIgnoreCase(netServerStatus);
            } else {
                LogManager.info("User profile '" + userProfile + "' not found on the IBM i system during verification.");
            }
        } finally {
            // Close JDBC resources
            if (rs != null) try { rs.close(); } catch (SQLException e) { LogManager.warning("Error closing ResultSet: " + e.getMessage()); }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { LogManager.warning("Error closing Statement: " + e.getMessage()); }
            if (jdbcConnection != null) try { jdbcConnection.close(); } catch (SQLException e) { LogManager.warning("Error closing JDBC Connection: " + e.getMessage()); }
        }
        return isDisabled;
    }


    private static List<String> getDisabledNetServerUsers(AS400 system) throws Exception {
        List<String> disabledUsers = new ArrayList<>();
        Connection jdbcConnection = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
            String url = "jdbc:as400://" + SYSTEM_NAME + ";user=" + USERNAME + ";password=" + PASSWORD;
            jdbcConnection = DriverManager.getConnection(url);
            stmt = jdbcConnection.createStatement();

            // SQL query to get all authorization names where NETSERVER_DISABLED is 'YES'
            String sql = "SELECT AUTHORIZATION_NAME FROM QSYS2.USER_INFO WHERE NETSERVER_DISABLED = 'YES'";
            LogManager.info("Querying for disabled NetServer users: " + sql);
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                disabledUsers.add(rs.getString("AUTHORIZATION_NAME"));
            }
            LogManager.info("Found " + disabledUsers.size() + " users with disabled NetServer access.");

        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { LogManager.warning("Error closing ResultSet: " + e.getMessage()); }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { LogManager.warning("Error closing Statement: " + e.getMessage()); }
            if (jdbcConnection != null) try { jdbcConnection.close(); } catch (SQLException e) { LogManager.warning("Error closing JDBC Connection: " + e.getMessage()); }
        }
        return disabledUsers;
    }


    private static void callQZLSCHSIAPI(AS400 system, String userProfile) throws Exception {
        ProgramCall programCall = new ProgramCall(system);
        ProgramParameter[] parmList = new ProgramParameter[4];

        // Total length = 4 (for binary length) + 10 (for user profile) = 14 bytes
        int requestVarLength = 14;
        ByteBuffer requestVarBuffer = ByteBuffer.allocate(requestVarLength);
        requestVarBuffer.order(ByteOrder.BIG_ENDIAN); // Changed to BIG_ENDIAN for typical IBM i binary integers
        requestVarBuffer.putInt(10); // Length of user profile name
        String paddedUserProfile = String.format("%-10s", userProfile.toUpperCase());
        requestVarBuffer.put(new AS400Text(paddedUserProfile.length(), system.getCcsid(), system).toBytes(paddedUserProfile));

        parmList[0] = new ProgramParameter(requestVarBuffer.array());
        parmList[1] = new ProgramParameter(new AS400Bin4().toBytes(requestVarLength));
        parmList[2] = new ProgramParameter(new AS400Text(8, system.getCcsid(), system).toBytes("ZLSS0200"));

        byte[] errorCode = new byte[8]; // Minimal error code structure
        parmList[3] = new ProgramParameter(errorCode); // Output parameter for error information

        programCall.setProgram("/QSYS.LIB/QZLSCHSI.PGM", parmList);

        LogManager.info("Calling QSYS/QZLSCHSI API for user: " + userProfile);

        if (programCall.run()) {
            LogManager.info("QZLSCHSI API call successful.");
            AS400Message[] messageList = programCall.getMessageList();
            for (int i = 0; i < messageList.length; ++i) {
                LogManager.info("API message: " + messageList[i].getText());
            }
        } else {
            AS400Message[] messageList = programCall.getMessageList();
            for (int i = 0; i < messageList.length; ++i) {
                LogManager.severe("API call failed. Message: " + messageList[i].getText());
            }
            throw new Exception("Failed to call QSYS/QZLSCHSI API for user: " + userProfile);
        }
    }
}
