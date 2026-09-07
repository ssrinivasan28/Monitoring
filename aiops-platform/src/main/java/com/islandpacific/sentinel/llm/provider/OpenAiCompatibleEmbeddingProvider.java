package com.islandpacific.sentinel.llm.provider;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.EmbeddingRequest;
import com.islandpacific.sentinel.llm.model.EmbeddingResponse;
import com.islandpacific.sentinel.security.SecretProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * EmbeddingProvider implementation targeting OpenAI-compatible vector embedding endpoints.
 */
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingProvider.class);

    private final LlmProperties.EmbeddingProperties properties;
    private final SecretProtector secretProtector;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleEmbeddingProvider(LlmProperties.EmbeddingProperties properties, SecretProtector secretProtector) {
        this(properties, secretProtector, createRestTemplate(properties.getTimeoutMs()));
    }

    public OpenAiCompatibleEmbeddingProvider(LlmProperties.EmbeddingProperties properties, SecretProtector secretProtector, RestTemplate restTemplate) {
        this.properties = properties;
        this.secretProtector = secretProtector;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs > 0 ? timeoutMs : 5000);
        factory.setReadTimeout(timeoutMs > 0 ? timeoutMs : 5000);
        return new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return properties.getProvider() != null ? properties.getProvider() : "local-embedding";
    }

    @Override
    public boolean isAvailable() {
        if ("disabled".equalsIgnoreCase(properties.getProvider())) {
            return false;
        }
        return properties.getApiUrl() != null && !properties.getApiUrl().trim().isEmpty();
    }

    private String getResolvedApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
            return null;
        }
        return secretProtector != null ? secretProtector.resolve(properties.getApiKey()) : properties.getApiKey();
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        if (!isAvailable()) {
            log.warn("Embedding provider is unavailable or disabled");
            return EmbeddingResponse.unavailable("Embedding service is disabled or unavailable");
        }

        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            return EmbeddingResponse.success(Collections.emptyList(), properties.getDimensions(), getProviderName(), properties.getModel());
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", request.getModel() != null ? request.getModel() : properties.getModel());
            payload.put("input", request.getInputs().size() == 1 ? request.getInputs().get(0) : request.getInputs());

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
                return parseEmbeddingResponse(response.getBody(), request.getModel() != null ? request.getModel() : properties.getModel());
            } else {
                log.error("Embedding API returned non-2xx status: {}", response.getStatusCode());
                return EmbeddingResponse.unavailable("Embedding API returned status " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error communicating with Embedding API: {}", e.getMessage(), e);
            return EmbeddingResponse.unavailable("Embedding API call failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private EmbeddingResponse parseEmbeddingResponse(Map<String, Object> responseBody, String model) {
        List<float[]> embeddings = new ArrayList<>();
        int dimensions = properties.getDimensions();

        Object dataObj = responseBody.get("data");
        if (dataObj instanceof List) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
            for (Map<String, Object> item : dataList) {
                Object embObj = item.get("embedding");
                if (embObj instanceof List) {
                    List<Number> numList = (List<Number>) embObj;
                    float[] floatArray = new float[numList.size()];
                    for (int i = 0; i < numList.size(); i++) {
                        floatArray[i] = numList.get(i).floatValue();
                    }
                    embeddings.add(floatArray);
                    dimensions = floatArray.length;
                }
            }
        }

        return EmbeddingResponse.success(embeddings, dimensions, getProviderName(), model);
    }
}
