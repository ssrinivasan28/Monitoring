package com.islandpacific.sentinel.integration.itsm.servicenow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;

/**
 * Per-tenant ServiceNow settings parsed from {@code integration_config.config_json} (kind =
 * "servicenow"): {@code {"instanceUrl","username","password","tableName"}} ({@code tableName}
 * optional, defaults to "incident"). {@code password} may be DPAPI-wrapped or plaintext - resolved
 * once here via {@link SecretProtector}, matching the *Config.java convention: throws on any
 * missing required field.
 */
public final class ServiceNowConfig {

    private final String instanceUrl;
    private final String username;
    private final String password;
    private final String tableName;

    private ServiceNowConfig(String instanceUrl, String username, String password, String tableName) {
        this.instanceUrl = instanceUrl;
        this.username = username;
        this.password = password;
        this.tableName = tableName;
    }

    public static ServiceNowConfig fromJson(ObjectMapper mapper, String configJson, SecretProtector secretProtector) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("ServiceNow integration_config.config_json is empty");
        }
        JsonNode node;
        try {
            node = mapper.readTree(configJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("ServiceNow integration_config.config_json is not valid JSON: " + e.getMessage());
        }

        String instanceUrl = requireText(node, "instanceUrl");
        String username = requireText(node, "username");
        String rawPassword = requireText(node, "password");
        String tableName = optionalText(node, "tableName", "incident");

        String resolvedPassword = secretProtector != null ? secretProtector.resolve(rawPassword) : rawPassword;
        String normalizedUrl = instanceUrl.endsWith("/") ? instanceUrl.substring(0, instanceUrl.length() - 1) : instanceUrl;
        return new ServiceNowConfig(normalizedUrl, username, resolvedPassword, tableName);
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("ServiceNow integration config missing required field: " + field);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull() || value.asText().isBlank()) ? defaultValue : value.asText();
    }

    public String getInstanceUrl() { return instanceUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getTableName() { return tableName; }
}
