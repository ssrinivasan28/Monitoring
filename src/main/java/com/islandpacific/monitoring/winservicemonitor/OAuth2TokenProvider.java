package com.islandpacific.monitoring.winservicemonitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class OAuth2TokenProvider {

    private static final Logger logger = AppLogger.getLogger();

    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String tokenEndpoint;

    private String cachedAccessToken;
    private long tokenExpiryTime;

    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret, String scope, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope != null && !scope.isEmpty() ? scope : "https://graph.microsoft.com/.default";

        if (tokenUrl != null && !tokenUrl.isEmpty()) {
            this.tokenEndpoint = tokenUrl;
        } else {
            this.tokenEndpoint = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        }
    }

    public synchronized String getAccessToken() throws IOException {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedAccessToken;
        }
        return requestNewToken();
    }

    private String requestNewToken() throws IOException {
        logger.info("Requesting new OAuth2 access token...");

        URL url = new URL(tokenEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String body = String.format(
                    "client_id=%s&client_secret=%s&scope=%s&grant_type=client_credentials",
                    java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                java.io.InputStream errStream = conn.getErrorStream();
                String err = errStream != null
                        ? new String(errStream.readAllBytes(), StandardCharsets.UTF_8)
                        : "(no error body)";
                throw new IOException("OAuth2 token request failed " + responseCode + ": " + err);
            }

            String responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            cachedAccessToken = json.get("access_token").getAsString();
            int expiresIn = json.get("expires_in").getAsInt();
            tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000L);

            logger.info("OAuth2 token obtained (expires in " + expiresIn + "s)");
            return cachedAccessToken;
        } finally {
            conn.disconnect();
        }
    }
}
