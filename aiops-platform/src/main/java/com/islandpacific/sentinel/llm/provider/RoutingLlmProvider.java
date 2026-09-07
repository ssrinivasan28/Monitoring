package com.islandpacific.sentinel.llm.provider;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Composite LlmProvider that manages provider routing, fallback execution,
 * and graceful degradation when AI capabilities are disabled or unavailable.
 */
public class RoutingLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(RoutingLlmProvider.class);

    private final LlmProperties properties;
    private final Map<String, LlmProvider> providers;

    public RoutingLlmProvider(LlmProperties properties, Map<String, LlmProvider> providers) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.providers = Objects.requireNonNull(providers, "providers map must not be null");
    }

    @Override
    public String getProviderName() {
        return "routing";
    }

    @Override
    public boolean isAvailable() {
        if (properties.getMode() == LlmProperties.Mode.DISABLED) {
            return false;
        }

        if (properties.getMode() == LlmProperties.Mode.CLAUDE) {
            LlmProvider claude = providers.get("claude");
            return claude != null && claude.isAvailable();
        }

        if (properties.getMode() == LlmProperties.Mode.LOCAL) {
            LlmProvider local = providers.get("local");
            return local != null && local.isAvailable();
        }

        // AUTO mode
        LlmProvider primary = providers.get(properties.getPrimary());
        LlmProvider secondary = providers.get(properties.getSecondary());
        return (primary != null && primary.isAvailable()) || (secondary != null && secondary.isAvailable());
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        if (properties.getMode() == LlmProperties.Mode.DISABLED) {
            log.info("LLM routing mode is DISABLED. Returning clean AI unavailable response.");
            return LlmResponse.unavailable("AI service is disabled");
        }

        if (properties.getMode() == LlmProperties.Mode.CLAUDE) {
            return executeProvider("claude", request);
        }

        if (properties.getMode() == LlmProperties.Mode.LOCAL) {
            return executeProvider("local", request);
        }

        // AUTO mode: Try primary, fallback to secondary
        String primaryName = properties.getPrimary() != null ? properties.getPrimary() : "claude";
        String secondaryName = properties.getSecondary() != null ? properties.getSecondary() : "local";

        log.debug("Executing primary LLM provider: {}", primaryName);
        LlmResponse primaryResp = executeProvider(primaryName, request);
        if (primaryResp.getStatus() == LlmResponse.Status.SUCCESS) {
            return primaryResp;
        }

        log.warn("Primary LLM provider [{}] failed or unavailable ({}). Attempting fallback provider [{}]",
                primaryName, primaryResp.getErrorMessage(), secondaryName);

        if (!secondaryName.equalsIgnoreCase(primaryName)) {
            LlmResponse secondaryResp = executeProvider(secondaryName, request);
            if (secondaryResp.getStatus() == LlmResponse.Status.SUCCESS) {
                log.info("Fallback to LLM provider [{}] succeeded", secondaryName);
                return secondaryResp;
            }
            log.error("Secondary LLM provider [{}] also failed: {}", secondaryName, secondaryResp.getErrorMessage());
        }

        return LlmResponse.unavailable("All configured LLM providers are currently unavailable");
    }

    private LlmResponse executeProvider(String providerName, LlmRequest request) {
        LlmProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            log.warn("Requested LLM provider [{}] is not registered", providerName);
            return LlmResponse.unavailable("Provider '" + providerName + "' not registered");
        }
        if (!provider.isAvailable()) {
            return LlmResponse.unavailable("Provider '" + providerName + "' is not available");
        }
        try {
            return provider.chat(request);
        } catch (Exception e) {
            log.error("Unexpected exception invoking provider [{}]: {}", providerName, e.getMessage(), e);
            return LlmResponse.unavailable("Error invoking provider '" + providerName + "': " + e.getMessage());
        }
    }
}
