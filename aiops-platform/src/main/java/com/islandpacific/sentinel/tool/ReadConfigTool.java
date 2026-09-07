package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.IntegrationConfig;
import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.repository.IntegrationConfigRepository;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for reading active tenant configuration and integrations with secret redaction.
 */
@Component
public class ReadConfigTool implements SentinelTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final IntegrationConfigRepository integrationConfigRepository;
    private final TenantDatasourceRepository tenantDatasourceRepository;

    @Autowired
    public ReadConfigTool(
            IntegrationConfigRepository integrationConfigRepository,
            TenantDatasourceRepository tenantDatasourceRepository) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.tenantDatasourceRepository = tenantDatasourceRepository;
    }

    @Override
    public String getName() {
        return "read_config";
    }

    @Override
    public String getDescription() {
        return "Reads tenant configurations and integration settings. Secret values are automatically redacted.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> categoryProp = new LinkedHashMap<>();
        categoryProp.put("type", "string");
        categoryProp.put("description", "Optional category filter: 'integrations', 'datasources', or 'all' (default)");
        props.put("category", categoryProp);

        params.put("properties", props);
        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        try {
            String category = arguments != null ? (String) arguments.getOrDefault("category", "all") : "all";
            Map<String, Object> configOutput = new LinkedHashMap<>();

            if ("all".equalsIgnoreCase(category) || "datasources".equalsIgnoreCase(category)) {
                List<TenantDatasource> datasources = tenantDatasourceRepository.findByTenantId(tenantId);
                List<Map<String, Object>> safeDatasources = datasources.stream().map(ds -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", ds.getId());
                    m.put("name", ds.getName());
                    m.put("kind", ds.getKind());
                    m.put("url", ds.getUrl());
                    m.put("authType", ds.getAuthType());
                    m.put("credentialsRef", SecretProtector.REDACTED);
                    m.put("enabled", ds.isEnabled());
                    m.put("isDefault", ds.isDefault());
                    return m;
                }).collect(Collectors.toList());
                configOutput.put("datasources", safeDatasources);
            }

            if ("all".equalsIgnoreCase(category) || "integrations".equalsIgnoreCase(category)) {
                List<IntegrationConfig> integrations = integrationConfigRepository.findByTenantId(tenantId);
                List<Map<String, Object>> safeIntegrations = integrations.stream().map(ic -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", ic.getId());
                    m.put("kind", ic.getKind());
                    m.put("enabled", ic.isEnabled());
                    m.put("config", redactJsonConfig(ic.getConfigJson()));
                    return m;
                }).collect(Collectors.toList());
                configOutput.put("integrations", safeIntegrations);
            }

            String jsonResult = objectMapper.writeValueAsString(configOutput);
            String summary = String.format("Read configuration for tenant %s (category: %s). Secrets redacted.", tenantId, category);
            return ToolExecutionResult.success(configOutput, summary + "\n" + jsonResult);

        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to read configuration: " + e.getMessage());
        }
    }

    private Map<String, Object> redactJsonConfig(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            return redactMap(map);
        } catch (Exception e) {
            return Map.of("raw", SecretProtector.REDACTED);
        }
    }

    private Map<String, Object> redactMap(Map<String, Object> map) {
        Map<String, Object> redacted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            if (isSecretKey(k)) {
                redacted.put(k, SecretProtector.REDACTED);
            } else if (v instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) v;
                redacted.put(k, redactMap(subMap));
            } else if (v instanceof String && SecretProtector.containsSecret((String) v)) {
                redacted.put(k, SecretProtector.redact((String) v));
            } else {
                redacted.put(k, v);
            }
        }
        return redacted;
    }

    private boolean isSecretKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("secret") || lower.contains("token")
                || lower.contains("cred") || lower.contains("key") || lower.contains("auth");
    }
}
