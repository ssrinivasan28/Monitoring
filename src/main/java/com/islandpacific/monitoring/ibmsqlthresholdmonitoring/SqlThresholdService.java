package com.islandpacific.monitoring.ibmsqlthresholdmonitoring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs configured SQL row-count checks against IBM i DB2 via JDBC and alerts on threshold breach.
 * Alerts once on breach, then suppresses until the row count drops back at or below threshold.
 */
public class SqlThresholdService {

    private final Logger logger;
    private final EmailService emailService;
    private final String connectionUrl;
    private final String user;
    private final String password;
    private final String monitorServerName;

    private final ConcurrentHashMap<String, Boolean> breached = new ConcurrentHashMap<>();

    public SqlThresholdService(Logger logger, EmailService emailService, String host, String user, String password, String monitorServerName) {
        this.logger = logger;
        this.emailService = emailService;
        this.connectionUrl = "jdbc:as400://" + host;
        this.user = user;
        this.password = password;
        this.monitorServerName = monitorServerName;

        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "IBM AS400 JDBC Driver not found. Please ensure jt400.jar is in your classpath.", e);
            throw new RuntimeException("AS400 JDBC Driver not found.", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl, user, password);
    }

    public void runCheck(SqlThresholdConfig.SqlCheckConfig check) {
        String code = check.getCode();
        int rowCount;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(check.getQuery());
             ResultSet rs = pstmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                count++;
            }
            rowCount = count;
            logger.info(String.format("SQL check '%s' (%s) returned %d row(s). Threshold: %d.",
                    code, check.getName(), rowCount, check.getThreshold()));

        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.format("Error executing SQL check '%s' (%s): %s",
                    code, check.getName(), e.getMessage()), e);
            SqlThresholdMetrics.setErrorState(code);
            return;
        }

        SqlThresholdMetrics.setRowCount(code, rowCount);

        boolean breachedNow = check.isAtLeast() ? rowCount >= check.getThreshold() : rowCount > check.getThreshold();
        if (breachedNow) {
            boolean alreadyBreached = breached.getOrDefault(code, false);
            if (!alreadyBreached) {
                String subject = String.format(check.getAlertSubject(), monitorServerName, check.getName());
                String body = String.format(check.getAlertBodyPrefix(), monitorServerName, check.getName(), rowCount, check.getThreshold());
                emailService.sendEmail(check.getName(), subject, body, check.getEmailImportance());
                SqlThresholdMetrics.incrementAlert(code);
                breached.put(code, true);
                logger.warning(String.format("Alert sent for SQL check '%s': %d rows exceeds threshold %d.",
                        code, rowCount, check.getThreshold()));
            } else {
                logger.info(String.format("SQL check '%s' still breached (%d rows). Alert already sent; suppressing.", code, rowCount));
            }
        } else {
            if (breached.getOrDefault(code, false)) {
                logger.info(String.format("SQL check '%s' resolved (%d rows, threshold %d). Re-arming alert.",
                        code, rowCount, check.getThreshold()));
            }
            breached.put(code, false);
        }
    }
}
