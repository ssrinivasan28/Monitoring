package com.islandpacific.monitoring.apiurlmonitoring;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiUrlMonitorService {

    private static final Logger logger = com.islandpacific.monitoring.common.AppLogger.getLogger();

    private final EmailService emailService;
    private final ApiUrlMonitorMetrics metrics;

    // Tracks consecutive DOWN count per URL code
    private final Map<String, Integer> downCounts = new HashMap<>();
    // Tracks whether an alert has been sent (to send recovery email)
    private final Map<String, Boolean> alertSent = new HashMap<>();

    public ApiUrlMonitorService(EmailService emailService, ApiUrlMonitorMetrics metrics) {
        this.emailService = emailService;
        this.metrics = metrics;
    }

    public void checkAll(ApiUrlMonitorConfig config) {
        for (ApiUrlMonitorConfig.UrlConfig urlConfig : config.getUrlConfigs()) {
            try {
                checkUrl(urlConfig);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error checking URL [" + urlConfig.code + "]: " + e.getMessage(), e);
            }
        }
    }

    private void checkUrl(ApiUrlMonitorConfig.UrlConfig urlConfig) {
        int statusCode = -1;
        long responseTimeMs = -1;
        boolean isUp = false;

        try {
            long start = System.currentTimeMillis();
            URL url = new URL(urlConfig.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(urlConfig.timeoutMs);
            conn.setReadTimeout(urlConfig.timeoutMs);
            conn.setInstanceFollowRedirects(true);
            if (urlConfig.bearerToken != null && !urlConfig.bearerToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + urlConfig.bearerToken);
            }
            conn.connect();
            statusCode = conn.getResponseCode();
            responseTimeMs = System.currentTimeMillis() - start;
            conn.disconnect();
            isUp = (statusCode == 200);
        } catch (Exception e) {
            logger.warning("[" + urlConfig.code + "] Connection failed: " + e.getMessage());
            isUp = false;
        }

        metrics.update(urlConfig.code, urlConfig.url, isUp ? 1 : 0, responseTimeMs);

        if (isUp) {
            handleRecovery(urlConfig, statusCode);
        } else {
            handleDown(urlConfig, statusCode, responseTimeMs);
        }
    }

    private void handleDown(ApiUrlMonitorConfig.UrlConfig urlConfig, int statusCode, long responseTimeMs) {
        int count = downCounts.getOrDefault(urlConfig.code, 0) + 1;
        downCounts.put(urlConfig.code, count);

        String statusStr = statusCode == -1 ? "Connection Failed" : "HTTP " + statusCode;
        logger.warning("[" + urlConfig.code + "] " + urlConfig.name + " is DOWN (" + statusStr + ") — breach " + count + "/" + urlConfig.breachCount);

        if (count >= urlConfig.breachCount && !Boolean.TRUE.equals(alertSent.get(urlConfig.code))) {
            alertSent.put(urlConfig.code, true);
            String responseInfo = responseTimeMs >= 0 ? responseTimeMs + " ms" : "N/A";
            emailService.sendDownAlert(urlConfig.name, urlConfig.url, statusStr, responseInfo);
        }
    }

    private void handleRecovery(ApiUrlMonitorConfig.UrlConfig urlConfig, int statusCode) {
        int prevCount = downCounts.getOrDefault(urlConfig.code, 0);
        downCounts.put(urlConfig.code, 0);

        logger.info("[" + urlConfig.code + "] " + urlConfig.name + " is UP (HTTP " + statusCode + ")");

        if (Boolean.TRUE.equals(alertSent.get(urlConfig.code))) {
            alertSent.put(urlConfig.code, false);
            emailService.sendRecoveryAlert(urlConfig.name, urlConfig.url, statusCode, prevCount);
        }
    }
}
