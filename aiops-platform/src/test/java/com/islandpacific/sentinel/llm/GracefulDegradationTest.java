package com.islandpacific.sentinel.llm;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.*;
import com.islandpacific.sentinel.llm.provider.ClaudeApiProvider;
import com.islandpacific.sentinel.llm.provider.LocalModelProvider;
import com.islandpacific.sentinel.llm.provider.RoutingLlmProvider;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class GracefulDegradationTest {

    @Test
    void testClaude500ServerErrorReturnsCleanUnavailableResponse() {
        LlmProperties.ClaudeProperties claudeProps = new LlmProperties.ClaudeProperties();
        claudeProps.setApiKey("test-key");
        claudeProps.setApiUrl("https://api.anthropic.com/v1/messages");

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        ClaudeApiProvider provider = new ClaudeApiProvider(claudeProps, new SecretProtector.DefaultSecretProtector(), restTemplate);

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = assertDoesNotThrow(() -> provider.chat(request));

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertEquals(LlmResponse.Status.UNAVAILABLE, response.getStatus());
        assertTrue(response.getErrorMessage().contains("Claude API call failed") || response.getErrorMessage().contains("500"));
        mockServer.verify();
    }

    @Test
    void testLocalModelConnectionRefusedReturnsCleanUnavailableResponse() {
        LlmProperties.LocalProperties localProps = new LlmProperties.LocalProperties();
        localProps.setApiUrl("http://invalid-unreachable-host-123456789.local:11434/v1/chat/completions");

        LocalModelProvider provider = new LocalModelProvider(localProps, new SecretProtector.DefaultSecretProtector());

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = assertDoesNotThrow(() -> provider.chat(request));

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertEquals(LlmResponse.Status.UNAVAILABLE, response.getStatus());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testRoutingProviderWithAllProvidersDownDegradesGracefully() {
        LlmProperties props = new LlmProperties();
        props.setMode(LlmProperties.Mode.AUTO);

        LlmProperties.ClaudeProperties claudeProps = new LlmProperties.ClaudeProperties();
        claudeProps.setApiKey("key");
        claudeProps.setApiUrl("http://invalid-claude-host/messages");

        LlmProperties.LocalProperties localProps = new LlmProperties.LocalProperties();
        localProps.setApiUrl("http://invalid-local-host/completions");

        ClaudeApiProvider claude = new ClaudeApiProvider(claudeProps, new SecretProtector.DefaultSecretProtector());
        LocalModelProvider local = new LocalModelProvider(localProps, new SecretProtector.DefaultSecretProtector());

        Map<String, com.islandpacific.sentinel.llm.provider.LlmProvider> map = new HashMap<>();
        map.put("claude", claude);
        map.put("local", local);

        RoutingLlmProvider routing = new RoutingLlmProvider(props, map);

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Hello")));
        LlmResponse response = assertDoesNotThrow(() -> routing.chat(request));

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertEquals(LlmResponse.Status.UNAVAILABLE, response.getStatus());
    }
}
