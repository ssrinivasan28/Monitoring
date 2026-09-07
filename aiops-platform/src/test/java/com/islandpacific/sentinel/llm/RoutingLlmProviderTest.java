package com.islandpacific.sentinel.llm;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.*;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.llm.provider.RoutingLlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoutingLlmProviderTest {

    private LlmProperties properties;
    private DummyLlmProvider primaryProvider;
    private DummyLlmProvider secondaryProvider;
    private RoutingLlmProvider routingProvider;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        properties.setMode(LlmProperties.Mode.AUTO);
        properties.setPrimary("claude");
        properties.setSecondary("local");

        primaryProvider = new DummyLlmProvider("claude", true);
        secondaryProvider = new DummyLlmProvider("local", true);

        Map<String, LlmProvider> providerMap = new HashMap<>();
        providerMap.put("claude", primaryProvider);
        providerMap.put("local", secondaryProvider);

        routingProvider = new RoutingLlmProvider(properties, providerMap);
    }

    @Test
    void testAutoModeSuccessPrimary() {
        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = routingProvider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertEquals("claude", response.getProviderName());
        assertEquals("Primary success", response.getText());
    }

    @Test
    void testAutoModeFallbackToSecondaryWhenPrimaryFails() {
        primaryProvider.setAvailable(false);

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = routingProvider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertEquals("local", response.getProviderName());
        assertEquals("Secondary success", response.getText());
    }

    @Test
    void testDisabledMode() {
        properties.setMode(LlmProperties.Mode.DISABLED);

        assertFalse(routingProvider.isAvailable());

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = routingProvider.chat(request);

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertEquals(LlmResponse.Status.UNAVAILABLE, response.getStatus());
        assertEquals("AI service is disabled", response.getErrorMessage());
    }

    @Test
    void testAllProvidersFailingReturnsCleanUnavailableResponse() {
        primaryProvider.setAvailable(false);
        secondaryProvider.setAvailable(false);

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = routingProvider.chat(request);

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertEquals(LlmResponse.Status.UNAVAILABLE, response.getStatus());
        assertNotNull(response.getErrorMessage());
    }

    private static class DummyLlmProvider implements LlmProvider {
        private final String name;
        private boolean available;

        public DummyLlmProvider(String name, boolean available) {
            this.name = name;
            this.available = available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        @Override
        public LlmResponse chat(LlmRequest request) {
            if (!available) {
                return LlmResponse.unavailable(name + " provider unavailable");
            }
            return LlmResponse.success(
                    "claude".equals(name) ? "Primary success" : "Secondary success",
                    Collections.emptyList(),
                    name,
                    "test-model"
            );
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String getProviderName() {
            return name;
        }
    }
}
