package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Talks to Microsoft Graph's channel-message API to post/update an incident's Adaptive Card.
 * Retries once-throttled (HTTP 429) requests with backoff (honoring {@code Retry-After} when
 * present); any other failure - or throttling that exhausts retries - is raised as a
 * {@link TeamsIntegrationException} rather than swallowed. Never logs the access token.
 */
@Component
public class TeamsGraphClient {

    private static final Logger log = LoggerFactory.getLogger(TeamsGraphClient.class);

    private final TeamsNotificationProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public TeamsGraphClient(TeamsNotificationProperties properties) {
        this(properties, createRestTemplate(properties.getTimeoutMs()));
    }

    TeamsGraphClient(TeamsNotificationProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        return new RestTemplate(factory);
    }

    /** Posts a new Adaptive Card to the tenant's configured team/channel; returns the Graph message id. */
    @SuppressWarnings("unchecked")
    public String postCard(TeamsChannelConfig config, String accessToken, ObjectNode card) {
        String url = properties.getGraphBaseUrl() + "/teams/" + config.getTeamId()
                + "/channels/" + config.getChannelId() + "/messages";

        ResponseEntity<Map> response = executeWithRetry(() ->
                restTemplate.exchange(url, HttpMethod.POST, entity(buildMessageBody(card), accessToken), Map.class));

        Object id = response.getBody() != null ? response.getBody().get("id") : null;
        if (id == null) {
            throw new TeamsIntegrationException("Microsoft Graph did not return a message id for the posted card");
        }
        return String.valueOf(id);
    }

    /** Updates the body/attachments of a previously-posted card in place (dedupe: same message, fresh content). */
    public void updateCard(TeamsChannelConfig config, String accessToken, String messageId, ObjectNode card) {
        String url = properties.getGraphBaseUrl() + "/teams/" + config.getTeamId()
                + "/channels/" + config.getChannelId() + "/messages/" + messageId;

        executeWithRetry(() ->
                restTemplate.exchange(url, HttpMethod.PATCH, entity(buildMessageBody(card), accessToken), Void.class));
    }

    /**
     * 2.3 - resolves the Teams-verified Azure AD object id of an inbound ChatOps sender to their
     * user principal name (email), so it can be matched against IP Sentinel's {@code User.email}.
     * Uses the same app-only token as the outbound card calls above (the app registration needs
     * {@code User.Read.All} application permission granted). Never logs the access token.
     */
    @SuppressWarnings("unchecked")
    public String getUserPrincipalName(TeamsChannelConfig config, String accessToken, String aadObjectId) {
        String url = properties.getGraphBaseUrl() + "/users/" + aadObjectId + "?$select=userPrincipalName,mail";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = executeWithRetry(() ->
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class));

        Map<?, ?> body = response.getBody();
        Object upn = body != null ? body.get("userPrincipalName") : null;
        Object mail = body != null ? body.get("mail") : null;
        if (upn != null && !String.valueOf(upn).isBlank()) {
            return String.valueOf(upn);
        }
        if (mail != null && !String.valueOf(mail).isBlank()) {
            return String.valueOf(mail);
        }
        throw new TeamsIntegrationException("Microsoft Graph did not return a userPrincipalName or mail for aadObjectId " + aadObjectId);
    }

    private HttpEntity<Map<String, Object>> entity(Map<String, Object> body, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> buildMessageBody(ObjectNode card) {
        String attachmentId = UUID.randomUUID().toString();

        Map<String, Object> bodyContent = new LinkedHashMap<>();
        bodyContent.put("contentType", "html");
        bodyContent.put("content", "<attachment id=\"" + attachmentId + "\"></attachment>");

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("id", attachmentId);
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.put("content", card.toString());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("body", bodyContent);
        message.put("attachments", List.of(attachment));
        return message;
    }

    private <T> ResponseEntity<T> executeWithRetry(Supplier<ResponseEntity<T>> call) {
        int attempt = 0;
        while (true) {
            try {
                return call.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                attempt++;
                if (attempt > properties.getMaxRetries()) {
                    throw new TeamsIntegrationException(
                            "Microsoft Graph throttled the request after " + attempt + " attempts", e);
                }
                long waitMs = retryAfterMillis(e).orElse(properties.getRetryBackoffMs() * attempt);
                log.warn("Microsoft Graph throttled a Teams request (attempt {}); retrying in {}ms", attempt, waitMs);
                sleep(waitMs);
            } catch (RestClientException e) {
                throw new TeamsIntegrationException("Microsoft Graph request failed: " + e.getMessage(), e);
            }
        }
    }

    private Optional<Long> retryAfterMillis(HttpClientErrorException.TooManyRequests e) {
        String retryAfter = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfter == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(retryAfter.trim()) * 1000L);
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new TeamsIntegrationException("Interrupted while waiting to retry a throttled Microsoft Graph request", ie);
        }
    }
}
