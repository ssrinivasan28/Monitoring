package com.islandpacific.sentinel.llm.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a tool call invocation issued by the model.
 */
public class LlmToolCall {

    private String id;
    private String name;
    private Map<String, Object> arguments;

    public LlmToolCall() {
    }

    public LlmToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmToolCall that = (LlmToolCall) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, arguments);
    }
}
