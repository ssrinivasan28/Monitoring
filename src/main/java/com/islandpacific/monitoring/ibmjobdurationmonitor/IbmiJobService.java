package com.islandpacific.monitoring.ibmjobdurationmonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IbmiJobService {

    private static final Logger logger = Logger.getLogger(IbmiJobService.class.getName());

    private final String connectionUrl;
    private final String user;
    private final String password;

    // Returns elapsed CPU seconds per active job matching name/user.
    // JOB_USER = "*" matches any user.
    public static class ActiveJobInfo {
        public final String jobName;
        public final String jobUser;
        public final String jobNumber;
        public final String jobStatus;
        public final long elapsedSeconds; // JOB_ACTIVE_TIME in seconds since job became active

        public ActiveJobInfo(String jobName, String jobUser, String jobNumber, String jobStatus, long elapsedSeconds) {
            this.jobName = jobName;
            this.jobUser = jobUser;
            this.jobNumber = jobNumber;
            this.jobStatus = jobStatus;
            this.elapsedSeconds = elapsedSeconds;
        }
    }

    public IbmiJobService(String host, String user, String password) {
        this.connectionUrl = "jdbc:as400://" + host;
        this.user = user;
        this.password = password;
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("IBM AS400 JDBC Driver not found.", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl, user, password);
    }

    /**
     * Returns all active jobs matching jobName (and optionally jobUser if not "*").
     * Elapsed time is calculated from JOB_ACTIVE_TIME (timestamp when job became active).
     */
    public List<ActiveJobInfo> getActiveJobs(String jobName, String jobUser) throws SQLException {
        List<ActiveJobInfo> results = new ArrayList<>();

        boolean matchAnyUser = "*".equals(jobUser);

        String sql = matchAnyUser
            ? "SELECT JOB_NAME, JOB_USER, JOB_NUMBER, JOB_STATUS, " +
              "TIMESTAMPDIFF(256, CHAR(TIMESTAMP(NOW()) - JOB_ACTIVE_TIME)) AS ELAPSED_SECONDS " +
              "FROM QSYS2.ACTIVE_JOB_INFO " +
              "WHERE JOB_NAME = ? AND JOB_STATUS = 'ACTIVE'"
            : "SELECT JOB_NAME, JOB_USER, JOB_NUMBER, JOB_STATUS, " +
              "TIMESTAMPDIFF(256, CHAR(TIMESTAMP(NOW()) - JOB_ACTIVE_TIME)) AS ELAPSED_SECONDS " +
              "FROM QSYS2.ACTIVE_JOB_INFO " +
              "WHERE JOB_NAME = ? AND JOB_USER = ? AND JOB_STATUS = 'ACTIVE'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobName);
            if (!matchAnyUser) {
                ps.setString(2, jobUser);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new ActiveJobInfo(
                        rs.getString("JOB_NAME"),
                        rs.getString("JOB_USER"),
                        rs.getString("JOB_NUMBER"),
                        rs.getString("JOB_STATUS"),
                        rs.getLong("ELAPSED_SECONDS")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL error querying active jobs for " + jobName + "/" + jobUser + ": " + e.getMessage(), e);
            throw e;
        }

        return results;
    }
}
