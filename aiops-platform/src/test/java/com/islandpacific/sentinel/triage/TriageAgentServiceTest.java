package com.islandpacific.sentinel.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.llm.model.LlmToolCall;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.service.RedactionService;
import com.islandpacific.sentinel.tool.ToolExecutionResult;
import com.islandpacific.sentinel.tool.ToolRegistryService;
import com.islandpacific.sentinel.triage.model.RootCauseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 1.2 triage agent — pure unit tests against mocked repositories/tools/LLM (no Spring context,
 * matching this codebase's CorrelationEngineServiceTest precedent). Governance/audit assertions
 * that need a real Postgres are covered separately by TriageAgentGovernanceIntegrationTest.
 */
class TriageAgentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID tenantId;
    private Incident incident;
    private IncidentSignal signal;

    private TenantRepository tenantRepository;
    private IncidentRepository incidentRepository;
    private IncidentSignalRepository incidentSignalRepository;
    private IncidentTimelineRepository incidentTimelineRepository;
    private ToolRegistryService toolRegistryService;
    private LlmProvider llmProvider;
    private TriageAgentProperties properties;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        incident = new Incident(tenantId, "high", "open", "Correlated incident: FS01 / disk_used_percent");
        incident.setId(UUID.randomUUID());
        signal = new IncidentSignal(incident.getId(), tenantId, "windows");
        signal.setDetailJson("{\"metric\":\"disk_used_percent\",\"value\":92,\"threshold\":90}");

        tenantRepository = mock(TenantRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        incidentSignalRepository = mock(IncidentSignalRepository.class);
        incidentTimelineRepository = mock(IncidentTimelineRepository.class);
        toolRegistryService = mock(ToolRegistryService.class);
        llmProvider = mock(LlmProvider.class);
        properties = new TriageAgentProperties();

        when(incidentRepository.findByTenantIdAndId(tenantId, incident.getId())).thenReturn(Optional.of(incident));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())).thenReturn(List.of(signal));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(toolRegistryService.getAvailableTools()).thenReturn(List.of(
                new LlmTool("kb_search", "KB search", Map.of()),
                new LlmTool("promql_query", "PromQL", Map.of()),
                new LlmTool("list_monitors", "Not a triage tool", Map.of())));
    }

    private TriageAgentService newService() {
        return new TriageAgentService(
                tenantRepository, incidentRepository, incidentSignalRepository, incidentTimelineRepository,
                toolRegistryService, llmProvider, new RedactionService(), properties);
    }

    @Test
    void seededIncidentProducesEvidenceCitedRootCauseWithMatchedRunbook() throws Exception {
        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Disk cleanup job on FS01 stopped running, filling the volume\","
                + "\"evidence\":[{\"source\":\"kb_search\",\"query\":\"disk usage high FS01\","
                + "\"snippet\":\"Runbook RB-102: restart the disk cleanup scheduled task\"}],"
                + "\"severity\":\"high\","
                + "\"suggested_checks\":[\"Verify disk cleanup task is running\"],"
                + "\"confidence\":0.82}";

        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                LlmToolCall call = new LlmToolCall("call-1", "kb_search", Map.of("query", "disk usage high FS01"));
                return LlmResponse.success("Looking up runbooks", List.of(call), "stub", "stub-model");
            }
            return LlmResponse.success(finalJson, List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap()))
                .thenReturn(ToolExecutionResult.success(List.of(), "Found 1 relevant KB chunk(s): Runbook RB-102"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNotNull();
        RootCauseResult persisted = MAPPER.readValue(incident.getRootCauseJson(), RootCauseResult.class);
        assertThat(persisted.isInsufficient()).isFalse();
        assertThat(persisted.getEvidence()).isNotEmpty();
        assertThat(persisted.getEvidence().get(0).getSource()).isEqualTo("kb_search");
        assertThat(persisted.getEvidence().get(0).getSnippet()).contains("RB-102");

        verify(toolRegistryService).executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap());
        verify(incidentTimelineRepository).save(argThat(t ->
                "root_cause_suggested".equals(t.getEventType()) && "triage-agent".equals(t.getActor())));
    }

    @Test
    void providerUnavailableLeavesIncidentIntactNoException() {
        when(llmProvider.chat(any())).thenReturn(LlmResponse.unavailable("AI service is disabled"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNull();
        verify(incidentRepository, never()).save(any());
        verify(incidentTimelineRepository, never()).save(any());
    }

    @Test
    void malformedModelOutputRetriesThenMarksInsufficient() {
        when(llmProvider.chat(any())).thenReturn(LlmResponse.success("this is not json", List.of(), "stub", "stub-model"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNotNull();
        assertThat(incident.getRootCauseJson()).contains("insufficient data");
        verify(llmProvider, times(properties.getMaxSchemaRetries() + 1)).chat(any());
        verify(incidentTimelineRepository).save(argThat((IncidentTimeline t) ->
                "root_cause_insufficient".equals(t.getEventType()) && "triage-agent".equals(t.getActor())));
    }

    @Test
    void alreadyTriagedIncidentIsSkipped() {
        incident.setRootCauseJson("{\"root_cause_hypothesis\":\"already done\"}");

        newService().triageIncident(tenantId, incident.getId());

        verify(llmProvider, never()).chat(any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void everyLlmRequestAndToolCallInAMultiStepInvestigationCarriesTheIncidentId() {
        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Disk cleanup job on FS01 stopped running\","
                + "\"evidence\":[{\"source\":\"kb_search\",\"query\":\"disk usage high FS01\",\"snippet\":\"Runbook RB-102\"}],"
                + "\"severity\":\"high\",\"suggested_checks\":[],\"confidence\":0.8}";

        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                LlmToolCall call = new LlmToolCall("call-1", "kb_search", Map.of("query", "disk usage high FS01"));
                return LlmResponse.success("Looking up runbooks", List.of(call), "stub", "stub-model");
            }
            return LlmResponse.success(finalJson, List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap()))
                .thenReturn(ToolExecutionResult.success(List.of(), "Found 1 relevant KB chunk(s): Runbook RB-102"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNotNull();

        // Multi-step: the model chose a tool call, then concluded - 2 dynamically-driven LLM calls.
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmProvider, times(2)).chat(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(req -> assertThat(req.getIncidentId()).isEqualTo(incident.getId()));

        // The tool call itself is tagged with the same incident id, so the trace is per-incident queryable.
        verify(toolRegistryService).executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap());
    }

    @Test
    void wallClockCapStopsInvestigationAndRecordsReasonOnTimeline() {
        properties.setMaxWallClockMs(0); // trips before the very first call is made

        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Best guess from limited evidence\","
                + "\"evidence\":[{\"source\":\"kb_search\",\"query\":\"q\",\"snippet\":\"s\"}],"
                + "\"severity\":\"medium\",\"suggested_checks\":[],\"confidence\":0.3}";
        when(llmProvider.chat(any())).thenReturn(LlmResponse.success(finalJson, List.of(), "stub", "stub-model"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNotNull();
        verify(incidentTimelineRepository).save(argThat((IncidentTimeline t) ->
                "investigation_stopped".equals(t.getEventType())
                        && t.getNote() != null && t.getNote().contains("wall-clock")));
    }

    @Test
    void costBudgetCapStopsInvestigationAfterAccumulatingCostAndRecordsReasonOnTimeline() {
        properties.setMaxCostPerInvestigation(new BigDecimal("0.001"));

        String finalJson = "{"
                + "\"root_cause_hypothesis\":\"Best guess from limited evidence\","
                + "\"evidence\":[{\"source\":\"kb_search\",\"query\":\"q\",\"snippet\":\"s\"}],"
                + "\"severity\":\"medium\",\"suggested_checks\":[],\"confidence\":0.3}";
        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                LlmToolCall call = new LlmToolCall("call-1", "kb_search", Map.of("query", "q"));
                LlmResponse resp = LlmResponse.success("Looking", List.of(call), "stub", "stub-model");
                resp.setPromptTokens(1000);
                resp.setCompletionTokens(1000); // default pricing => well over the $0.001 cap
                return resp;
            }
            return LlmResponse.success(finalJson, List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap()))
                .thenReturn(ToolExecutionResult.success(List.of(), "ok"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).isNotNull();
        verify(incidentTimelineRepository).save(argThat((IncidentTimeline t) ->
                "investigation_stopped".equals(t.getEventType())
                        && t.getNote() != null && t.getNote().contains("cost budget")));
    }

    @Test
    void iterationCapStopsInvestigationAndRecordsReasonOnTimeline() {
        properties.setMaxToolIterations(1);
        // Model never stops asking for another tool call - the cap must still end the investigation.
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            LlmToolCall call = new LlmToolCall("call-x", "kb_search", Map.of("query", "q"));
            return LlmResponse.success("still looking", List.of(call), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq(incident.getId()), eq("kb_search"), anyMap()))
                .thenReturn(ToolExecutionResult.success(List.of(), "ok"));

        newService().triageIncident(tenantId, incident.getId());

        assertThat(incident.getRootCauseJson()).contains("insufficient data");
        verify(incidentTimelineRepository).save(argThat((IncidentTimeline t) ->
                "investigation_stopped".equals(t.getEventType())
                        && t.getNote() != null && t.getNote().contains("iteration limit")));
    }
}
