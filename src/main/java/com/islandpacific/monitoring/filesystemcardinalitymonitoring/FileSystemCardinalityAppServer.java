package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP server for exposing Prometheus metrics for Windows File System Cardinality Monitor.
 */
public class FileSystemCardinalityAppServer {

    private static final Logger logger = Logger.getLogger(FileSystemCardinalityAppServer.class.getName());
    private static final String METRICS_PATH = "/metrics";
    private HttpServer server;
    private final Logger parentLogger;

    public FileSystemCardinalityAppServer(Logger parentLogger, int port) {
        this.parentLogger = parentLogger;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext(METRICS_PATH, httpExchange -> {
                httpExchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
                httpExchange.sendResponseHeaders(200, 0);

                try (OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody())) {
                    TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
                } catch (Exception e) {
                    parentLogger.log(Level.SEVERE, "Error while generating or writing metrics to HTTP response: " + e.getMessage(), e);
                } finally {
                    httpExchange.close();
                }
            });
            this.server.setExecutor(Executors.newSingleThreadExecutor());
            parentLogger.info("Metrics server initialized on port " + port + METRICS_PATH);
        } catch (IOException e) {
            parentLogger.log(Level.SEVERE, "Failed to create Metrics HTTP server on port " + port + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to start metrics server", e);
        }
    }

    public void start() {
        if (server != null) {
            server.start();
            parentLogger.info("Metrics server started.");
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            parentLogger.info("Metrics server stopped.");
        }
    }
}
