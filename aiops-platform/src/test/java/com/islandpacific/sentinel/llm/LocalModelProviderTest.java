package com.islandpacific.sentinel.llm;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.*;
import com.islandpacific.sentinel.llm.provider.LocalModelProvider;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LocalModelProviderTest {

    private LlmProperties.LocalProperties properties;
    private SecretProtector secretProtector;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private LocalModelProvider provider;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties.LocalProperties();
        properties.setApiUrl("http://localhost:11434/v1/chat/completions");
        properties.setModel("llama3.1");

        secretProtector = new SecretProtector.DefaultSecretProtector();
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        provider = new LocalModelProvider(properties, secretProtector, restTemplate);
    }

    @Test
    void testIsAvailable() {
        assertTrue(provider.isAvailable());
        properties.setApiUrl("");
        assertFalse(provider.isAvailable());
    }

    @Test
    void testChatTextResponse() {
        String jsonResponse = """
                {
                  "id": "chatcmpl-123",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "llama3.1",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "Local model analysis complete."
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 5
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Analyze log metrics")));
        LlmResponse response = provider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertEquals(LlmResponse.Status.SUCCESS, response.getStatus());
        assertEquals("Local model analysis complete.", response.getText());
        assertEquals(10, response.getPromptTokens());
        assertEquals(5, response.getCompletionTokens());
        mockServer.verify();
    }

    @Test
    void testChatToolCallResponse() {
        String jsonResponse = """
                {
                  "id": "chatcmpl-456",
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "tool_calls": [{
                        "id": "call_abc123",
                        "type": "function",
                        "function": {
                          "name": "query_prometheus",
                          "arguments": "{\\"query\\": \\"node_cpu_seconds_total\\"}"
                        }
                      }]
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LlmTool tool = new LlmTool("query_prometheus", "Run PromQL", Map.of("type", "object"));
        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Get CPU metric")));
        request.setTools(List.of(tool));

        LlmResponse response = provider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertTrue(response.hasToolCalls());
        assertEquals(1, response.getToolCalls().size());

        LlmToolCall toolCall = response.getToolCalls().get(0);
        assertEquals("call_abc123", toolCall.getId());
        assertEquals("query_prometheus", toolCall.getName());
        assertEquals("node_cpu_seconds_total", toolCall.getArguments().get("query"));
        mockServer.verify();
    }
}
