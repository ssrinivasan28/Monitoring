package com.islandpacific.sentinel.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.query.QueryGatewayService;
import com.islandpacific.sentinel.query.ResponseMerger;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * PromQL tool for querying Prometheus metrics via QueryGatewayService.
 */
@Component
public class PromQlQueryTool implements SentinelTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final QueryGatewayService queryGatewayService;

    @Autowired
    public PromQlQueryTool(QueryGatewayService queryGatewayService) {
        this.queryGatewayService = queryGatewayService;
    }

    @Override
    public String getName() {
        return "promql_query";
    }

    @Override
    public String getDescription() {
        return "Executes PromQL instant or range metrics queries against tenant metrics endpoints.";
    }

    @Override
    public LlmTool getLlmToolDefinition() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");

        Map<String, Object> props = new LinkedHashMap<>();

        Map<String, Object> queryProp = new LinkedHashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "PromQL query string, e.g., 'up' or 'cpu_usage_percent'");
        props.put("query", queryProp);

        Map<String, Object> startProp = new LinkedHashMap<>();
        startProp.put("type", "number");
        startProp.put("description", "Start time unix timestamp for range query (optional)");
        props.put("start", startProp);

        Map<String, Object> endProp = new LinkedHashMap<>();
        endProp.put("type", "number");
        endProp.put("description", "End time unix timestamp for range query (optional)");
        props.put("end", endProp);

        Map<String, Object> stepProp = new LinkedHashMap<>();
        stepProp.put("type", "string");
        stepProp.put("description", "Query step duration for range query, e.g. '15s', '1m' (optional)");
        props.put("step", stepProp);

        Map<String, Object> timeProp = new LinkedHashMap<>();
        timeProp.put("type", "string");
        timeProp.put("description", "Evaluation timestamp for instant query (optional)");
        props.put("time", timeProp);

        params.put("properties", props);
        params.put("required", List.of("query"));

        return new LlmTool(getName(), getDescription(), params);
    }

    @Override
    public ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments) {
        if (arguments == null || !arguments.containsKey("query")) {
            return ToolExecutionResult.failure("Missing required 'query' argument");
        }

        String query = (String) arguments.get("query");
        String correlationId = "tool-promql-" + UUID.randomUUID();

        Optional<TenantContext> existingContext = TenantContextHolder.getContext();
        boolean contextSet = false;
        if (existingContext.isEmpty() || !existingContext.get().getTenantId().equals(tenantId)) {
            TenantContext ctx = new TenantContext(
                    UUID.nameUUIDFromBytes("agent".getBytes()),
                    tenantId,
                    "agent-tool",
                    "SYSTEM"
            );
            TenantContextHolder.setContext(ctx);
            contextSet = true;
        }

        try {
            ResponseMerger.MergedResult result;
            if (arguments.containsKey("start") && arguments.containsKey("end")) {
                double start = parseDouble(arguments.get("start"));
                double end = parseDouble(arguments.get("end"));
                String step = (String) arguments.getOrDefault("step", "1m");
                result = queryGatewayService.executePromQlRange(query, start, end, step, correlationId);
            } else {
                String timeStr = (String) arguments.get("time");
                result = queryGatewayService.executePromQlInstant(query, timeStr, correlationId);
            }

            String jsonResult = result.getJsonResponse();
            String summary = String.format("PromQL query '%s' executed. Status code: %d.", query, result.getHttpStatusCode());
            return ToolExecutionResult.success(jsonResult, summary + "\n" + jsonResult);

        } catch (Exception e) {
            return ToolExecutionResult.failure("PromQL execution failed: " + e.getMessage());
        } finally {
            if (contextSet) {
                TenantContextHolder.clear();
            }
        }
    }

    private double parseDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return Double.parseDouble(val.toString());
    }
}
