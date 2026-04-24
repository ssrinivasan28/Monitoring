package com.islandpacific.monitoring.ibmifilemembermonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class to interact with IBM i (AS/400) to retrieve file member information.
 */
public class IbmiFileMemberService {

    private static final Logger logger = Logger.getLogger(IbmiFileMemberService.class.getName());

    private final String connectionUrl;

    public IbmiFileMemberService(String host, String user, String password) {
        // The connection URL for IBM i using JTOpen JDBC driver
        this.connectionUrl = "jdbc:as400://" + host + ";user=" + user + ";password=" + password;

        // Ensure the AS400 JDBC driver is loaded
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "IBM AS400 JDBC Driver not found. Please ensure jt400.jar is in your classpath.", e);
            throw new RuntimeException("AS400 JDBC Driver not found.", e);
        }
    }

    /**
     * Establishes a connection to the IBM i system.
     *
     * @return A SQL Connection object.
     * @throws SQLException If a database access error occurs.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl);
    }

    /**
     * Retrieves the record count for a specific physical file member on IBM i.
     * This method queries QSYS2.SYSPARTITIONSTAT for the NUMBER_ROWS.
     *
     * @param library The library where the file resides.
     * @param fileName The name of the physical file.
     * @param memberName The name of the member within the file.
     * @return The number of records in the member, or -1 if not found or an error occurs.
     * @throws SQLException If a database access error occurs.
     */
    public long getMemberRecordCount(String library, String fileName, String memberName) throws SQLException {
        String sql = "SELECT NUMBER_ROWS " +
                     "FROM QSYS2.SYSPARTITIONSTAT " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND TABLE_PARTITION = ?";
        long recordCount = -1;

        // Debug logging for exact parameters and SQL before execution
        String processedLibrary = library.trim().toUpperCase();
        String processedFileName = fileName.trim().toUpperCase();
        String processedMemberName = memberName.trim().toUpperCase();
        logger.info("Attempting to get record count for: Library='" + processedLibrary + "', File='" + processedFileName + "', Member='" + processedMemberName + "'");
        logger.info("SQL Query: " + sql);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, processedLibrary);
            pstmt.setString(2, processedFileName);
            pstmt.setString(3, processedMemberName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    recordCount = rs.getLong("NUMBER_ROWS");
                    logger.info("Found record count for " + library + "/" + fileName + "/" + memberName + ": " + recordCount);
                } else {
                    logger.warning("No record count found for member: " + library + "/" + fileName + "/" + memberName +
                                   ". This might mean the member does not exist or has no records matching the query parameters.");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL Error while getting member record count for " +
                               library + "/" + fileName + "/" + memberName + ": " + e.getMessage(), e);
            throw e;
        }
        return recordCount;
    }

    /**
     * This method is added to satisfy the call from MainApplication's shutdown hook.
     * In this JDBC-based implementation, connections are typically managed per operation
     * using try-with-resources, so there isn't a persistent connection object to close here.
     */
    public void disconnect() {
        logger.info("IbmiFileMemberService disconnect() called. In this JDBC implementation, " +
                    "connections are managed per operation and do not require explicit disconnection here.");
        // No persistent connection to close in this JDBC implementation
    }

    public List<FileMemberInfo> getFileMembers(String library, String fileName) throws SQLException {
        logger.warning("getFileMembers(String, String) is called but its implementation is pending " +
                        "or it's intended for a different use case than specific member monitoring.");
        return new ArrayList<>();
    }
}