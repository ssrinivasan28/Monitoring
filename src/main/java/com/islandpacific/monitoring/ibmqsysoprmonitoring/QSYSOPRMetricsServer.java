package com.islandpacific.monitoring.ibmqsysoprmonitoring;

import io.prometheus.client.exporter.HTTPServer;
// import io.prometheus.client.hotspot.DefaultExports; // This import is no longer needed after removing initialize() call
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger; // Import the JUL Logger

public class QSYSOPRMetricsServer {

    private static final Logger LOGGER = com.islandpacific.monitoring.common.AppLogger.getLogger(); // Use AppLogger for consistency
    private final int port;
    private HTTPServer server;

   
    public QSYSOPRMetricsServer(int port) {
        this.port = port;
    }

 
    public void startServer() {
        try {
        

            server = new HTTPServer(port);
            LOGGER.info("Prometheus metrics server starting on port: " + port);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not start Prometheus metrics server on port " + port + ": " + e.getMessage(), e);
            // Consider setting monitor status to error if metrics server fails to start
            QSYSOPRMonitorMetrics.setMonitorStopped();
        }
    }

    public void stopServer() {
        if (server != null) {
            server.close();
        }
    }
}
