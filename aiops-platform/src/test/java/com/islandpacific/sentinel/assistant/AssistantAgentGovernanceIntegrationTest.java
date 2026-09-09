package com.islandpacific.sentinel.assistant;

import com.islandpacific.sentinel.AbstractIntegrationTest;
import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.llm.model.LlmMessage;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.model.LlmToolCall;
import com.islandpacific.sentinel.llm.provider.GovernanceLlmProviderDecorator;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.ToolCallRepository;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.GovernanceAuditService;
import com.islandpacific.sentinel.service.QuotaService;
import com.islandpacific.sentinel.service.RedactionService;
import com.islandpacific.sentinel.tool.ToolRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2.1 governance + isolation integration: verifies a real assistant call - composed exactly as
 * LlmAutoConfiguration wires the primary LlmProvider bean (0.7 governance) and the real 0.6
 * ToolRegistryService/ListMonitorsTool - is redacted, audited, cost-metered, and - the chunk's
 * hard acceptance requirement - cannot surface another tenant's data no matter what the model does
 * with the evidence it's handed.
 */
class AssistantAgentGovernanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private ToolRegistryService toolRegistryService;
    @Autowired private RedactionService redactionService;
    @Autowired private QuotaService quotaService;
    @Autowired private GovernanceAuditService auditService;
    @Autowired private AgentRunRepository agentRunRepository;
    @Autowired private ToolCallRepository toolCallRepository;

    private static final String SECRET = "sk-ant-secretKey1234567890abcdef";

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void assistantCallIsRedactedAuditedAndCostMetered() {
        Tenant tenant = tenantRepository.save(new Tenant("Assistant Tenant", "client-assistant-01"));

        String finalAnswer = "The disk is fine now; found a leaked credential " + SECRET + " in a log line earlier.";
        LlmProvider stub = new LlmProvider() {
            @Override
            public LlmResponse chat(LlmRequest request) {
                LlmResponse resp = LlmResponse.success(finalAnswer, List.of(), "stub", "stub-model");
                resp.setPromptTokens(42);
                resp.setCompletionTokens(42);
                return resp;
            }

            @Override
            public boolean isAvailable() { return true; }

            @Override
            public String getProviderName() { return "stub"; }
        };

        GovernanceLlmProviderDecorator governed = new GovernanceLlmProviderDecorator(
                stub, redactionService, quotaService, auditService);
        AssistantAgentService service = new AssistantAgentService(
                toolRegistryService, governed, redactionService, new AssistantAgentProperties());

        AssistantAnswer answer = service.answer(tenant.getId(), "staff-admin", "Is disk pressure resolved?", AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isTrue();
        assertThat(answer.getAnswer()).doesNotContain(SECRET);
        assertThat(answer.getAnswer()).contains("SECRET_REDACTED");

        List<AgentRun> runs = agentRunRepository.findByTenantId(tenant.getId());
        assertThat(runs).isNotEmpty();
        AgentRun run = runs.get(0);
        assertThat(run.getPromptRedacted()).isNotNull();
        assertThat(run.getCost()).isNotNull();
        assertThat(run.getTokensIn() + run.getTokensOut()).isGreaterThan(0);
    }

    /**
     * The chunk's hard acceptance criterion: "a user cannot pull another tenant's data through the
     * agent." Two tenants each have a distinctly-named monitor; the model can only ever see the
     * tool output for the tenant it was invoked for (ListMonitorsTool queries by tenantId), and this
     * test proves that by having the stub model echo back verbatim whatever the tool handed it -
     * if isolation ever broke, tenant B's monitor name would leak into tenant A's answer.
     */
    @Test
    void cannotPullAnotherTenantsDataThroughTheAgent() {
        Tenant tenantA = tenantRepository.save(new Tenant("Tenant A", "client-tenant-a"));
        Tenant tenantB = tenantRepository.save(new Tenant("Tenant B", "client-tenant-b"));
        monitorRepository.save(new Monitor(tenantA.getId(), "ACME-PROD-DB", "windows", 3010));
        monitorRepository.save(new Monitor(tenantB.getId(), "OTHERCO-PROD-DB", "windows", 3010));

        AtomicInteger callCount = new AtomicInteger();
        LlmProvider echoingStub = new LlmProvider() {
            @Override
            public LlmResponse chat(LlmRequest request) {
                LlmResponse resp;
                if (callCount.getAndIncrement() == 0) {
                    LlmToolCall call = new LlmToolCall("call-1", "list_monitors", Map.of());
                    resp = LlmResponse.success("Checking monitors", List.of(call), "stub", "stub-model");
                } else {
                    // A model that only ever repeats back what the tool told it - proving the leak
                    // (if any) would have to come from the tool call itself, not model imagination.
                    String toolOutput = request.getMessages().stream()
                            .filter(m -> m.getRole() == LlmMessage.Role.TOOL)
                            .map(LlmMessage::getContent)
                            .findFirst()
                            .orElse("");
                    resp = LlmResponse.success("Monitors on file: " + toolOutput, List.of(), "stub", "stub-model");
                }
                resp.setPromptTokens(10);
                resp.setCompletionTokens(10);
                return resp;
            }

            @Override
            public boolean isAvailable() { return true; }

            @Override
            public String getProviderName() { return "stub"; }
        };

        GovernanceLlmProviderDecorator governed = new GovernanceLlmProviderDecorator(
                echoingStub, redactionService, quotaService, auditService);
        AssistantAgentService service = new AssistantAgentService(
                toolRegistryService, governed, redactionService, new AssistantAgentProperties());

        AssistantAnswer answer = service.answer(tenantA.getId(), "customer-viewer", "What monitors do we have?", AssistantProgressListener.NOOP);

        assertThat(answer.getAnswer()).contains("ACME-PROD-DB");
        assertThat(answer.getAnswer()).doesNotContain("OTHERCO-PROD-DB");

        List<ToolCall> tenantAToolCalls = toolCallRepository.findByTenantId(tenantA.getId());
        assertThat(tenantAToolCalls).anyMatch(tc -> "list_monitors".equals(tc.getTool()));
        assertThat(toolCallRepository.findByTenantId(tenantB.getId())).isEmpty();
    }
}
