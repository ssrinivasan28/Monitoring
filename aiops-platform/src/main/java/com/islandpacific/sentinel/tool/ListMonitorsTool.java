package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.repository.MonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool for listing active/configured monitors and their service statuses for a tenant.
 */
@Component
public class ListMonitorsTool implements SentinelTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final MonitorRepository monitorRepository;

    @Autowired
    public ListMonitorsTool(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    @Override
    public String getName() {
        return "list_monitors";
    }

    @Override
    public String getDescription() {
        return "Lists monitored services and monitors for the current tenant including kind, port, and status.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> kindProp = new LinkedHashMap<>();
        kindProp.put("type", "string");
        kindProp.put("description", "Optional monitor kind filter (e.g. 'ibmi', 'windows')");
        props.put("kind", kindProp);

        params.put("properties", props);
        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        try {
            List<Monitor> monitors = monitorRepository.findByTenantId(tenantId);
            String kindFilter = arguments != null ? (String) arguments.get("kind") : null;

            if (kindFilter != null && !kindFilter.isBlank()) {
                monitors = monitors.stream()
                        .filter(m -> kindFilter.equalsIgnoreCase(m.getKind()))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> resultList = monitors.stream().map(m -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", m.getId());
                dto.put("name", m.getName());
                dto.put("kind", m.getKind());
                dto.put("port", m.getPort());
                dto.put("lastSeenAt", m.getLastSeenAt() != null ? m.getLastSeenAt().toString() : "never");
                return dto;
            }).collect(Collectors.toList());

            String jsonResult = objectMapper.writeValueAsString(resultList);
            String summary = String.format("Found %d monitor(s) for tenant %s.", resultList.size(), tenantId);
            return ToolExecutionResult.success(resultList, summary + "\n" + jsonResult);

        } catch (Exception e) {
            return ToolExecutionResult.failure("Failed to list monitors: " + e.getMessage());
        }
    }
}
