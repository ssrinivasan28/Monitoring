package com.islandpacific.monitoring.folderkeywordmonitoring;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.util.logging.Logger;

public class FolderKeywordMonitorServer {

    private final Logger logger;
    private final int port;
    private HTTPServer server;

    public FolderKeywordMonitorServer(Logger logger, int port, FolderKeywordMonitorMetrics metrics) throws IOException {
        this.logger = logger;
        this.port = port;
        metrics.register();
        DefaultExports.initialize();
    }

    public void start() throws IOException {
        server = new HTTPServer(port);
        logger.info("Metrics server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.close();
            logger.info("Metrics server stopped");
        }
    }
}
