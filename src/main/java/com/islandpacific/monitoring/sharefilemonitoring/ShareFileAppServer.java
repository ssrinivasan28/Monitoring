package com.islandpacific.monitoring.sharefilemonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareFileAppServer {

    private final HttpServer server;
    private final Logger logger;

    public ShareFileAppServer(Logger logger, int port) throws IOException {
        this.logger = logger;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/metrics", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter writer = new OutputStreamWriter(exchange.getResponseBody())) {
                TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error writing metrics: " + e.getMessage(), e);
            } finally {
                exchange.close();
            }
        });
        this.server.setExecutor(Executors.newSingleThreadExecutor());
        logger.info("ShareFile metrics server initialized on port " + port);
    }

    public void start() {
        server.start();
        logger.info("ShareFile metrics server started.");
    }

    public void stop() {
        server.stop(0);
        logger.info("ShareFile metrics server stopped.");
    }
}
