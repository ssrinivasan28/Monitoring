package com.islandpacific.sentinel.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Standardized response returned from an LlmProvider.
 * Designed for graceful degradation: when AI is offline or disabled, aiAvailable is false
 * and status is UNAVAILABLE, guaranteeing zero exceptions bubble to callers or UI.
 */
public class LlmResponse {

    public enum Status {
        SUCCESS,
        UNAVAILABLE,
        ERROR
    }

    private String text;
    private List<LlmToolCall> toolCalls = new ArrayList<>();
    private Integer promptTokens;
    private Integer completionTokens;
    private String providerName;
    private String modelName;
    private Status status = Status.SUCCESS;
    private boolean aiAvailable = true;
    private String errorMessage;

    public LlmResponse() {
    }

    public static LlmResponse success(String text, List<LlmToolCall> toolCalls, String providerName, String modelName) {
        LlmResponse resp = new LlmResponse();
        resp.setText(text);
        resp.setToolCalls(toolCalls);
        resp.setProviderName(providerName);
        resp.setModelName(modelName);
        resp.setStatus(Status.SUCCESS);
        resp.setAiAvailable(true);
        return resp;
    }

    public static LlmResponse unavailable(String message) {
        LlmResponse resp = new LlmResponse();
        resp.setStatus(Status.UNAVAILABLE);
        resp.setAiAvailable(false);
        resp.setErrorMessage(message != null ? message : "AI service unavailable");
        resp.setText("AI service unavailable");
        return resp;
    }

    public static LlmResponse error(String message, String providerName) {
        LlmResponse resp = new LlmResponse();
        resp.setStatus(Status.ERROR);
        resp.setAiAvailable(false);
        resp.setProviderName(providerName);
        resp.setErrorMessage(message);
        resp.setText("AI service unavailable: " + message);
        return resp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<LlmToolCall> toolCalls) {
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isAiAvailable() {
        return aiAvailable;
    }

    public void setAiAvailable(boolean aiAvailable) {
        this.aiAvailable = aiAvailable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmResponse response = (LlmResponse) o;
        return aiAvailable == response.aiAvailable &&
                Objects.equals(text, response.text) &&
                Objects.equals(toolCalls, response.toolCalls) &&
                Objects.equals(promptTokens, response.promptTokens) &&
                Objects.equals(completionTokens, response.completionTokens) &&
                Objects.equals(providerName, response.providerName) &&
                Objects.equals(modelName, response.modelName) &&
                status == response.status &&
                Objects.equals(errorMessage, response.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, toolCalls, promptTokens, completionTokens, providerName, modelName, status, aiAvailable, errorMessage);
    }
}
