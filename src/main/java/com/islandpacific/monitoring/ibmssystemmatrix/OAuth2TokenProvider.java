package com.islandpacific.monitoring.ibmssystemmatrix;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class OAuth2TokenProvider {
    private static final Logger logger = Logger.getLogger(OAuth2TokenProvider.class.getName());
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
        logger.info("Requesting new OAuth2 access token from Microsoft...");
        URL url = new URL(tokenEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            String requestBody = String.format("client_id=%s&client_secret=%s&scope=%s&grant_type=client_credentials",
                java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8));
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorResponse = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("OAuth2 token request failed with code " + responseCode + ": " + errorResponse);
            }
            String responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            String accessToken = jsonResponse.get("access_token").getAsString();
            int expiresIn = jsonResponse.get("expires_in").getAsInt();
            this.cachedAccessToken = accessToken;
            this.tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000L);
            logger.info("Successfully obtained OAuth2 access token (expires in " + expiresIn + " seconds)");
            return accessToken;
        } finally {
            conn.disconnect();
        }
    }
    
    public void clearCache() {
        this.cachedAccessToken = null;
        this.tokenExpiryTime = 0;
    }
}

