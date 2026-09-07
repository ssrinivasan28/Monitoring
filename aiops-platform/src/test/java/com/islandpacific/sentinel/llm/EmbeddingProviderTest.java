package com.islandpacific.sentinel.llm;

import com.islandpacific.sentinel.llm.config.LlmProperties;
import com.islandpacific.sentinel.llm.model.EmbeddingRequest;
import com.islandpacific.sentinel.llm.model.EmbeddingResponse;
import com.islandpacific.sentinel.llm.provider.OpenAiCompatibleEmbeddingProvider;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingProviderTest {

    private LlmProperties.EmbeddingProperties properties;
    private SecretProtector secretProtector;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OpenAiCompatibleEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties.EmbeddingProperties();
        properties.setProvider("local");
        properties.setApiUrl("http://localhost:11434/v1/embeddings");
        properties.setModel("nomic-embed-text");
        properties.setDimensions(3);

        secretProtector = new SecretProtector.DefaultSecretProtector();
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        provider = new OpenAiCompatibleEmbeddingProvider(properties, secretProtector, restTemplate);
    }

    @Test
    void testIsAvailable() {
        assertTrue(provider.isAvailable());
        properties.setProvider("disabled");
        assertFalse(provider.isAvailable());
    }

    @Test
    void testEmbedSuccess() {
        String jsonResponse = """
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "embedding",
                      "index": 0,
                      "embedding": [0.123, -0.456, 0.789]
                    }
                  ],
                  "model": "nomic-embed-text"
                }
                """;

        mockServer.expect(requestTo("http://localhost:11434/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        EmbeddingRequest request = new EmbeddingRequest(List.of("Sample metric text"));
        EmbeddingResponse response = provider.embed(request);

        assertNotNull(response);
        assertTrue(response.isAiAvailable());
        assertEquals(1, response.getEmbeddings().size());
        assertEquals(3, response.getDimensions());
        assertEquals(0.123f, response.getEmbeddings().get(0)[0], 0.001f);
        mockServer.verify();
    }

    @Test
    void testEmbedDisabledDegradesGracefully() {
        properties.setProvider("disabled");

        EmbeddingRequest request = new EmbeddingRequest(List.of("Sample metric text"));
        EmbeddingResponse response = assertDoesNotThrow(() -> provider.embed(request));

        assertNotNull(response);
        assertFalse(response.isAiAvailable());
        assertNotNull(response.getErrorMessage());
    }
}
