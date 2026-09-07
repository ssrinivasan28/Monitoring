package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.TenantQuota;
import com.islandpacific.sentinel.exception.QuotaExceededException;
import com.islandpacific.sentinel.llm.model.LlmMessage;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.provider.GovernanceLlmProviderDecorator;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.TenantQuotaRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.GovernanceAuditService;
import com.islandpacific.sentinel.service.QuotaService;
import com.islandpacific.sentinel.service.RedactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QuotaEnforcementTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantQuotaRepository tenantQuotaRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private RedactionService redactionService;

    @Autowired
    private GovernanceAuditService auditService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = tenantRepository.save(new Tenant("Quota Tenant", "client-quota-01"));
        TenantContextHolder.setTenantId(testTenant.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void checkQuotaPassesWhenUsageUnderLimits() {
        tenantQuotaRepository.save(new TenantQuota(testTenant.getId(), 1000, 10000, new BigDecimal("10.00"), new BigDecimal("100.00")));
        quotaService.checkQuota(testTenant.getId());
    }

    @Test
    void checkQuotaThrowsExceptionWhenDailyTokenLimitBreached() {
        UUID tenantId = testTenant.getId();
        tenantQuotaRepository.save(new TenantQuota(tenantId, 500, 10000, new BigDecimal("10.00"), new BigDecimal("100.00")));

        // Insert agent run exceeding 500 tokens
        agentRunRepository.save(new AgentRun(tenantId, "test_agent", "prompt", "claude-3-5-sonnet", 300, 300, new BigDecimal("0.01")));

        assertThatThrownBy(() -> quotaService.checkQuota(tenantId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Daily token quota exceeded");
    }

    @Test
    void governanceDecoratorRedactsAndEnforcesQuota() {
        UUID tenantId = testTenant.getId();
        tenantQuotaRepository.save(new TenantQuota(tenantId, 100, 1000, new BigDecimal("10.00"), new BigDecimal("100.00")));

        TestLlmProvider mockProvider = new TestLlmProvider();
        GovernanceLlmProviderDecorator decorator = new GovernanceLlmProviderDecorator(
                mockProvider, redactionService, quotaService, auditService
        );

        LlmRequest req = new LlmRequest(List.of(LlmMessage.user("Analyze log with key sk-ant-secretKey1234567890")));

        // First call succeeds and redacts
        LlmResponse response = decorator.chat(req);
        assertThat(response.isAiAvailable()).isTrue();

        // Fill up quota
        agentRunRepository.save(new AgentRun(tenantId, "test_agent", "prompt", "claude-3-5-sonnet", 100, 100, new BigDecimal("0.01")));

        // Second call fails with unavailable due to quota
        LlmResponse quotaResponse = decorator.chat(req);
        assertThat(quotaResponse.isAiAvailable()).isFalse();
        assertThat(quotaResponse.getErrorMessage()).contains("Daily token quota exceeded");
    }

    private static class TestLlmProvider implements LlmProvider {
        @Override
        public LlmResponse chat(LlmRequest request) {
            LlmResponse resp = LlmResponse.success("Execution completed successfully", List.of(), "mock", "claude-3-5-sonnet");
            resp.setPromptTokens(10);
            resp.setCompletionTokens(10);
            return resp;
        }

        @Override
        public boolean isAvailable() { return true; }

        @Override
        public String getProviderName() { return "mock"; }
    }
}
