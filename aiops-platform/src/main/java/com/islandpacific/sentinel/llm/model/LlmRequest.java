package com.islandpacific.sentinel.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Standardized request sent to an LlmProvider.
 */
public class LlmRequest {

    private String systemPrompt;
    private List<LlmMessage> messages = new ArrayList<>();
    private List<LlmTool> tools = new ArrayList<>();
    private String model;
    private Integer maxTokens;
    private Double temperature;

    public LlmRequest() {
    }

    public LlmRequest(List<LlmMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<LlmMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<LlmMessage> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public List<LlmTool> getTools() {
        return tools;
    }

    public void setTools(List<LlmTool> tools) {
        this.tools = tools != null ? tools : new ArrayList<>();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmRequest request = (LlmRequest) o;
        return Objects.equals(systemPrompt, request.systemPrompt) &&
                Objects.equals(messages, request.messages) &&
                Objects.equals(tools, request.tools) &&
                Objects.equals(model, request.model) &&
                Objects.equals(maxTokens, request.maxTokens) &&
                Objects.equals(temperature, request.temperature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemPrompt, messages, tools, model, maxTokens, temperature);
    }
}
