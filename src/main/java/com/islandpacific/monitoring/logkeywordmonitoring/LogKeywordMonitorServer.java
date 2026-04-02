package com.islandpacific.monitoring.logkeywordmonitoring;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * HTTP server for Prometheus metrics endpoint.
 */
public class LogKeywordMonitorServer {

    private final Logger logger;
    private final int port;
    private HTTPServer server;

    public LogKeywordMonitorServer(Logger logger, int port, LogKeywordMonitorMetrics metrics) throws IOException {
        this.logger = logger;
        this.port = port;

        // Register metrics
        metrics.register();

        // Register JVM metrics
        DefaultExports.initialize();
    }

    public void start() throws IOException {
        server = new HTTPServer(port);
        logger.info("Metrics server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop();
            logger.info("Metrics server stopped");
        }
    }
}
