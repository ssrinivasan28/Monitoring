package com.islandpacific.monitoring.ibmiifsmonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IFSMonitorAppServer { // Renamed from MetricsServer to match error

    private static final String METRICS_PATH = "/metrics";
    private HttpServer server;
    private final Logger parentLogger; // To log messages to the main application logger

    public IFSMonitorAppServer(Logger parentLogger, int port) {
        this.parentLogger = parentLogger;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext(METRICS_PATH, httpExchange -> {
                // Set the Content-Type header for Prometheus exposition format
                httpExchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
                // Send HTTP 200 OK response header without specifying content length
                httpExchange.sendResponseHeaders(200, 0);

                try (OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody())) {
                    // Use the Prometheus client's TextFormat utility to write all registered metrics
                    // to the response body. This automatically pulls metrics from the default registry.
                    TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
                } catch (Exception e) {
                    parentLogger.log(Level.SEVERE, "Error while generating or writing metrics to HTTP response: " + e.getMessage(), e);
                    // Important: Do not re-throw here, as it might leave the exchange open
                } finally {
                    httpExchange.close(); // Ensure the exchange is closed
                }
            });
            // Use a single thread executor for handling metrics requests to avoid resource contention
            this.server.setExecutor(Executors.newSingleThreadExecutor());
            parentLogger.info("Metrics server initialized on port " + port + METRICS_PATH);
        } catch (IOException e) {
            parentLogger.log(Level.SEVERE, "Failed to create Metrics HTTP server on port " + port + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to start metrics server", e); // Re-throw as runtime exception to signal critical failure
        }
    }


    public void start() {
        if (server != null) {
            server.start();
            parentLogger.info("Metrics server started.");
        }
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0); // Stop immediately with a zero delay
            parentLogger.info("Metrics server stopped.");
        }
    }
}
