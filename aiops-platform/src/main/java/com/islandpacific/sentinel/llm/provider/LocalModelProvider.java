package com.islandpacific.sentinel.llm.provider;

import com.fasterxml.jackson.core.type.TypeReference;
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
 * LlmProvider implementation targeting OpenAI-compatible local endpoints (Ollama / vLLM).
 * Supports OpenAI function/tool-calling schemas and conversation flow.
 */
public class LocalModelProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalModelProvider.class);

    private final LlmProperties.LocalProperties properties;
    private final SecretProtector secretProtector;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LocalModelProvider(LlmProperties.LocalProperties properties, SecretProtector secretProtector) {
        this(properties, secretProtector, createRestTemplate(properties.getTimeoutMs()));
    }

    public LocalModelProvider(LlmProperties.LocalProperties properties, SecretProtector secretProtector, RestTemplate restTemplate) {
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
        return "local";
    }

    @Override
    public boolean isAvailable() {
        return properties.getApiUrl() != null && !properties.getApiUrl().trim().isEmpty();
    }

    private String getResolvedApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
            return null;
        }
        return secretProtector != null ? secretProtector.resolve(properties.getApiKey()) : properties.getApiKey();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        if (!isAvailable()) {
            log.warn("LocalModelProvider is unavailable - API URL not configured");
            return LlmResponse.unavailable("Local LLM API URL not configured");
        }

        try {
            Map<String, Object> payload = buildPayload(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String key = getResolvedApiKey();
            if (key != null && !key.isEmpty()) {
                headers.setBearerAuth(key);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseOpenAiResponse(response.getBody(), request.getModel() != null ? request.getModel() : properties.getModel());
            } else {
                log.error("Local LLM API returned non-2xx status: {}", response.getStatusCode());
                return LlmResponse.unavailable("Local LLM API returned status " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with Local LLM API: {}", e.getMessage(), e);
            return LlmResponse.unavailable("Local LLM API call failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(LlmRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.getModel() != null ? request.getModel() : properties.getModel());

        if (request.getMaxTokens() != null) {
            payload.put("max_tokens", request.getMaxTokens());
        } else if (properties.getMaxTokens() > 0) {
            payload.put("max_tokens", properties.getMaxTokens());
        }

        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }

        List<Map<String, Object>> formattedMessages = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            Map<String, Object> sysMsg = new LinkedHashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", request.getSystemPrompt());
            formattedMessages.add(sysMsg);
        }

        for (LlmMessage msg : request.getMessages()) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (msg.getRole() == LlmMessage.Role.SYSTEM) {
                m.put("role", "system");
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
            } else if (msg.getRole() == LlmMessage.Role.USER) {
                m.put("role", "user");
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
            } else if (msg.getRole() == LlmMessage.Role.ASSISTANT) {
                m.put("role", "assistant");
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    List<Map<String, Object>> toolCallsPayload = new ArrayList<>();
                    for (LlmToolCall tc : msg.getToolCalls()) {
                        Map<String, Object> tcMap = new LinkedHashMap<>();
                        tcMap.put("id", tc.getId());
                        tcMap.put("type", "function");
                        Map<String, Object> fn = new LinkedHashMap<>();
                        fn.put("name", tc.getName());
                        try {
                            fn.put("arguments", objectMapper.writeValueAsString(tc.getArguments() != null ? tc.getArguments() : Collections.emptyMap()));
                        } catch (Exception e) {
                            fn.put("arguments", "{}");
                        }
                        tcMap.put("function", fn);
                        toolCallsPayload.add(tcMap);
                    }
                    m.put("tool_calls", toolCallsPayload);
                }
            } else if (msg.getRole() == LlmMessage.Role.TOOL) {
                m.put("role", "tool");
                m.put("tool_call_id", msg.getToolCallId());
                if (msg.getName() != null) {
                    m.put("name", msg.getName());
                }
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
            }
            formattedMessages.add(m);
        }
        payload.put("messages", formattedMessages);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            List<Map<String, Object>> toolsPayload = new ArrayList<>();
            for (LlmTool t : request.getTools()) {
                Map<String, Object> toolMap = new LinkedHashMap<>();
                toolMap.put("type", "function");
                Map<String, Object> fn = new LinkedHashMap<>();
                fn.put("name", t.getName());
                fn.put("description", t.getDescription() != null ? t.getDescription() : "");
                fn.put("parameters", t.getParameters() != null ? t.getParameters() : Collections.singletonMap("type", "object"));
                toolMap.put("function", fn);
                toolsPayload.add(toolMap);
            }
            payload.put("tools", toolsPayload);
        }

        return payload;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseOpenAiResponse(Map<String, Object> responseBody, String model) {
        List<LlmToolCall> toolCalls = new ArrayList<>();
        String text = null;

        Object choicesObj = responseBody.get("choices");
        if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
            Map<String, Object> choice0 = (Map<String, Object>) ((List<?>) choicesObj).get(0);
            Map<String, Object> message = (Map<String, Object>) choice0.get("message");
            if (message != null) {
                text = (String) message.get("content");

                Object tcObj = message.get("tool_calls");
                if (tcObj instanceof List) {
                    List<Map<String, Object>> tcList = (List<Map<String, Object>>) tcObj;
                    for (Map<String, Object> tc : tcList) {
                        String id = (String) tc.get("id");
                        Map<String, Object> function = (Map<String, Object>) tc.get("function");
                        if (function != null) {
                            String name = (String) function.get("name");
                            Object rawArgs = function.get("arguments");
                            Map<String, Object> argsMap = parseArguments(rawArgs);
                            toolCalls.add(new LlmToolCall(id != null ? id : UUID.randomUUID().toString(), name, argsMap));
                        }
                    }
                }
            }
        }

        LlmResponse resp = LlmResponse.success(text != null ? text : "", toolCalls, getProviderName(), model);

        Object usageObj = responseBody.get("usage");
        if (usageObj instanceof Map) {
            Map<String, Object> usageMap = (Map<String, Object>) usageObj;
            if (usageMap.get("prompt_tokens") instanceof Number) {
                resp.setPromptTokens(((Number) usageMap.get("prompt_tokens")).intValue());
            }
            if (usageMap.get("completion_tokens") instanceof Number) {
                resp.setCompletionTokens(((Number) usageMap.get("completion_tokens")).intValue());
            }
        }

        return resp;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(Object rawArgs) {
        if (rawArgs == null) {
            return Collections.emptyMap();
        }
        if (rawArgs instanceof Map) {
            return (Map<String, Object>) rawArgs;
        }
        if (rawArgs instanceof String) {
            String str = (String) rawArgs;
            if (str.trim().isEmpty()) {
                return Collections.emptyMap();
            }
            try {
                return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse JSON string tool call arguments: {}", str);
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }
}
