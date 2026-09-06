package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobStatusService {

    private static final Logger logger = Logger.getLogger(JobStatusService.class.getName());

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;

    private static final String SQL =
        "SELECT JOB_NAME, JOB_USER, JOB_NUMBER, SUBSYSTEM, JOB_STATUS, " +
        "ELAPSED_CPU_TIME / 1000 AS ELAPSED_SECONDS " +
        "FROM QSYS2.ACTIVE_JOB_INFO " +
        "WHERE JOB_STATUS = 'MSGW'";

    public JobStatusService(String ibmiHost, String ibmiUser, String ibmiPassword) {
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("AS400 JDBC Driver not found.", e);
        }
    }

    public List<JobStatusInfo> getMsgwJobs() throws SQLException {
        List<JobStatusInfo> jobs = new ArrayList<>();
        String url = "jdbc:as400://" + ibmiHost + ";naming=system;";
        try (Connection conn = DriverManager.getConnection(url, ibmiUser, ibmiPassword);
             PreparedStatement ps = conn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                jobs.add(new JobStatusInfo(
                    rs.getString("JOB_NAME"),
                    rs.getString("JOB_USER"),
                    rs.getString("JOB_NUMBER"),
                    rs.getString("SUBSYSTEM"),
                    rs.getString("JOB_STATUS"),
                    rs.getLong("ELAPSED_SECONDS")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error querying ACTIVE_JOB_INFO: " + e.getMessage(), e);
            throw e;
        }
        logger.info("Found " + jobs.size() + " job(s) in MSGW status.");
        return jobs;
    }
}
