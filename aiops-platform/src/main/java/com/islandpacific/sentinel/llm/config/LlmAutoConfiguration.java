package com.islandpacific.sentinel.llm.config;

import com.islandpacific.sentinel.llm.provider.*;
import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.service.GovernanceAuditService;
import com.islandpacific.sentinel.service.QuotaService;
import com.islandpacific.sentinel.service.RedactionService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean(name = "routingLlmProvider")
    public LlmProvider routingLlmProvider(LlmProperties properties,
                                   ClaudeApiProvider claudeProvider,
                                   LocalModelProvider localProvider) {
        Map<String, LlmProvider> providerMap = new HashMap<>();
        providerMap.put("claude", claudeProvider);
        providerMap.put("local", localProvider);
        return new RoutingLlmProvider(properties, providerMap);
    }

    /**
     * Primary LlmProvider seen by callers (e.g. the 1.2 triage agent): wraps routing/fallback with
     * mandatory quota enforcement, redaction, and audit/cost-metering (0.7 governance).
     */
    @Bean
    @Primary
    public LlmProvider llmProvider(@Qualifier("routingLlmProvider") LlmProvider routingLlmProvider,
                                    RedactionService redactionService,
                                    QuotaService quotaService,
                                    GovernanceAuditService auditService) {
        return new GovernanceLlmProviderDecorator(routingLlmProvider, redactionService, quotaService, auditService);
    }

    @Bean
    public EmbeddingProvider embeddingProvider(LlmProperties properties, SecretProtector secretProtector) {
        return new OpenAiCompatibleEmbeddingProvider(properties.getEmbedding(), secretProtector);
    }

    @Bean
    public Gauge llmAvailabilityGauge(MeterRegistry registry, LlmProvider llmProvider) {
        return Gauge.builder("sentinel_llm_available", llmProvider, p -> p.isAvailable() ? 1 : 0)
                .description("1 if at least one configured LLM provider is currently available, 0 if AI capabilities are fully degraded")
                .register(registry);
    }
}
