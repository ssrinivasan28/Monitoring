package com.islandpacific.sentinel.tool;

import com.islandpacific.sentinel.llm.model.LlmTool;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for read-only tools executable by IP Sentinel LLM providers and agents.
 */
public interface SentinelTool {

    /**
     * Gets the unique tool name (e.g. "promql_query", "ibmi_sql").
     */
    String getName();

    /**
     * Gets a human-readable description of what the tool does.
     */
    String getDescription();

    /**
     * Returns the LlmTool definition containing parameter schema for LLM tool selection.
     */
    LlmTool getLlmToolDefinition();

    /**
     * Executes the tool with tenant isolation.
     *
     * @param tenantId Target tenant ID
     * @param arguments Input arguments passed by the model or caller
     * @return Execution result wrapper
     */
    ToolExecutionResult execute(UUID tenantId, Map<String, Object> arguments);
}
