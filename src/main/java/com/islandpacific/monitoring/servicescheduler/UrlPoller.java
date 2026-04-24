package com.islandpacific.monitoring.servicescheduler;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class UrlPoller {

    private static final Logger logger = AppLogger.getLogger();

    // Per-connection trust-all SSL factory — scoped to polling only, not JVM-wide
    private static final SSLSocketFactory TRUST_ALL_FACTORY;
    private static final HostnameVerifier TRUST_ALL_VERIFIER = (h, s) -> true;

    static {
        SSLSocketFactory factory = null;
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            factory = sc.getSocketFactory();
        } catch (Exception e) {
            logger.warning("Failed to configure trust-all SSL factory: " + e.getMessage());
        }
        TRUST_ALL_FACTORY = factory;
    }

    /**
     * Polls url every intervalSeconds until HTTP 200 is received or timeoutSeconds elapses.
     *
     * @return true if URL responded with 200 within timeout, false otherwise
     */
    public static boolean waitForReady(String url, int intervalSeconds, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        logger.info("Polling URL until ready: " + url + " (timeout=" + timeoutSeconds + "s)");

        while (System.currentTimeMillis() < deadline) {
            if (check(url)) {
                logger.info("URL is ready: " + url);
                return true;
            }
            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        logger.warning("URL did not become ready within " + timeoutSeconds + "s: " + url);
        return false;
    }

    private static boolean check(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            // Apply trust-all SSL per-connection, not JVM-wide
            if (conn instanceof HttpsURLConnection && TRUST_ALL_FACTORY != null) {
                HttpsURLConnection https = (HttpsURLConnection) conn;
                https.setSSLSocketFactory(TRUST_ALL_FACTORY);
                https.setHostnameVerifier(TRUST_ALL_VERIFIER);
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            // Drain response body so the connection is cleanly released
            try (java.io.InputStream is = conn.getInputStream()) {
                if (is != null) is.transferTo(java.io.OutputStream.nullOutputStream());
            } catch (Exception ignored) {}
            conn.disconnect();
            logger.fine("Poll " + urlStr + " -> HTTP " + code);
            return code == 200;
        } catch (Exception e) {
            logger.fine("Poll " + urlStr + " failed: " + e.getMessage());
            return false;
        }
    }
}
