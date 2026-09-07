package com.islandpacific.sentinel.llm.provider;

import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;

/**
 * Interface for pluggable LLM provider integrations (Claude API, Local Ollama/vLLM, etc).
 */
public interface LlmProvider {

    /**
     * Executes a chat/tool request against the LLM provider.
     * Guarantees returning an LlmResponse (with status UNAVAILABLE if provider fails or is disabled).
     */
    LlmResponse chat(LlmRequest request);

    /**
     * Indicates whether the provider is configured and reachable.
     */
    boolean isAvailable();

    /**
     * Provider identifier string (e.g. "claude", "local", "routing").
     */
    String getProviderName();
}
