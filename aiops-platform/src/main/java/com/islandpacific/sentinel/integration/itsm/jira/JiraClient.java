package com.islandpacific.sentinel.integration.itsm.jira;

import com.islandpacific.sentinel.integration.itsm.ItsmIntegrationException;
import com.islandpacific.sentinel.integration.itsm.ItsmSyncProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Talks to Jira Cloud's REST API v3 to create issues and drive their workflow transitions. */
@Component
public class JiraClient {

    private final RestTemplate restTemplate;

    @Autowired
    public JiraClient(ItsmSyncProperties properties) {
        this(createRestTemplate(properties.getTimeoutMs()));
    }

    JiraClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 10_000);
        return new RestTemplate(factory);
    }

    /** Creates an issue; returns the response body containing at least {@code id}/{@code key}/{@code self}. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createIssue(JiraConfig config, String summary, String description) {
        String url = config.getBaseUrl() + "/rest/api/3/issue";

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("project", Map.of("key", config.getProjectKey()));
        fields.put("summary", summary);
        fields.put("description", adfDocument(description));
        fields.put("issuetype", Map.of("name", config.getIssueType()));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity(Map.of("fields", fields), config), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("key") == null) {
                throw new ItsmIntegrationException("Jira did not return a key for the created issue");
            }
            return body;
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("Jira create issue failed: " + e.getMessage(), e);
        }
    }

    /** Looks up the current status name of an issue. */
    @SuppressWarnings("unchecked")
    public String fetchStatus(JiraConfig config, String issueKey) {
        String url = config.getBaseUrl() + "/rest/api/3/issue/" + issueKey + "?fields=status";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity(null, config), Map.class);
            return extractStatusName(response.getBody());
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("Jira fetch issue status failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transitions an issue to the target status name. Jira has no direct "set status" call - the
     * caller must look up the transition whose destination status matches, then invoke it.
     */
    @SuppressWarnings("unchecked")
    public void transitionToStatus(JiraConfig config, String issueKey, String targetStatusName) {
        String transitionsUrl = config.getBaseUrl() + "/rest/api/3/issue/" + issueKey + "/transitions";
        Map<String, Object> body;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(transitionsUrl, HttpMethod.GET, entity(null, config), Map.class);
            body = response.getBody();
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("Jira fetch transitions failed: " + e.getMessage(), e);
        }

        String transitionId = findTransitionId(body, targetStatusName);
        if (transitionId == null) {
            throw new ItsmIntegrationException(
                    "Jira issue " + issueKey + " has no transition available to reach status: " + targetStatusName);
        }

        try {
            restTemplate.exchange(transitionsUrl, HttpMethod.POST,
                    entity(Map.of("transition", Map.of("id", transitionId)), config), Void.class);
        } catch (RestClientException e) {
            throw new ItsmIntegrationException("Jira transition issue failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String findTransitionId(Map<String, Object> transitionsResponse, String targetStatusName) {
        if (transitionsResponse == null) {
            return null;
        }
        Object rawTransitions = transitionsResponse.get("transitions");
        if (!(rawTransitions instanceof List)) {
            return null;
        }
        for (Object rawTransition : (List<Object>) rawTransitions) {
            if (!(rawTransition instanceof Map)) {
                continue;
            }
            Map<String, Object> transition = (Map<String, Object>) rawTransition;
            Object to = transition.get("to");
            if (to instanceof Map) {
                Object statusName = ((Map<String, Object>) to).get("name");
                if (targetStatusName.equalsIgnoreCase(String.valueOf(statusName))) {
                    return String.valueOf(transition.get("id"));
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractStatusName(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object fields = body.get("fields");
        if (!(fields instanceof Map)) {
            return null;
        }
        Object status = ((Map<String, Object>) fields).get("status");
        if (!(status instanceof Map)) {
            return null;
        }
        Object name = ((Map<String, Object>) status).get("name");
        return name != null ? String.valueOf(name) : null;
    }

    private Map<String, Object> adfDocument(String text) {
        return Map.of(
                "type", "doc",
                "version", 1,
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", text)))));
    }

    private <T> HttpEntity<T> entity(T body, JiraConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(config.getEmail(), config.getApiToken());
        return new HttpEntity<>(body, headers);
    }
}
