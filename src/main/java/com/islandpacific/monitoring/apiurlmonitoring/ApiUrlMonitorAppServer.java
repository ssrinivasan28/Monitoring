package com.islandpacific.monitoring.apiurlmonitoring;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ApiUrlMonitorAppServer {

    private static final Logger logger = Logger.getLogger(ApiUrlMonitorAppServer.class.getName());

    private final int port;
    private HttpServer server;

    public ApiUrlMonitorAppServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
                byte[] response;
                try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                     OutputStreamWriter writer = new OutputStreamWriter(baos)) {
                    TextFormat.write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples());
                    writer.flush();
                    response = baos.toByteArray();
                }
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; }));
        server.start();
        logger.info("API URL Monitor metrics server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            logger.info("API URL Monitor metrics server stopped.");
        }
    }
}
