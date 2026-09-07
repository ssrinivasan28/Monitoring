package com.islandpacific.sentinel.llm;


import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.*;
import com.islandpacific.sentinel.llm.provider.ClaudeApiProvider;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClaudeApiProviderTest {

    private LlmProperties.ClaudeProperties properties;
    private SecretProtector secretProtector;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ClaudeApiProvider provider;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties.ClaudeProperties();
        properties.setApiKey("test-key-123");
        properties.setApiUrl("https://api.anthropic.com/v1/messages");
        properties.setModel("claude-3-5-sonnet-20241022");

        secretProtector = new SecretProtector.DefaultSecretProtector();
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        provider = new ClaudeApiProvider(properties, secretProtector, restTemplate);
    }

    @Test
    void testIsAvailable() {
        assertTrue(provider.isAvailable());
        properties.setApiKey("");
        assertFalse(provider.isAvailable());
    }

    @Test
    void testChatTextResponse() {
        String jsonResponse = """
                {
                  "id": "msg_123",
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "text",
                      "text": "Hello, system is operating normally."
                    }
                  ],
                  "usage": {
                    "input_tokens": 15,
                    "output_tokens": 8
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key-123"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("Check status")));
        LlmResponse response = provider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertEquals(LlmResponse.Status.SUCCESS, response.getStatus());
        assertEquals("Hello, system is operating normally.", response.getText());
        assertEquals(15, response.getPromptTokens());
        assertEquals(8, response.getCompletionTokens());
        mockServer.verify();
    }

    @Test
    void testChatNativeToolUseResponse() {
        String jsonResponse = """
                {
                  "id": "msg_tool_1",
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "text",
                      "text": "Checking job queue status..."
                    },
                    {
                      "type": "tool_use",
                      "id": "toolu_01A",
                      "name": "get_job_queue_depth",
                      "input": {
                        "job_queue": "QBATCH"
                      }
                    }
                  ],
                  "usage": {
                    "input_tokens": 25,
                    "output_tokens": 30
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        LlmTool tool = new LlmTool("get_job_queue_depth", "Get job queue depth", Map.of("type", "object"));
        LlmRequest request = new LlmRequest(List.of(LlmMessage.user("What is QBATCH depth?")));
        request.setTools(List.of(tool));

        LlmResponse response = provider.chat(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertTrue(response.hasToolCalls());
        assertEquals(1, response.getToolCalls().size());

        LlmToolCall toolCall = response.getToolCalls().get(0);
        assertEquals("toolu_01A", toolCall.getId());
        assertEquals("get_job_queue_depth", toolCall.getName());
        assertEquals("QBATCH", toolCall.getArguments().get("job_queue"));
        mockServer.verify();
    }
}
