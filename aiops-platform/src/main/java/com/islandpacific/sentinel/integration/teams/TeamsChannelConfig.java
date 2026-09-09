package com.islandpacific.sentinel.integration.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.security.SecretProtector;

/**
 * Per-tenant Teams settings parsed from {@code integration_config.config_json} (kind = "teams"):
 * {@code {"aadTenantId","clientId","clientSecret","teamId","channelId"}}. {@code clientSecret} may
 * be DPAPI-wrapped or plaintext - resolved once here via {@link SecretProtector}, matching the
 * *Config.java convention: throws on any missing required field.
 */
public final class TeamsChannelConfig {

    private final String aadTenantId;
    private final String clientId;
    private final String clientSecret;
    private final String teamId;
    private final String channelId;

    private TeamsChannelConfig(String aadTenantId, String clientId, String clientSecret, String teamId, String channelId) {
        this.aadTenantId = aadTenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.teamId = teamId;
        this.channelId = channelId;
    }

    public static TeamsChannelConfig fromJson(ObjectMapper mapper, String configJson, SecretProtector secretProtector) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Teams integration_config.config_json is empty");
        }
        JsonNode node;
        try {
            node = mapper.readTree(configJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Teams integration_config.config_json is not valid JSON: " + e.getMessage());
        }

        String aadTenantId = requireText(node, "aadTenantId");
        String clientId = requireText(node, "clientId");
        String rawSecret = requireText(node, "clientSecret");
        String teamId = requireText(node, "teamId");
        String channelId = requireText(node, "channelId");

        String resolvedSecret = secretProtector != null ? secretProtector.resolve(rawSecret) : rawSecret;
        return new TeamsChannelConfig(aadTenantId, clientId, resolvedSecret, teamId, channelId);
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Teams integration config missing required field: " + field);
        }
        return value.asText();
    }

    public String getAadTenantId() { return aadTenantId; }
    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getTeamId() { return teamId; }
    public String getChannelId() { return channelId; }
}
