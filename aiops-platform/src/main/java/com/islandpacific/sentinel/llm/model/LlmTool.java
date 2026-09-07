package com.islandpacific.sentinel.llm.model;

import java.util.Map;
import java.util.Objects;

/**
 * Defines a tool/function that can be called by the LLM.
 */
public class LlmTool {

    private String name;
    private String description;
    private Map<String, Object> parameters;

    public LlmTool() {
    }

    public LlmTool(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmTool llmTool = (LlmTool) o;
        return Objects.equals(name, llmTool.name) &&
                Objects.equals(description, llmTool.description) &&
                Objects.equals(parameters, llmTool.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, parameters);
    }
}
