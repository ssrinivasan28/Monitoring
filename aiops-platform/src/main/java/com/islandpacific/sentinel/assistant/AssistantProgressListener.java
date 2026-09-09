package com.islandpacific.sentinel.assistant;

/**
 * 2.1 — incremental progress hook for the assistant agent loop, so a streaming transport (SPA/Teams,
 * 2.2/2.3) can render each tool call as it happens instead of waiting for the whole answer.
 */
public interface AssistantProgressListener {

    AssistantProgressListener NOOP = new AssistantProgressListener() {
    };

    default void onToolCall(String tool, String query) {
    }

    default void onToolResult(String tool, String summary) {
    }

    default void onAnswer(AssistantAnswer answer) {
    }
}
