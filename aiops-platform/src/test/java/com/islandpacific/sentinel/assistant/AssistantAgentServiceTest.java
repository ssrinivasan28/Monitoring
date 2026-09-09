package com.islandpacific.sentinel.assistant;

import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.llm.model.LlmTool;
import com.islandpacific.sentinel.llm.model.LlmToolCall;
import com.islandpacific.sentinel.llm.provider.LlmProvider;
import com.islandpacific.sentinel.service.RedactionService;
import com.islandpacific.sentinel.tool.ToolExecutionResult;
import com.islandpacific.sentinel.tool.ToolRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 2.1 assistant agent — pure unit tests against mocked tool registry/LLM (no Spring context,
 * matching this codebase's TriageAgentServiceTest precedent). Governance/audit + real cross-tenant
 * isolation assertions that need a real Postgres are covered by AssistantAgentGovernanceIntegrationTest.
 */
class AssistantAgentServiceTest {

    private UUID tenantId;
    private ToolRegistryService toolRegistryService;
    private LlmProvider llmProvider;
    private AssistantAgentProperties properties;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        toolRegistryService = mock(ToolRegistryService.class);
        llmProvider = mock(LlmProvider.class);
        properties = new AssistantAgentProperties();

        when(toolRegistryService.getAvailableTools()).thenReturn(List.of(
                new LlmTool("promql_query", "PromQL", Map.of()),
                new LlmTool("ibmi_sql", "SELECT-only IBM i SQL", Map.of()),
                new LlmTool("kb_search", "KB search", Map.of())));
    }

    private AssistantAgentService newService() {
        return new AssistantAgentService(toolRegistryService, llmProvider, new RedactionService(), properties);
    }

    @Test
    void multiStepQuestionCitesEveryToolAndQueryItActuallyRan() {
        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            int call = callCount.getAndIncrement();
            if (call == 0) {
                LlmToolCall toolCall = new LlmToolCall("call-1", "promql_query",
                        Map.of("query", "ibmi_job_queue_depth > 10"));
                return LlmResponse.success("Checking job queue depth", List.of(toolCall), "stub", "stub-model");
            }
            if (call == 1) {
                LlmToolCall toolCall = new LlmToolCall("call-2", "ibmi_sql",
                        Map.of("query", "SELECT JOB_NAME FROM QSYS2.JOB_QUEUE_INFO"));
                return LlmResponse.success("Checking which jobs are running", List.of(toolCall), "stub", "stub-model");
            }
            return LlmResponse.success(
                    "Client ACME had a job-queue backup in the last 24h; JOBABC was running.",
                    List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq("promql_query"), anyMap()))
                .thenReturn(ToolExecutionResult.success(null, "ibmi_job_queue_depth is 14 for ACME"));
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq("ibmi_sql"), anyMap()))
                .thenReturn(ToolExecutionResult.success(null, "1 row: JOB_NAME=JOBABC"));

        AssistantAnswer answer = newService().answer(tenantId, "staff-admin",
                "Which clients had job-queue backups in the last 24h, and what was running?",
                AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isTrue();
        assertThat(answer.isInsufficient()).isFalse();
        assertThat(answer.getAnswer()).contains("ACME").contains("JOBABC");

        assertThat(answer.getCitations()).hasSize(2);
        assertThat(answer.getCitations().get(0).getTool()).isEqualTo("promql_query");
        assertThat(answer.getCitations().get(0).getQuery()).isEqualTo("ibmi_job_queue_depth > 10");
        assertThat(answer.getCitations().get(1).getTool()).isEqualTo("ibmi_sql");
        assertThat(answer.getCitations().get(1).getQuery()).contains("SELECT JOB_NAME");

        verify(toolRegistryService).executeTool(isNull(), eq(tenantId), eq("promql_query"), anyMap());
        verify(toolRegistryService).executeTool(isNull(), eq(tenantId), eq("ibmi_sql"), anyMap());
    }

    @Test
    void providerUnavailableReturnsCleanUnavailableAnswerNoException() {
        when(llmProvider.chat(any())).thenReturn(LlmResponse.unavailable("AI service is disabled"));

        AssistantAnswer answer = newService().answer(tenantId, "customer-viewer", "What's wrong?", AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isFalse();
        assertThat(answer.getAnswer()).isNotBlank();
        verify(toolRegistryService, never()).executeTool(any(), any(), any(), anyMap());
    }

    @Test
    void underlyingProviderExceptionDegradesToUnavailableAnswer() {
        when(llmProvider.chat(any())).thenThrow(new RuntimeException("boom"));

        AssistantAnswer answer = newService().answer(tenantId, "staff-admin", "Anything odd today?", AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isFalse();
    }

    @Test
    void iterationCapForcesOneFinalToollessTurnAndStopsCleanly() {
        properties.setMaxToolIterations(1);
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            LlmRequest req = inv.getArgument(0);
            if (req.getTools() != null && req.getTools().isEmpty()) {
                // the forced final turn - no tools offered, model must answer now
                return LlmResponse.success("Best guess from limited evidence.", List.of(), "stub", "stub-model");
            }
            LlmToolCall toolCall = new LlmToolCall("call-x", "promql_query", Map.of("query", "up"));
            return LlmResponse.success("still looking", List.of(toolCall), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq("promql_query"), anyMap()))
                .thenReturn(ToolExecutionResult.success(null, "1 result"));

        AssistantAnswer answer = newService().answer(tenantId, "staff-admin", "Keep digging", AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isTrue();
        assertThat(answer.getAnswer()).isEqualTo("Best guess from limited evidence.");

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmProvider, times(2)).chat(captor.capture());
        assertThat(captor.getAllValues().get(1).getTools()).isEmpty();
    }

    @Test
    void wallClockCapStopsLoopAndForcesFinalAnswer() {
        properties.setMaxWallClockMs(0); // trips before the very first call is made
        when(llmProvider.chat(any())).thenReturn(
                LlmResponse.success("Best guess from limited evidence.", List.of(), "stub", "stub-model"));

        AssistantAnswer answer = newService().answer(tenantId, "staff-admin", "Keep digging", AssistantProgressListener.NOOP);

        assertThat(answer.isAiAvailable()).isTrue();
        assertThat(answer.getAnswer()).isEqualTo("Best guess from limited evidence.");
        verify(llmProvider, times(1)).chat(any());
    }

    @Test
    void toolCallArgumentsCannotRedirectExecutionToAnotherTenant() {
        UUID otherTenantId = UUID.randomUUID();
        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                // A malicious/confused model tries to reference another tenant inside the query text
                // or arguments - the tool contract has no tenant-id parameter, so this cannot work.
                LlmToolCall toolCall = new LlmToolCall("call-1", "promql_query",
                        Map.of("query", "up{tenant=\"" + otherTenantId + "\"}", "tenantId", otherTenantId.toString()));
                return LlmResponse.success("Looking", List.of(toolCall), "stub", "stub-model");
            }
            return LlmResponse.success("Here is what I found for your tenant.", List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq("promql_query"), anyMap()))
                .thenReturn(ToolExecutionResult.success(null, "scoped result"));

        newService().answer(tenantId, "customer-viewer", "Show me tenant data", AssistantProgressListener.NOOP);

        // Regardless of what the model put in the tool-call arguments, execution is always scoped to
        // the caller's own tenantId - never anything derived from the model/user-controlled input.
        verify(toolRegistryService).executeTool(isNull(), eq(tenantId), eq("promql_query"), anyMap());
        verify(toolRegistryService, never()).executeTool(any(), eq(otherTenantId), any(), anyMap());
    }

    @Test
    void progressListenerIsNotifiedOfEachToolCallResultAndFinalAnswerInOrder() {
        AtomicInteger callCount = new AtomicInteger();
        when(llmProvider.chat(any())).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                LlmToolCall toolCall = new LlmToolCall("call-1", "kb_search", Map.of("query", "disk full"));
                return LlmResponse.success("Searching KB", List.of(toolCall), "stub", "stub-model");
            }
            return LlmResponse.success("Final answer.", List.of(), "stub", "stub-model");
        });
        when(toolRegistryService.executeTool(isNull(), eq(tenantId), eq("kb_search"), anyMap()))
                .thenReturn(ToolExecutionResult.success(null, "Runbook RB-1"));

        List<String> events = new ArrayList<>();
        AssistantProgressListener listener = new AssistantProgressListener() {
            @Override public void onToolCall(String tool, String query) { events.add("tool_call:" + tool); }
            @Override public void onToolResult(String tool, String summary) { events.add("tool_result:" + tool); }
            @Override public void onAnswer(AssistantAnswer answer) { events.add("answer:" + answer.getAnswer()); }
        };

        newService().answer(tenantId, "staff-admin", "Disk full anywhere?", listener);

        assertThat(events).containsExactly("tool_call:kb_search", "tool_result:kb_search", "answer:Final answer.");
    }
}
