package com.islandpacific.monitoring.filesystemerrormonitoring;

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
    
    /**
     * Creates a new OAuth2TokenProvider.
     * 
     * @param tenantId Azure AD tenant ID
     * @param clientId Application (client) ID
     * @param clientSecret Client secret value
     * @param scope OAuth2 scope (typically https://graph.microsoft.com/.default)
     * @param tokenUrl Custom token URL (optional, will use default Azure AD URL if empty)
     */
    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret, String scope, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        
        // Use custom token URL if provided, otherwise construct default Azure AD URL
        if (tokenUrl != null && !tokenUrl.trim().isEmpty()) {
            this.tokenUrl = tokenUrl.trim();
        } else {
            this.tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        }
    }
    
    /**
     * Gets an access token, using cached token if still valid.
     * 
     * @return Access token for Microsoft Graph API
     * @throws Exception if token acquisition fails
     */
    public synchronized String getAccessToken() throws Exception {
        // Check if cached token is still valid (with 5-minute buffer)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpirationTime - 300000) {
            return cachedToken;
        }
        
        return refreshAccessToken();
    }
    
    /**
     * Refreshes the access token from Azure AD.
     * 
     * @return New access token
     * @throws Exception if token refresh fails
     */
    private String refreshAccessToken() throws Exception {
        LOGGER.info("Acquiring new OAuth2 access token...");
        
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            // Build request body
            String requestBody = String.format(
                "client_id=%s&scope=%s&client_secret=%s&grant_type=client_credentials",
                java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString()),
                java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()),
                java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8.toString())
            );
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                
                // Parse JSON response
                String jsonResponse = response.toString();
                cachedToken = extractJsonValue(jsonResponse, "access_token");
                String expiresIn = extractJsonValue(jsonResponse, "expires_in");
                
                if (cachedToken == null || cachedToken.isEmpty()) {
                    throw new Exception("No access_token in response");
                }
                
                // Set expiration time
                long expiresInSeconds = 3600; // Default to 1 hour
                if (expiresIn != null && !expiresIn.isEmpty()) {
                    try {
                        expiresInSeconds = Long.parseLong(expiresIn);
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Could not parse expires_in value: " + expiresIn);
                    }
                }
                tokenExpirationTime = System.currentTimeMillis() + (expiresInSeconds * 1000);
                
                LOGGER.info("Successfully acquired OAuth2 access token. Expires in " + expiresInSeconds + " seconds.");
                return cachedToken;
                
            } else {
                // Read error response
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                }
                
                String errorMsg = "Failed to acquire OAuth2 token. HTTP " + responseCode + ": " + errorResponse.toString();
                LOGGER.log(Level.SEVERE, errorMsg);
                throw new Exception(errorMsg);
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Simple JSON value extractor (to avoid external JSON library dependency).
     * 
     * @param json JSON string
     * @param key Key to extract
     * @return Extracted value or null
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) {
            return null;
        }
        
        if (json.charAt(valueStart) == '"') {
            // String value
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) {
                return null;
            }
            return json.substring(valueStart + 1, valueEnd);
        } else {
            // Number or other value
            int valueEnd = valueStart;
            while (valueEnd < json.length() && !Character.isWhitespace(json.charAt(valueEnd)) 
                    && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        }
    }
    
    /**
     * Invalidates the cached token, forcing a refresh on next getAccessToken() call.
     */
    public synchronized void invalidateToken() {
        cachedToken = null;
        tokenExpirationTime = 0;
        LOGGER.info("OAuth2 token cache invalidated.");
    }
}
