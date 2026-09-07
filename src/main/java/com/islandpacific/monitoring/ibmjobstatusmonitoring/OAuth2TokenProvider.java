package com.islandpacific.monitoring.ibmjobstatusmonitoring;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAuth2TokenProvider {

    private static final Logger logger = Logger.getLogger(OAuth2TokenProvider.class.getName());
    private static final long EXPIRY_BUFFER_SECONDS = 300;

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String tokenUrl;

    private String cachedToken;
    private long tokenExpiryEpochSeconds;

    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret,
                               String scope, String tokenUrl) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenUrl = (tokenUrl != null && !tokenUrl.isEmpty())
                ? tokenUrl
                : "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
    }

    public String getTenantId() {
        return tenantId;
    }

    public synchronized String getAccessToken() throws IOException {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now < tokenExpiryEpochSeconds - EXPIRY_BUFFER_SECONDS) {
            return cachedToken;
        }
        String body = "grant_type=client_credentials"
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&scope=" + scope;
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Token request failed: HTTP " + status);
        }
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
            String response = scanner.useDelimiter("\\A").next();
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            cachedToken = json.get("access_token").getAsString();
            long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
            tokenExpiryEpochSeconds = now + expiresIn;
            logger.info("OAuth2 token refreshed, expires in " + expiresIn + "s.");
            return cachedToken;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse token response: " + e.getMessage(), e);
            throw new IOException("Failed to parse token response", e);
        }
    }
}
