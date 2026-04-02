package com.islandpacific.monitoring.ibmifilemembermonitor;

import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMemberMetricsExporter {
    private static final Logger logger = Logger.getLogger(FileMemberMetricsExporter.class.getName());
    private HTTPServer httpServer;

    // Corrected constructor: It now takes the port and directly starts the HTTP server
    public FileMemberMetricsExporter(int port) {
        try {
            this.httpServer = new HTTPServer(port);
            logger.info("Prometheus metrics server started on port " + port);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start Prometheus metrics server on port " + port + ": " + e.getMessage(), e);
            // It's critical if the metrics server can't start, so re-throw as a RuntimeException
            throw new RuntimeException("Could not start Prometheus metrics server", e);
        }
    }

    // This method is now responsible only for stopping the server
    public void stopMetricsServer() {
        if (httpServer != null) {
            httpServer.stop();
            logger.info("Prometheus metrics server stopped.");
        }
    }

    // The 'startMetricsServer()' method is no longer needed/used as the server starts in the constructor.
    // If you had a previous 'startMetricsServer(int port)' method, you can now remove it.
}
