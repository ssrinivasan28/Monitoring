package com.islandpacific.sentinel.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single message in an LLM conversation history.
 */
public class LlmMessage {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    private Role role;
    private String content;
    private String name;
    private String toolCallId;
    private List<LlmToolCall> toolCalls = new ArrayList<>();

    public LlmMessage() {
    }

    public LlmMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content);
    }

    public static LlmMessage assistantWithTools(String content, List<LlmToolCall> toolCalls) {
        LlmMessage msg = new LlmMessage(Role.ASSISTANT, content);
        if (toolCalls != null) {
            msg.setToolCalls(toolCalls);
        }
        return msg;
    }

    public static LlmMessage toolResult(String toolCallId, String toolName, String content) {
        LlmMessage msg = new LlmMessage(Role.TOOL, content);
        msg.setToolCallId(toolCallId);
        msg.setName(toolName);
        return msg;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<LlmToolCall> toolCalls) {
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmMessage that = (LlmMessage) o;
        return role == that.role &&
                Objects.equals(content, that.content) &&
                Objects.equals(name, that.name) &&
                Objects.equals(toolCallId, that.toolCallId) &&
                Objects.equals(toolCalls, that.toolCalls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, name, toolCallId, toolCalls);
    }
}
