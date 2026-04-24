package com.islandpacific.monitoring.ibmsubsystemmonitoring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement; // Use PreparedStatement for safety
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IbmiSubsystemService {

    private static final Logger logger = Logger.getLogger(IbmiSubsystemService.class.getName());

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;


    public IbmiSubsystemService(String ibmiHost, String ibmiUser, String ibmiPassword) {
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
        // Ensure the JDBC driver is loaded when the service is instantiated
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "AS400 JDBC Driver not found. Ensure jt400.jar is in the classpath. " + e.getMessage(), e);
            throw new RuntimeException("AS400 JDBC Driver not found.", e);
        }
    }


    public SubsystemInfo getSubsystemInfo(String subsystemDescription, String subsystemLibrary) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        
        // JDBC URL for IBM i
        String jdbcUrl = String.format("jdbc:as400://%s;user=%s;password=%s;libraries=QSYS2;naming=system;",
                                       ibmiHost, ibmiUser, ibmiPassword);

        try {
            // Establish connection using DriverManager
            connection = DriverManager.getConnection(jdbcUrl);
            logger.fine("Successfully established JDBC connection to IBM i.");

//            String sql = "SELECT SUBSYSTEM_DESCRIPTION, STATUS, SUBSYSTEM_DESCRIPTION_LIBRARY " +
//                         "FROM QSYS2.SUBSYSTEM_INFO " +
//                         "WHERE SUBSYSTEM_DESCRIPTION = ? AND SUBSYSTEM_DESCRIPTION_LIBRARY = ?";
//            
            
            
            String sql = "SELECT SUBSYSTEM_DESCRIPTION, STATUS, SUBSYSTEM_DESCRIPTION_LIBRARY, TEXT_DESCRIPTION " + // Added TEXT_DESCRIPTION
                    "FROM QSYS2/SUBSYSTEM_INFO " + // Changed the dot to a slash
                    "WHERE SUBSYSTEM_DESCRIPTION = ? AND SUBSYSTEM_DESCRIPTION_LIBRARY = ?";
            logger.info("Executing SQL query: " + sql + " with params: '" + subsystemDescription + "', '" + subsystemLibrary + "'");
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, subsystemDescription);
            preparedStatement.setString(2, subsystemLibrary);
            
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String name = resultSet.getString("SUBSYSTEM_DESCRIPTION"); 
                String description = resultSet.getString("TEXT_DESCRIPTION");
                String status = resultSet.getString("STATUS"); 
                String library = resultSet.getString("SUBSYSTEM_DESCRIPTION_LIBRARY");

                logger.info(String.format("Found subsystem %s/%s with status %s.", name, library, status));
                return new SubsystemInfo(name, description, status, library);
            } else {
                logger.info(String.format("Subsystem %s/%s not found.", subsystemDescription, subsystemLibrary));
                return null;
            }

        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("28")) { 
                 logger.log(Level.SEVERE, "IBM i Security Error fetching subsystem info for " + subsystemDescription + "/" + subsystemLibrary + ": Check user ID and password/authority. " + e.getMessage() + " SQLState: " + e.getSQLState(), e);
            } else if (e.getSQLState() != null && e.getSQLState().equals("42703")) { // SQL0206 - Column not found
                 logger.log(Level.SEVERE, "SQL0206 Error: Column not found in QSYS2.SUBSYSTEM_INFO. This typically indicates 'SUBSYSTEM_DESCRIPTION_LIBRARY' column does not exist on your IBM i version. Error: " + e.getMessage() + " SQLState: " + e.getSQLState(), e);
            }
            logger.log(Level.SEVERE, "SQL Error fetching subsystem info for " + subsystemDescription + "/" + subsystemLibrary + ": " + e.getMessage() + " SQLState: " + e.getSQLState(), e);
            throw e; // Re-throw the original SQLException
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while fetching subsystem info for " + subsystemDescription + "/" + subsystemLibrary + ": " + e.getMessage(), e);
            throw e;
        } finally {
            // Close resources in reverse order of creation
            try { if (resultSet != null) resultSet.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Error closing ResultSet: " + e.getMessage(), e); }
            try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Error closing PreparedStatement: " + e.getMessage(), e); }
            try { if (connection != null) connection.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Error closing Connection: " + e.getMessage(), e); }
        }
    }
}