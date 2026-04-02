package com.islandpacific.monitoring.userprofilechecker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * OAuth2 Token Provider for Microsoft 365 / Azure AD authentication
 */
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
        
        // Use provided tokenUrl if available, otherwise build from tenantId
        if (tokenUrl != null && !tokenUrl.isEmpty()) {
            this.tokenEndpoint = tokenUrl;
        } else {
            String endpoint = "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token";
            this.tokenEndpoint = endpoint.replace("{tenant}", tenantId);
        }
    }
    
    /**
     * Gets a valid access token, refreshing if necessary
     */
    public synchronized String getAccessToken() throws IOException {
        // Check if we have a valid cached token
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedAccessToken;
        }
        
        // Request a new token
        return requestNewToken();
    }
    
    /**
     * Requests a new access token from Microsoft OAuth2 endpoint
     */
    private String requestNewToken() throws IOException {
        logger.info("Requesting new OAuth2 access token from Microsoft...");
        
        URL url = new URL(tokenEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            // Build request body
            String requestBody = String.format(
                "client_id=%s&client_secret=%s&scope=%s&grant_type=client_credentials",
                java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8)
            );
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorResponse = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("OAuth2 token request failed with code " + responseCode + ": " + errorResponse);
            }
            
            String responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            String accessToken = jsonResponse.get("access_token").getAsString();
            int expiresIn = jsonResponse.get("expires_in").getAsInt();
            
            // Cache the token (subtract 60 seconds for safety margin)
            this.cachedAccessToken = accessToken;
            this.tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000L);
            
            logger.info("Successfully obtained OAuth2 access token (expires in " + expiresIn + " seconds)");
            return accessToken;
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Clears the cached token, forcing a refresh on next request
     */
    public void clearCache() {
        this.cachedAccessToken = null;
        this.tokenExpiryTime = 0;
    }
}

