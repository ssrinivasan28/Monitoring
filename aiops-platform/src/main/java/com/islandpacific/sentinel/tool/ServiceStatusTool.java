package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.llm.model.LlmTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service status tool reporting system/service health for a tenant.
 */
@Component
public class ServiceStatusTool implements SentinelTool {

    private final ListMonitorsTool listMonitorsTool;

    @Autowired
    public ServiceStatusTool(ListMonitorsTool listMonitorsTool) {
        this.listMonitorsTool = listMonitorsTool;
    }

    @Override
    public String getName() {
        return "service_status";
    }

    @Override
    public String getDescription() {
        return "Returns current service health and monitor status for the tenant.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", Map.of());
        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        return listMonitorsTool.execute(tenantId, arguments);
    }
}
