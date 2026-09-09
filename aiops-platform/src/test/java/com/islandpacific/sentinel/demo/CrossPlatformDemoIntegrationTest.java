package com.islandpacific.sentinel.demo;

import com.islandpacific.sentinel.AbstractIntegrationTest;
import com.islandpacific.sentinel.correlation.CorrelationEngineProperties;
import com.islandpacific.sentinel.correlation.CorrelationEngineService;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.KbChunk;
import com.islandpacific.sentinel.entity.KbDoc;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.provider.GovernanceLlmProviderDecorator;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.KbChunkRepository;
import com.islandpacific.sentinel.repository.KbDocRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.TopologyLinkRepository;
import com.islandpacific.sentinel.service.GovernanceAuditService;
import com.islandpacific.sentinel.service.QuotaService;
import com.islandpacific.sentinel.service.RedactionService;
import com.islandpacific.sentinel.tool.ToolRegistryService;
import com.islandpacific.sentinel.triage.TriageAgentProperties;
import com.islandpacific.sentinel.triage.TriageAgentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1.8 — the reproducible cross-platform correlation demo, reused as the acceptance test for both
 * 1.1 (correlation) and 1.2 (triage): seeding an IBM i subsystem outage plus its Windows service
 * fallout must collapse into exactly ONE incident with a root-cause narrative, and the seeded
 * runbook chunk must be retrievable by the same RAG mechanism 1.2 uses.
 *
 * The LLM step is stubbed (as {@code TriageAgentGovernanceIntegrationTest} does) so this stays
 * deterministic and self-contained - no live IBM i/Windows/LLM API dependency.
 */
class CrossPlatformDemoIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private TopologyLinkRepository topologyLinkRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private KbDocRepository kbDocRepository;
    @Autowired private KbChunkRepository kbChunkRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentSignalRepository incidentSignalRepository;
    @Autowired private IncidentTimelineRepository incidentTimelineRepository;
    @Autowired private CorrelationEngineService correlationEngineService;
    @Autowired private CorrelationEngineProperties correlationEngineProperties;
    @Autowired private ToolRegistryService toolRegistryService;
    @Autowired private RedactionService redactionService;
    @Autowired private QuotaService quotaService;
    @Autowired private GovernanceAuditService auditService;

    private static final String STUBBED_ROOT_CAUSE_JSON = "{"
            + "\"root_cause_hypothesis\":\"IBM i subsystem QINTER went down, causing dependent Windows "
            + "application services on APPSRV01 to fail with connection-refused errors\","
            + "\"evidence\":[{\"source\":\"kb_search\",\"query\":\"QINTER outage\",\"snippet\":\"restart the "
            + "subsystem with STRSBS QINTER\"}],"
            + "\"severity\":\"critical\",\"suggested_checks\":[\"STRSBS QINTER\",\"restart dependent Windows services\"],"
            + "\"confidence\":0.9}";

    @Test
    void crossPlatformSeedProducesOneIncidentWithRootCauseNarrative() {
        CrossPlatformDemoService demoService = new CrossPlatformDemoService(
                tenantRepository, monitorRepository, topologyLinkRepository, alertRepository,
                kbDocRepository, kbChunkRepository, incidentRepository, incidentSignalRepository,
                correlationEngineService, correlationEngineProperties, buildStubbedTriageAgentService());

        CrossPlatformDemoResult result = demoService.runDemo();

        // 1.1: exactly one cross-platform incident, deterministically.
        assertThat(result.getPlatforms()).containsExactly("ibmi", "windows");
        assertThat(result.getSeverity()).isEqualTo("critical");
        List<Incident> tenantIncidents = incidentRepository.findByTenantId(result.getTenantId());
        assertThat(tenantIncidents).hasSize(1);
        assertThat(tenantIncidents.get(0).getId()).isEqualTo(result.getIncidentId());

        // 1.2: a root-cause narrative was produced (not left un-triaged).
        assertThat(result.getRootCauseJson()).isNotNull();
        assertThat(result.getRootCauseJson()).contains("QINTER");

        // Seeded runbook is present and is what 1.2's kb_search/RAG mechanism would match against.
        List<KbDoc> docs = kbDocRepository.findByTenantId(result.getTenantId());
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getSource()).isEqualTo("runbook");

        List<KbChunk> chunks = kbChunkRepository.findSimilarChunks(result.getTenantId(), buildVectorString(seedVector()), 1);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getKbDocId()).isEqualTo(docs.get(0).getId());
        assertThat(chunks.get(0).getChunkText()).contains("STRSBS QINTER");
    }

    @Test
    void runningDemoTwiceCreatesTwoIndependentTenantsEachWithTheirOwnSingleIncident() {
        CrossPlatformDemoService demoService = new CrossPlatformDemoService(
                tenantRepository, monitorRepository, topologyLinkRepository, alertRepository,
                kbDocRepository, kbChunkRepository, incidentRepository, incidentSignalRepository,
                correlationEngineService, correlationEngineProperties, buildStubbedTriageAgentService());

        CrossPlatformDemoResult first = demoService.runDemo();
        CrossPlatformDemoResult second = demoService.runDemo();

        assertThat(first.getTenantId()).isNotEqualTo(second.getTenantId());
        assertThat(incidentRepository.findByTenantId(first.getTenantId())).hasSize(1);
        assertThat(incidentRepository.findByTenantId(second.getTenantId())).hasSize(1);
    }

    private TriageAgentService buildStubbedTriageAgentService() {
        StubLlmProvider stub = new StubLlmProvider(STUBBED_ROOT_CAUSE_JSON);
        GovernanceLlmProviderDecorator governed = new GovernanceLlmProviderDecorator(
                stub, redactionService, quotaService, auditService);

        return new TriageAgentService(
                tenantRepository, incidentRepository, incidentSignalRepository, incidentTimelineRepository,
                toolRegistryService, governed, redactionService, new TriageAgentProperties());
    }

    private float[] seedVector() {
        float[] v = new float[1536];
        v[0] = 1.0f;
        return v;
    }

    private String buildVectorString(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            sb.append(vec[i]);
            if (i < vec.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
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
