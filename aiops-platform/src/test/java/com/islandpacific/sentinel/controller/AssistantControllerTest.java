package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.assistant.AssistantAgentService;
import com.islandpacific.sentinel.assistant.AssistantAnswer;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 2.1 assistant controller — verifies the tenant used to scope every call always comes from the
 * authenticated {@link TenantContextHolder}, never from the request body (there is no tenantId
 * field in the request DTO at all), and that the SPA's existing {@code {prompt} -> {answer}}
 * contract (AiAssistantPage.tsx) keeps working now that it's backed by the real agent.
 */
class AssistantControllerTest {

    private AssistantAgentService assistantAgentService;
    private AssistantController controller;
    private final UUID tenantId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        assistantAgentService = mock(AssistantAgentService.class);
        controller = new AssistantController(assistantAgentService);
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void chat_returnsAnswerAndCitationsUsingTheExistingPromptAndAnswerContract() {
        AssistantAnswer answer = AssistantAnswer.of("JOBABC is stuck on ACME's queue.",
                List.of(new AssistantAnswer.Citation("ibmi_sql", "SELECT * FROM QSYS2.JOB_QUEUE_INFO", "1 row")));
        when(assistantAgentService.answer(tenantId, "Which jobs are stuck?")).thenReturn(answer);

        ResponseEntity<Map<String, Object>> response = controller.chat(Map.of("prompt", "Which jobs are stuck?"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("JOBABC is stuck on ACME's queue.", response.getBody().get("answer"));
        assertEquals(tenantId.toString(), response.getBody().get("tenantId"));
        assertEquals("PRO", response.getBody().get("tier"));
        assertEquals(answer.getCitations(), response.getBody().get("citations"));
    }

    @Test
    void chat_neverReadsATenantIdFromTheRequestBody_onlyFromAuthenticatedContext() {
        // The request DTO is a bare Map<String,String>; even if a caller sneaks in a "tenantId" key,
        // the controller has no code path that reads it - the service is always called with the
        // tenant bound to the authenticated request (TenantContextHolder).
        when(assistantAgentService.answer(eq(tenantId), any())).thenReturn(AssistantAnswer.of("ok", List.of()));

        controller.chat(Map.of("prompt", "hello", "tenantId", UUID.randomUUID().toString()));

        verify(assistantAgentService).answer(eq(tenantId), eq("hello"));
    }

    @Test
    void chat_blankPrompt_returns400WithoutCallingTheAgent() {
        ResponseEntity<Map<String, Object>> response = controller.chat(Map.of("prompt", "   "));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void chat_missingPromptKey_returns400WithoutCallingTheAgent() {
        ResponseEntity<Map<String, Object>> response = controller.chat(Map.of());

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void chatStream_blankPrompt_completesImmediatelyWithoutInvokingTheAgent() {
        SseEmitter emitter = controller.chatStream(Map.of("prompt", ""));

        assertNotNull(emitter);
        verifyNoInteractions(assistantAgentService);
    }

    @Test
    void chatStream_validPrompt_invokesAgentOnceWithTenantAndRoleResolvedFromAuthenticatedContext() {
        when(assistantAgentService.answer(eq(tenantId), eq("staff-admin"), eq("What's up?"), any()))
                .thenReturn(AssistantAnswer.of("fine", List.of()));

        SseEmitter emitter = controller.chatStream(Map.of("prompt", "What's up?"));

        assertNotNull(emitter);
        verify(assistantAgentService, timeout(2000))
                .answer(eq(tenantId), eq("staff-admin"), eq("What's up?"), any());
    }

    @Test
    void status_reportsProTierAndAssistantEnabledForCurrentTenant() {
        ResponseEntity<Map<String, Object>> response = controller.status();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(tenantId.toString(), response.getBody().get("tenantId"));
        assertEquals(true, response.getBody().get("assistantEnabled"));
    }
}
