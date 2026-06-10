package com.islandpacific.monitoring.sslcertmonitor;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.islandpacific.monitoring.common.AppLogger;

public class SSLCertMonitorService {

    private static final Logger logger = AppLogger.getLogger();

    public SSLCertInfo checkCertificate(SSLCertMonitorConfig.HostEntry entry) {
        String host = entry.host;
        int port = entry.port;
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(10000);
                socket.startHandshake();

                SSLSession session = socket.getSession();
                X509Certificate[] certs = (X509Certificate[]) session.getPeerCertificates();

                if (certs == null || certs.length == 0) {
                    return new SSLCertInfo(host, port, "No certificates returned");
                }

                X509Certificate leafCert = certs[0];
                Date expiryDate = leafCert.getNotAfter();
                long nowMs = System.currentTimeMillis();
                long daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(expiryDate.getTime() - nowMs);

                logger.info("Checked " + host + ":" + port + " — expires " + expiryDate + " (" + daysUntilExpiry + " days)");
                return new SSLCertInfo(host, port, expiryDate, daysUntilExpiry);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to check SSL cert for " + host + ":" + port + " — " + e.getMessage(), e);
            return new SSLCertInfo(host, port, e.getMessage());
        }
    }
}
