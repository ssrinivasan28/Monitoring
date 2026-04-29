package com.islandpacific.monitoring.sharefilemonitoring;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;

public class OAuth2TokenProvider {

    private static final Logger logger = Logger.getLogger(OAuth2TokenProvider.class.getName());

    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String tokenUrl;

    private String cachedToken;
    private long tokenExpiryMs;

    public OAuth2TokenProvider(String tenantId, String clientId, String clientSecret, String scope, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenUrl = (tokenUrl != null && !tokenUrl.isBlank())
                ? tokenUrl
                : "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
    }

    public synchronized String getAccessToken() throws IOException {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryMs - 300_000) {
            return cachedToken;
        }
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String body = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, "UTF-8")
                + "&scope=" + java.net.URLEncoder.encode(scope, "UTF-8")
                + "&grant_type=client_credentials";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String resp = s.useDelimiter("\\A").next();
                JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
                cachedToken = json.get("access_token").getAsString();
                tokenExpiryMs = System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000L;
                logger.info("OAuth2 token acquired.");
                return cachedToken;
            }
        } else {
            try (Scanner s = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                throw new IOException("Token request failed " + code + ": " + s.useDelimiter("\\A").next());
            }
        }
    }
}
