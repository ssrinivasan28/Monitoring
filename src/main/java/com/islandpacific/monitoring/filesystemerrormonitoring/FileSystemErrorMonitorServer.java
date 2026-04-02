package com.islandpacific.monitoring.filesystemerrormonitoring;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Manages the HTTP server for exposing application metrics for Windows File System Error Monitor.
 */
public class FileSystemErrorMonitorServer {

    private final Logger logger;
    private final int metricsPort;
    private final FileSystemErrorMetrics metricsService;
    private static final String METRICS_PATH = "/metrics";

    private HttpServer server;

    public FileSystemErrorMonitorServer(Logger logger, int metricsPort, FileSystemErrorMetrics metricsService) {
        this.logger = logger;
        this.metricsPort = metricsPort;
        this.metricsService = metricsService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(metricsPort), 0);
        server.createContext(METRICS_PATH, httpExchange -> {
            String response = metricsService.generateMetrics();
            httpExchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            httpExchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        logger.info("Metrics server started on port " + metricsPort + METRICS_PATH);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Metrics server stopped.");
        }
    }
}
