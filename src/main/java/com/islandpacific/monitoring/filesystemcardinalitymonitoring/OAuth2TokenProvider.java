package com.islandpacific.monitoring.filesystemcardinalitymonitoring;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * OAuth2 Token Provider for Microsoft Graph API authentication.
 * Handles token acquisition and caching for Windows File System Cardinality Monitor.
 */
public class OAuth2TokenProvider {
    
    private static final Logger logger = Logger.getLogger(OAuth2TokenProvider.class.getName());
    
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String tokenUrl;
    
    private String cachedAccessToken;
    private long tokenExpiryTime;
    
    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret, String scope, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenUrl = (tokenUrl != null && !tokenUrl.isEmpty()) 
            ? tokenUrl 
            : String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId);
    }
    
    /**
     * Gets an access token, either from cache or by requesting a new one.
     * @return The access token string.
     * @throws IOException If token acquisition fails.
     */
    public synchronized String getAccessToken() throws IOException {
        // Return cached token if still valid (with 5-minute buffer)
        if (cachedAccessToken != null && System.currentTimeMillis() < (tokenExpiryTime - 300000)) {
            return cachedAccessToken;
        }
        
        // Request new token
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        String requestBody = String.format(
            "client_id=%s&client_secret=%s&scope=%s&grant_type=client_credentials",
            java.net.URLEncoder.encode(clientId, "UTF-8"),
            java.net.URLEncoder.encode(clientSecret, "UTF-8"),
            java.net.URLEncoder.encode(scope, "UTF-8")
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String responseBody = scanner.useDelimiter("\\A").next();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                
                cachedAccessToken = jsonResponse.get("access_token").getAsString();
                int expiresIn = jsonResponse.get("expires_in").getAsInt();
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000L);
                
                logger.info("Successfully acquired new OAuth2 access token.");
                return cachedAccessToken;
            }
        } else {
            String errorResponse;
            try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                errorResponse = scanner.useDelimiter("\\A").next();
            }
            throw new IOException("Failed to acquire OAuth2 token. Response code: " + responseCode + ", Error: " + errorResponse);
        }
    }
}
