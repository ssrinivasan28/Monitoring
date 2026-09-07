package com.islandpacific.sentinel.llm.config;

import com.islandpacific.sentinel.llm.provider.*;
import com.islandpacific.sentinel.security.SecretProtector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration wiring LLM providers, routing layer, and embedding provider.
 */
@Configuration
public class LlmAutoConfiguration {

    @Bean(name = "claudeLlmProvider")
    public ClaudeApiProvider claudeLlmProvider(LlmProperties properties, SecretProtector secretProtector) {
        return new ClaudeApiProvider(properties.getClaude(), secretProtector);
    }

    @Bean(name = "localLlmProvider")
    public LocalModelProvider localLlmProvider(LlmProperties properties, SecretProtector secretProtector) {
        return new LocalModelProvider(properties.getLocal(), secretProtector);
    }

    @Bean
    @Primary
    public LlmProvider llmProvider(LlmProperties properties,
                                   ClaudeApiProvider claudeProvider,
                                   LocalModelProvider localProvider) {
        Map<String, LlmProvider> providerMap = new HashMap<>();
        providerMap.put("claude", claudeProvider);
        providerMap.put("local", localProvider);
        return new RoutingLlmProvider(properties, providerMap);
    }

    @Bean
    public EmbeddingProvider embeddingProvider(LlmProperties properties, SecretProtector secretProtector) {
        return new OpenAiCompatibleEmbeddingProvider(properties.getEmbedding(), secretProtector);
    }
}
