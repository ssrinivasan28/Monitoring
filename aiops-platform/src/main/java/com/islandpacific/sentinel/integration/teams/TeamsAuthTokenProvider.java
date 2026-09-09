package com.islandpacific.sentinel.integration.teams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-credentials Azure AD token acquisition for the Teams/Graph API, mirroring the existing
 * monitors' {@code OAuth2TokenProvider} pattern (see CLAUDE.md). Unlike that single-tenant JVM
 * process, this platform is multi-tenant in one JVM, so tokens are cached per (AAD tenant, client)
 * pair rather than as a single static field.
 */
@Component
public class TeamsAuthTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TeamsAuthTokenProvider.class);
    private static final String SCOPE = "https://graph.microsoft.com/.default";

    private final RestTemplate restTemplate;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    public TeamsAuthTokenProvider(TeamsNotificationProperties properties) {
        this(createRestTemplate(properties.getTimeoutMs()));
    }

    TeamsAuthTokenProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        return new RestTemplate(factory);
    }

    public String getAccessToken(TeamsChannelConfig config) {
        String key = config.getAadTenantId() + "|" + config.getClientId();
        CachedToken cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return cached.token;
        }
        return requestNewToken(key, config);
    }

    // Coarse-grained lock (like OAuth2TokenProvider#getAccessToken): a refresh briefly blocks other
    // tenants' refreshes too, but tokens are cached for ~55 minutes so contention is negligible.
    private synchronized String requestNewToken(String key, TeamsChannelConfig config) {
        CachedToken cached = cache.get(key);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return cached.token; // refreshed by another thread while we waited for the lock
        }

        log.info("Requesting new Azure AD access token for Teams integration (aadTenantId={})", config.getAadTenantId());
        String tokenUrl = "https://login.microsoftonline.com/" + config.getAadTenantId() + "/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());
        form.add("scope", SCOPE);
        form.add("grant_type", "client_credentials");

        try {
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || body.get("access_token") == null) {
                throw new TeamsIntegrationException("Azure AD token response missing access_token");
            }
            String token = String.valueOf(body.get("access_token"));
            int expiresIn = body.get("expires_in") instanceof Number ? ((Number) body.get("expires_in")).intValue() : 3600;
            Instant expiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
            cache.put(key, new CachedToken(token, expiresAt));
            return token;
        } catch (RestClientException e) {
            throw new TeamsIntegrationException("Failed to obtain an Azure AD token for the Teams integration: " + e.getMessage(), e);
        }
    }

    private static final class CachedToken {
        private final String token;
        private final Instant expiresAt;

        private CachedToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }
}
