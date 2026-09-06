package com.islandpacific.monitoring.ibmierrormonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the HTTP server for exposing application metrics.
 */
public class IFSErrorMonitorsServer {

    private final Logger logger;
    private final int metricsPort;
    private static final String METRICS_PATH = "/metrics";

    private HttpServer server;

    public IFSErrorMonitorsServer(Logger logger, int metricsPort, IFSErrorMonitorMetrics metricsService) {
        this.logger = logger;
        this.metricsPort = metricsPort;
    }


    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(metricsPort), 0);
        server.createContext(METRICS_PATH, httpExchange -> {
            httpExchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
            httpExchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody())) {
                TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error writing metrics response: " + e.getMessage(), e);
            } finally {
                httpExchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        logger.info("Metrics server started on port " + metricsPort + METRICS_PATH);
    }

 
    public void stop() {
        if (server != null) {
            server.stop(0); // Stop immediately
            logger.info("Metrics server stopped.");
        }
    }
}
