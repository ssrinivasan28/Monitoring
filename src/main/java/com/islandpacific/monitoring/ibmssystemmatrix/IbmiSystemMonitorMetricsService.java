package com.islandpacific.monitoring.ibmssystemmatrix;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.SystemStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IbmiSystemMonitorMetricsService {

    private static final Logger logger = Logger.getLogger(IbmiSystemMonitorMetricsService.class.getName());
    private static final String JDBC_DRIVER = "com.ibm.as400.access.AS400JDBCDriver";
    private static final String JDBC_URL_FORMAT = "jdbc:as400://%s";

    public IbmiSystemMonitorMetricsService() {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("IBM i JDBC Driver missing", e);
        }
    }

    public IbmiSystemMonitorInfo getSystemUtilization(String ibmiHost, String ibmiUser, String ibmiPassword) throws Exception {
        double cpuUtil = 0.0, aspUtil = 0.0, sharedPoolUtil = 0.0;
        long totalJobs = 0, activeJobs = 0;

        // --- Try SQL for CPU ---
        try (Connection conn = DriverManager.getConnection(String.format(JDBC_URL_FORMAT, ibmiHost), ibmiUser, ibmiPassword);
             PreparedStatement ps = conn.prepareStatement("SELECT AVERAGE_CPU_UTILIZATION FROM QSYS2.SYSTEM_STATUS_INFO");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) cpuUtil = rs.getDouble("AVERAGE_CPU_UTILIZATION");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQL CPU query failed: " + e.getMessage(), e);
        }

        // --- SystemStatus fallback ---
        AS400 as400 = new AS400(ibmiHost, ibmiUser, ibmiPassword);
        try {
            as400.connectService(AS400.COMMAND);
            SystemStatus st = new SystemStatus(as400);

            if (cpuUtil <= 0.0) {
                cpuUtil = st.getPercentProcessingUnitUsed();
                logger.info("CPU fallback via SystemStatus: " + cpuUtil);
            }

            aspUtil = st.getPercentSystemASPUsed();
            sharedPoolUtil = st.getPercentSharedProcessorPoolUsed();
            totalJobs = st.getJobsInSystem();
            activeJobs = st.getActiveJobsInSystem();

            if (sharedPoolUtil < 0) {
                logger.warning("Shared Pool utilization not available, setting to 0.");
                sharedPoolUtil = 0.0;
            }
        } finally {
            as400.disconnectAllServices();
        }

        return new IbmiSystemMonitorInfo(ibmiHost, cpuUtil, aspUtil, sharedPoolUtil, totalJobs, activeJobs);
    }
}
