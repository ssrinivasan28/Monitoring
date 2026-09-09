package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.repository.ToolCallRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry and dispatcher for read-only Sentinel tools.
 * Enforces tenant scope, secret redaction, and mandatory audit logging to tool_calls table.
 */
@Service
public class ToolRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, SentinelTool> toolsByName;
    private final ToolCallRepository toolCallRepository;

    @Autowired
    public ToolRegistryService(List<SentinelTool> tools, ToolCallRepository toolCallRepository) {
        this.toolsByName = new HashMap<>();
        if (tools != null) {
            for (SentinelTool tool : tools) {
                this.toolsByName.put(tool.getName(), tool);
            }
        }
        this.toolCallRepository = toolCallRepository;
    }

    /**
     * Returns LLM tool definitions for all registered tools.
     */
    public List<LlmTool> getAvailableTools() {
        return toolsByName.values().stream()
                .map(SentinelTool::getLlmToolDefinition)
                .collect(Collectors.toList());
    }

    /**
     * Lookup registered tool by name.
     */
    public Optional<SentinelTool> getTool(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    /**
     * Dispatches tool invocation, enforces tenant scope, and logs to tool_calls table.
     */
    public ToolExecutionResult executeTool(UUID agentRunId, UUID tenantId, String toolName, Map<String, Object> arguments) {
        return executeTool(agentRunId, tenantId, null, toolName, arguments);
    }

    /**
     * Same as above, additionally tagging the audit row with the incident being investigated (1.9),
     * so an investigation's tool trace is queryable per-incident. {@code incidentId} is nullable.
     */
    public ToolExecutionResult executeTool(UUID agentRunId, UUID tenantId, UUID incidentId, String toolName, Map<String, Object> arguments) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required for tool execution");
        }

        UUID resolvedAgentRunId = agentRunId != null ? agentRunId : UUID.nameUUIDFromBytes("standalone-tool-call".getBytes());
        Map<String, Object> safeArgs = arguments != null ? redactArguments(arguments) : Collections.emptyMap();
        String safeArgsJson;
        try {
            safeArgsJson = objectMapper.writeValueAsString(safeArgs);
        } catch (Exception e) {
            safeArgsJson = "{}";
        }

        SentinelTool tool = toolsByName.get(toolName);
        ToolExecutionResult result;

        if (tool == null) {
            result = ToolExecutionResult.failure("Unknown tool: " + toolName);
        } else {
            try {
                result = tool.execute(tenantId, arguments);
            } catch (Exception e) {
                log.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
                result = ToolExecutionResult.failure("Tool execution failed: " + e.getMessage());
            }
        }

        String summary = result.isSuccess() ? result.getResultSummary() : "ERROR: " + result.getErrorMessage();
        if (summary != null && summary.length() > 2000) {
            summary = summary.substring(0, 1997) + "...";
        }

        // Mandatory SOC 2 audit logging to tool_calls
        try {
            ToolCall toolCall = new ToolCall(resolvedAgentRunId, tenantId, toolName, safeArgsJson, summary);
            toolCall.setIncidentId(incidentId);
            toolCallRepository.save(toolCall);
        } catch (Exception e) {
            log.error("Failed to record tool_calls audit log: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Redacts secret patterns and password/token keys from arguments.
     */
    public Map<String, Object> redactArguments(Map<String, Object> args) {
        Map<String, Object> redacted = new HashMap<>();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSecretKey(key)) {
                redacted.put(key, SecretProtector.REDACTED);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                redacted.put(key, redactArguments(subMap));
            } else if (value instanceof String && SecretProtector.containsSecret((String) value)) {
                redacted.put(key, SecretProtector.redact((String) value));
            } else {
                redacted.put(key, value);
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
