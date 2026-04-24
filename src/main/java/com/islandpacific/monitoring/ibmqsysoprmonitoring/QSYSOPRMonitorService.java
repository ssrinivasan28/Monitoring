package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class QSYSOPRMonitorService {

    private static final Logger LOGGER = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private final QSYSOPRMonitorConfig config;
    private final EmailService emailService;


    public QSYSOPRMonitorService(QSYSOPRMonitorConfig config, EmailService emailService) {
        this.config = config;
        this.emailService = emailService;
    }


    private Connection getDbConnection() {
        try {
            return DriverManager.getConnection(config.getDbUrl(), config.getDbUsername(), config.getDbPassword());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error: " + e.getMessage(), e);
            QSYSOPRMonitorMetrics.getDbConnectionErrors().inc();
            return null;
        }
    }


    private List<MessageInfo> getQsysoprMessages(Connection conn, String lastCheckedTimestamp) throws SQLException {
        List<MessageInfo> messages = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT MESSAGE_ID, MESSAGE_TEXT, MESSAGE_TIMESTAMP, FROM_JOB, FROM_USER ");
        query.append("FROM QSYS2.MESSAGE_QUEUE_INFO ");
        query.append("WHERE MESSAGE_QUEUE_NAME = 'QSYSOPR'");

        if (lastCheckedTimestamp != null && !lastCheckedTimestamp.isEmpty()) {
            query.append(" AND MESSAGE_TIMESTAMP > ?");
        }
        query.append(" ORDER BY MESSAGE_TIMESTAMP ASC");

        try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            if (lastCheckedTimestamp != null && !lastCheckedTimestamp.isEmpty()) {
                pstmt.setTimestamp(1, Timestamp.valueOf(lastCheckedTimestamp));
            }

            LOGGER.log(Level.FINE, "Executing SQL query: " + query.toString() + (lastCheckedTimestamp != null ? " with timestamp " + lastCheckedTimestamp : ""));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp msgTimestamp = rs.getTimestamp("MESSAGE_TIMESTAMP");
                    messages.add(new MessageInfo(
                        rs.getString("MESSAGE_ID"),
                        rs.getString("MESSAGE_TEXT"),
                        msgTimestamp,
                        rs.getString("FROM_JOB"),
                        rs.getString("FROM_USER")
                    ));
                    QSYSOPRMonitorMetrics.getMessagesProcessedTotal().inc();
                    // Add debug logging for the timestamp retrieved
                    LOGGER.log(Level.FINE, "Retrieved message: ID=" + rs.getString("MESSAGE_ID") + 
                                            ", Timestamp=" + msgTimestamp + 
                                            ", EpochSeconds=" + (msgTimestamp != null ? msgTimestamp.getTime() / 1000 : "null"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL query error: " + e.getMessage(), e);
            QSYSOPRMonitorMetrics.getSqlQueryErrors().inc();
            throw e;
        }
        return messages;
    }


    private boolean isJobFailureMessage(MessageInfo message) {
        String msgId = message.getMessageId() != null ? message.getMessageId().trim() : "";
        String msgText = message.getMessageText() != null ? message.getMessageText().trim().toUpperCase() : "";

        // Check by MESSAGE_ID
        if (config.getJobFailureMessageIds().contains(msgId)) {
            LOGGER.log(Level.FINE, "  [DEBUG] Matched by ID: " + msgId);
            return true;
        }

        // Check by MESSAGE_TEXT keywords
        for (String keyword : config.getJobFailureKeywords()) {
            if (msgText.contains(keyword.toUpperCase().trim())) {
                LOGGER.log(Level.FINE, "  [DEBUG] Matched by Keyword: '" + keyword + "' in text: '" + msgText + "'");
                return true;
            }
        }
        return false;
    }


    public String loadLastCheckedTimestamp() {
        try (BufferedReader reader = new BufferedReader(new FileReader(config.getStateFileName()))) {
            String timestamp = reader.readLine();
            if (timestamp != null && !timestamp.trim().isEmpty()) {
                return timestamp.trim();
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not load last checked timestamp file (might not exist yet): " + e.getMessage());
        }
        return null;
    }

 
    public void saveLastCheckedTimestamp(String timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(config.getStateFileName()))) {
            if (timestamp != null) {
                writer.write(timestamp);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving last checked timestamp file: " + e.getMessage(), e);
        }
    }

 
    public String scanAndAlert(String lastCheckedTimestamp) throws SQLException {
        Connection dbConnection = null;
        String currentScanLatestTimestamp = lastCheckedTimestamp;
       
        long latestMessageTimestampSecondsForGauge = System.currentTimeMillis() / 1000;

        try {
            dbConnection = getDbConnection();
            if (dbConnection == null) {
                LOGGER.warning("Database connection not available. Skipping scan.");
                // Set the gauge to current time as fallback if DB connection fails
                QSYSOPRMonitorMetrics.getLastProcessedMessageTimestamp().set(latestMessageTimestampSecondsForGauge);
                return lastCheckedTimestamp;
            }

            List<MessageInfo> newMessages = getQsysoprMessages(dbConnection, lastCheckedTimestamp);
            
            if (!newMessages.isEmpty()) {
                LOGGER.info("Found " + newMessages.size() + " new messages since last check.");

                for (MessageInfo message : newMessages) {
                    LOGGER.log(Level.FINE, "  [DEBUG] Checking message: ID=" + message.getMessageId() + ", Text='" + message.getMessageText() + "'");

                    // Always update the latest timestamp for the Prometheus gauge from messages found in this scan
                    Timestamp msgTimestamp = message.getMessageTimestamp();
                    if (msgTimestamp != null) { // Ensure timestamp is not null
                        long messageTimestampSec = msgTimestamp.getTime() / 1000;
                        if (messageTimestampSec > latestMessageTimestampSecondsForGauge) {
                            latestMessageTimestampSecondsForGauge = messageTimestampSec;
                        }
                    }

                    if (isJobFailureMessage(message)) {
                        // Construct the subject with client.name
                        String subject = String.format("%s IBM i QSYSOPR Alert: Job Failure - %s",
                                config.getClientName(), message.getMessageId());
                        LOGGER.info("New job failure detected: " + message.getMessageId() + " - " + message.getMessageText());
                        emailService.sendEmailAlert(message, subject);
                        QSYSOPRMonitorMetrics.getJobFailureAlertsSent().inc();
                    } else {
                        LOGGER.log(Level.FINE, "  [DEBUG] Message is not a job failure: " + message.getMessageId());
                    }

                    // Update currentScanLatestTimestamp for the state file (which determines the next query start)
                    String currentMsgTimestampStr = message.getMessageTimestamp() != null ? message.getMessageTimestamp().toString() : ""; // Handle null timestamp
                    if (currentMsgTimestampStr != null && !currentMsgTimestampStr.isEmpty() && // Ensure it's not null/empty
                        (currentScanLatestTimestamp == null || currentMsgTimestampStr.compareTo(currentScanLatestTimestamp) > 0)) {
                        currentScanLatestTimestamp = currentMsgTimestampStr;
                    }
                }
            } else {
                LOGGER.info("No new messages found or no job failures detected in this scan.");
               
            }
            
            // Always update the Prometheus gauge with the latest determined timestamp for this scan
            QSYSOPRMonitorMetrics.getLastProcessedMessageTimestamp().set(latestMessageTimestampSecondsForGauge);
            
            return currentScanLatestTimestamp;

        } finally {
            if (dbConnection != null) {
                try {
                    dbConnection.close();
                    LOGGER.info("Database connection closed after scan.");
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing database connection after scan: " + e.getMessage(), e);
                }
            }
        }
    }
}