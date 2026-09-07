package com.islandpacific.sentinel.llm.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.*;
import com.islandpacific.sentinel.security.SecretProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * LlmProvider implementation targeting Anthropic Messages API (Claude).
 * Supports native tool-use and structured conversation history.
 */
public class ClaudeApiProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiProvider.class);

    private final LlmProperties.ClaudeProperties properties;
    private final SecretProtector secretProtector;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ClaudeApiProvider(LlmProperties.ClaudeProperties properties, SecretProtector secretProtector) {
        this(properties, secretProtector, createRestTemplate(properties.getTimeoutMs()));
    }

    public ClaudeApiProvider(LlmProperties.ClaudeProperties properties, SecretProtector secretProtector, RestTemplate restTemplate) {
        this.properties = properties;
        this.secretProtector = secretProtector;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 10000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 10000);
        return new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return "claude";
    }

    @Override
    public boolean isAvailable() {
        String resolvedKey = getResolvedApiKey();
        return resolvedKey != null && !resolvedKey.trim().isEmpty() && properties.getApiUrl() != null && !properties.getApiUrl().trim().isEmpty();
    }

    private String getResolvedApiKey() {
        if (properties.getApiKey() == null) {
            return null;
        }
        return secretProtector != null ? secretProtector.resolve(properties.getApiKey()) : properties.getApiKey();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        if (!isAvailable()) {
            log.warn("ClaudeApiProvider is unavailable or missing API key");
            return LlmResponse.unavailable("Claude API key not configured or provider unavailable");
        }

        try {
            Map<String, Object> payload = buildPayload(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", getResolvedApiKey());
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseClaudeResponse(response.getBody(), request.getModel() != null ? request.getModel() : properties.getModel());
            } else {
                log.error("Claude API returned non-2xx status: {}", response.getStatusCode());
                return LlmResponse.unavailable("Claude API returned status " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with Claude API: {}", e.getMessage(), e);
            return LlmResponse.unavailable("Claude API call failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(LlmRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.getModel() != null ? request.getModel() : properties.getModel());
        payload.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : properties.getMaxTokens());

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            payload.put("system", request.getSystemPrompt());
        }

        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }

        List<Map<String, Object>> formattedMessages = new ArrayList<>();
        for (LlmMessage msg : request.getMessages()) {
            if (msg.getRole() == LlmMessage.Role.SYSTEM) {
                // If system message is in history, set as system field if not already set
                if (!payload.containsKey("system") && msg.getContent() != null) {
                    payload.put("system", msg.getContent());
                }
                continue;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            if (msg.getRole() == LlmMessage.Role.USER) {
                m.put("role", "user");
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
            } else if (msg.getRole() == LlmMessage.Role.ASSISTANT) {
                m.put("role", "assistant");
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    List<Map<String, Object>> contentBlocks = new ArrayList<>();
                    if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                        Map<String, Object> textBlock = new LinkedHashMap<>();
                        textBlock.put("type", "text");
                        textBlock.put("text", msg.getContent());
                        contentBlocks.add(textBlock);
                    }
                    for (LlmToolCall tc : msg.getToolCalls()) {
                        Map<String, Object> toolUseBlock = new LinkedHashMap<>();
                        toolUseBlock.put("type", "tool_use");
                        toolUseBlock.put("id", tc.getId());
                        toolUseBlock.put("name", tc.getName());
                        toolUseBlock.put("input", tc.getArguments() != null ? tc.getArguments() : Collections.emptyMap());
                        contentBlocks.add(toolUseBlock);
                    }
                    m.put("content", contentBlocks);
                } else {
                    m.put("content", msg.getContent() != null ? msg.getContent() : "");
                }
            } else if (msg.getRole() == LlmMessage.Role.TOOL) {
                m.put("role", "user");
                List<Map<String, Object>> contentBlocks = new ArrayList<>();
                Map<String, Object> toolResultBlock = new LinkedHashMap<>();
                toolResultBlock.put("type", "tool_result");
                toolResultBlock.put("tool_use_id", msg.getToolCallId());
                toolResultBlock.put("content", msg.getContent() != null ? msg.getContent() : "");
                contentBlocks.add(toolResultBlock);
                m.put("content", contentBlocks);
            }
            formattedMessages.add(m);
        }
        payload.put("messages", formattedMessages);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> toolsPayload = new ArrayList<>();
            for (LlmTool t : request.getTools()) {
                Map<String, Object> toolMap = new LinkedHashMap<>();
                toolMap.put("name", t.getName());
                toolMap.put("description", t.getDescription() != null ? t.getDescription() : "");
                toolMap.put("input_schema", t.getParameters() != null ? t.getParameters() : Collections.singletonMap("type", "object"));
                toolsPayload.add(toolMap);
            }
            payload.put("tools", toolsPayload);
        }

        return payload;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseClaudeResponse(Map<String, Object> responseBody, String model) {
        List<LlmToolCall> toolCalls = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        Object contentObj = responseBody.get("content");
        if (contentObj instanceof List) {
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
            for (Map<String, Object> block : contentList) {
                String type = (String) block.get("type");
                if ("text".equals(type)) {
                    String text = (String) block.get("text");
                    if (text != null) {
                        textBuilder.append(text);
                    }
                } else if ("tool_use".equals(type)) {
                    String id = (String) block.get("id");
                    String name = (String) block.get("name");
                    Map<String, Object> input = (Map<String, Object>) block.get("input");
                    toolCalls.add(new LlmToolCall(id, name, input != null ? input : Collections.emptyMap()));
                }
            }
        }

        LlmResponse resp = LlmResponse.success(textBuilder.toString(), toolCalls, getProviderName(), model);

        Object usageObj = responseBody.get("usage");
        if (usageObj instanceof Map) {
            Map<String, Object> usageMap = (Map<String, Object>) usageObj;
            if (usageMap.get("input_tokens") instanceof Number) {
                resp.setPromptTokens(((Number) usageMap.get("input_tokens")).intValue());
            }
            if (usageMap.get("output_tokens") instanceof Number) {
                resp.setCompletionTokens(((Number) usageMap.get("output_tokens")).intValue());
            }
        }

        return resp;
    }
}
