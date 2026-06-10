package com.islandpacific.monitoring.sslcertmonitor;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSLCertMetrics {

    private final int port;
    private HTTPServer server;

    private static final Gauge DAYS_UNTIL_EXPIRY = Gauge.build()
            .name("ssl_cert_days_until_expiry")
            .help("Number of days until the SSL certificate expires")
            .labelNames("host")
            .register();

    private static final Gauge EXPIRY_TIMESTAMP = Gauge.build()
            .name("ssl_cert_expiry_timestamp")
            .help("Unix timestamp of SSL certificate expiry date")
            .labelNames("host")
            .register();

    private static final Gauge CERT_ERROR = Gauge.build()
            .name("ssl_cert_check_error")
            .help("1 if the certificate check failed, 0 if successful")
            .labelNames("host")
            .register();

    public SSLCertMetrics(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = new HTTPServer(port);
    }

    public void stop() {
        if (server != null) server.close();
    }

    public void updateMetrics(List<SSLCertInfo> results) {
        for (SSLCertInfo info : results) {
            if (info == null) continue;
            String label = info.getHostPort();
            if (info.hasError()) {
                CERT_ERROR.labels(label).set(1);
            } else {
                CERT_ERROR.labels(label).set(0);
                DAYS_UNTIL_EXPIRY.labels(label).set(info.getDaysUntilExpiry());
                EXPIRY_TIMESTAMP.labels(label).set(
                        TimeUnit.MILLISECONDS.toSeconds(info.getExpiryDate().getTime()));
            }
        }
    }
}
