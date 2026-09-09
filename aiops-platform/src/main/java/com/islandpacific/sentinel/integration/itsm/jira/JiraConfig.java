package com.islandpacific.sentinel.integration.itsm.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;

/**
 * Per-tenant Jira settings parsed from {@code integration_config.config_json} (kind = "jira"):
 * {@code {"baseUrl","email","apiToken","projectKey","issueType"}} ({@code issueType} optional,
 * defaults to "Task"). {@code apiToken} may be DPAPI-wrapped or plaintext - resolved once here via
 * {@link SecretProtector}, matching the *Config.java convention: throws on any missing required field.
 */
public final class JiraConfig {

    private final String baseUrl;
    private final String email;
    private final String apiToken;
    private final String projectKey;
    private final String issueType;

    private JiraConfig(String baseUrl, String email, String apiToken, String projectKey, String issueType) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.apiToken = apiToken;
        this.projectKey = projectKey;
        this.issueType = issueType;
    }

    public static JiraConfig fromJson(ObjectMapper mapper, String configJson, SecretProtector secretProtector) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Jira integration_config.config_json is empty");
        }
        JsonNode node;
        try {
            node = mapper.readTree(configJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Jira integration_config.config_json is not valid JSON: " + e.getMessage());
        }

        String baseUrl = requireText(node, "baseUrl");
        String email = requireText(node, "email");
        String rawApiToken = requireText(node, "apiToken");
        String projectKey = requireText(node, "projectKey");
        String issueType = optionalText(node, "issueType", "Task");

        String resolvedApiToken = secretProtector != null ? secretProtector.resolve(rawApiToken) : rawApiToken;
        String normalizedUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new JiraConfig(normalizedUrl, email, resolvedApiToken, projectKey, issueType);
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Jira integration config missing required field: " + field);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull() || value.asText().isBlank()) ? defaultValue : value.asText();
    }

    public String getBaseUrl() { return baseUrl; }
    public String getEmail() { return email; }
    public String getApiToken() { return apiToken; }
    public String getProjectKey() { return projectKey; }
    public String getIssueType() { return issueType; }
}
