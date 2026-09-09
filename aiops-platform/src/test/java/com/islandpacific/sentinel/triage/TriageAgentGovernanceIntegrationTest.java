package com.islandpacific.sentinel.triage;

import com.islandpacific.sentinel.AbstractIntegrationTest;
import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.model.LlmToolCall;
import com.islandpacific.sentinel.llm.provider.GovernanceLlmProviderDecorator;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
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
 * 1.2 governance integration: verifies a real triage call - composed exactly as
 * LlmAutoConfiguration wires the primary LlmProvider bean (RoutingLlmProvider wrapped in
 * GovernanceLlmProviderDecorator, 0.7) - is redacted before the call, written to the immutable
 * agent_runs audit log, cost-metered, and that the persisted root cause itself has secrets
 * scrubbed even though they arrived embedded in the model's own response text.
 */
class TriageAgentGovernanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentSignalRepository incidentSignalRepository;
    @Autowired private IncidentTimelineRepository incidentTimelineRepository;
    @Autowired private ToolRegistryService toolRegistryService;
    @Autowired private RedactionService redactionService;
    @Autowired private QuotaService quotaService;
    @Autowired private GovernanceAuditService auditService;
    @Autowired private AgentRunRepository agentRunRepository;
    @Autowired private ToolCallRepository toolCallRepository;

    private Tenant tenant;
    private Incident incident;

    private static final String SECRET = "sk-ant-secretKey1234567890abcdef";

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(new Tenant("Triage Tenant", "client-triage-01"));
        incident = incidentRepository.save(new Incident(
                tenant.getId(), "high", "open", "Correlated incident: FS01 / disk_used_percent"));
        incidentSignalRepository.save(new IncidentSignal(incident.getId(), tenant.getId(), "windows"));
        TenantContextHolder.setTenantId(tenant.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void triageCallIsRedactedAuditedCostMeteredAndScrubsSecretsFromPersistedRootCause() {
        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Disk pressure on FS01; found leaked credential " + SECRET + " in log line\","
                + "\"evidence\":[{\"source\":\"loki_query\",\"query\":\"{job=\\\"fs01\\\"}\",\"snippet\":\"disk at 92%\"}],"
                + "\"severity\":\"high\",\"suggested_checks\":[\"rotate the leaked key\"],\"confidence\":0.7}";

        StubLlmProvider stub = new StubLlmProvider(finalJson);
        GovernanceLlmProviderDecorator governed = new GovernanceLlmProviderDecorator(
                stub, redactionService, quotaService, auditService);

        TriageAgentProperties properties = new TriageAgentProperties();
        TriageAgentService service = new TriageAgentService(
                tenantRepository, incidentRepository, incidentSignalRepository, incidentTimelineRepository,
                toolRegistryService, governed, redactionService, properties);

        service.triageIncident(tenant.getId(), incident.getId());

        // Root cause persisted, and the secret embedded in the model's own answer was scrubbed.
        Incident reloaded = incidentRepository.findByTenantIdAndId(tenant.getId(), incident.getId()).orElseThrow();
        assertThat(reloaded.getRootCauseJson()).isNotNull();
        assertThat(reloaded.getRootCauseJson()).doesNotContain(SECRET);
        assertThat(reloaded.getRootCauseJson()).contains("SECRET_REDACTED");

        // Immutable audit log: agent_runs row written, prompt redacted, cost metered.
        List<AgentRun> runs = agentRunRepository.findByTenantId(tenant.getId());
        assertThat(runs).isNotEmpty();
        AgentRun run = runs.get(0);
        assertThat(run.getPromptRedacted()).isNotNull();
        assertThat(run.getCost()).isNotNull();
        assertThat(run.getTokensIn() + run.getTokensOut()).isGreaterThan(0);
        // 1.9: the agent_runs row is correlated to the incident it investigated.
        assertThat(run.getIncidentId()).isEqualTo(incident.getId());

        // Timeline entry recorded by the triage agent.
        List<IncidentTimeline> timeline = incidentTimelineRepository.findByTenantIdAndIncidentId(tenant.getId(), incident.getId());
        assertThat(timeline).anyMatch(t -> "root_cause_suggested".equals(t.getEventType()) && "triage-agent".equals(t.getActor()));
    }

    /**
     * 1.9: a real (non-mocked) tool call through the governed ToolRegistryService bean must be
     * tagged with the incident id too, so the per-incident agent-trace endpoint can join agent_runs
     * and tool_calls to reconstruct the whole investigation.
     */
    @Test
    void multiStepInvestigationTagsBothAgentRunsAndToolCallsWithTheIncidentId() {
        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Disk pressure on FS01\","
                + "\"evidence\":[{\"source\":\"service_status\",\"query\":\"n/a\",\"snippet\":\"all monitors reporting\"}],"
                + "\"severity\":\"high\",\"suggested_checks\":[],\"confidence\":0.6}";

        AtomicInteger callCount = new AtomicInteger();
        LlmProvider toolCallingStub = new LlmProvider() {
            @Override
            public LlmResponse chat(LlmRequest request) {
                LlmResponse resp;
                if (callCount.getAndIncrement() == 0) {
                    LlmToolCall call = new LlmToolCall("call-1", "service_status", Map.of());
                    resp = LlmResponse.success("Checking service status", List.of(call), "stub", "stub-model");
                } else {
                    resp = LlmResponse.success(finalJson, List.of(), "stub", "stub-model");
                }
                resp.setPromptTokens(10);
                resp.setCompletionTokens(10);
                return resp;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getProviderName() {
                return "stub";
            }
        };

        GovernanceLlmProviderDecorator governed = new GovernanceLlmProviderDecorator(
                toolCallingStub, redactionService, quotaService, auditService);
        TriageAgentProperties properties = new TriageAgentProperties();
        TriageAgentService service = new TriageAgentService(
                tenantRepository, incidentRepository, incidentSignalRepository, incidentTimelineRepository,
                toolRegistryService, governed, redactionService, properties);

        service.triageIncident(tenant.getId(), incident.getId());

        Incident reloaded = incidentRepository.findByTenantIdAndId(tenant.getId(), incident.getId()).orElseThrow();
        assertThat(reloaded.getRootCauseJson()).isNotNull();

        List<AgentRun> runs = agentRunRepository.findByTenantIdAndIncidentIdOrderByCreatedAtAsc(tenant.getId(), incident.getId());
        assertThat(runs).hasSize(2); // one tool-call turn, one final-answer turn

        List<ToolCall> toolCalls = toolCallRepository.findByTenantIdAndIncidentIdOrderByCreatedAtAsc(tenant.getId(), incident.getId());
        assertThat(toolCalls).hasSize(1);
        assertThat(toolCalls.get(0).getTool()).isEqualTo("service_status");
        assertThat(toolCalls.get(0).getIncidentId()).isEqualTo(incident.getId());
    }

    private static final class StubLlmProvider implements LlmProvider {
        private final String finalJson;

        StubLlmProvider(String finalJson) {
            this.finalJson = finalJson;
        }

        @Override
        public LlmResponse chat(LlmRequest request) {
            LlmResponse resp = LlmResponse.success(finalJson, List.of(), "stub", "stub-model");
            resp.setPromptTokens(42);
            resp.setCompletionTokens(42);
            return resp;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getProviderName() {
            return "stub";
        }
    }
}
