package com.islandpacific.monitoring.folderkeywordmonitoring;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAuth2TokenProvider {

    private static final Logger LOGGER = Logger.getLogger(OAuth2TokenProvider.class.getName());

    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String tokenUrl;

    private String cachedToken;
    private long tokenExpirationTime;

    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret, String scope, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        if (tokenUrl != null && !tokenUrl.trim().isEmpty()) {
            this.tokenUrl = tokenUrl.trim();
        } else {
            this.tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        }
    }

    public synchronized String getAccessToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpirationTime - 300000) {
            return cachedToken;
        }
        return refreshAccessToken();
    }

    private String refreshAccessToken() throws Exception {
        LOGGER.info("Acquiring new OAuth2 access token...");
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String requestBody = String.format(
                    "client_id=%s&scope=%s&client_secret=%s&grant_type=client_credentials",
                    java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString()),
                    java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()),
                    java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8.toString()));

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) response.append(line.trim());
                }
                String jsonResponse = response.toString();
                cachedToken = extractJsonValue(jsonResponse, "access_token");
                String expiresIn = extractJsonValue(jsonResponse, "expires_in");
                if (cachedToken == null || cachedToken.isEmpty()) {
                    throw new Exception("No access_token in response");
                }
                long expiresInSeconds = 3600;
                if (expiresIn != null && !expiresIn.isEmpty()) {
                    try { expiresInSeconds = Long.parseLong(expiresIn); } catch (NumberFormatException ignored) {}
                }
                tokenExpirationTime = System.currentTimeMillis() + (expiresInSeconds * 1000);
                LOGGER.info("OAuth2 token acquired. Expires in " + expiresInSeconds + "s.");
                return cachedToken;
            } else {
                StringBuilder err = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) err.append(line.trim());
                }
                String msg = "Failed to acquire OAuth2 token. HTTP " + responseCode + ": " + err;
                LOGGER.log(Level.SEVERE, msg);
                throw new Exception(msg);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && !Character.isWhitespace(json.charAt(valueEnd))
                    && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') valueEnd++;
            return json.substring(valueStart, valueEnd);
        }
    }

    public synchronized void invalidateToken() {
        cachedToken = null;
        tokenExpirationTime = 0;
    }
}
