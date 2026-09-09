package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.assistant.AssistantAgentService;
import com.islandpacific.sentinel.assistant.AssistantAnswer;
import com.islandpacific.sentinel.assistant.AssistantProgressListener;
import com.islandpacific.sentinel.security.EntitlementTier;
import com.islandpacific.sentinel.security.RequiresEntitlement;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 2.1 — the AI Assistant's HTTP surface. Every endpoint is Pro-tier gated (full policy in 2.4) and
 * tenant-scoped strictly from the authenticated {@link TenantContextHolder} - never from anything
 * in the request body - so a caller cannot steer the agent at another tenant.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiresEntitlement(EntitlementTier.PRO)
public class AssistantController {

    private static final String ROLE_CHECK =
            "hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')";

    private final AssistantAgentService assistantAgentService;

    @Autowired
    public AssistantController(AssistantAgentService assistantAgentService) {
        this.assistantAgentService = assistantAgentService;
    }

    /** Synchronous chat. Response keeps the existing {@code answer} field the SPA already reads. */
    @PostMapping("/chat")
    @PreAuthorize(ROLE_CHECK)
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String prompt = request != null ? request.getOrDefault("prompt", "") : "";
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400, "error", "Bad Request", "message", "prompt must not be blank"));
        }

        AssistantAnswer answer = assistantAgentService.answer(tenantId, prompt);
        return ResponseEntity.ok(toBody(tenantId, answer));
    }

    /**
     * Streaming-ready variant for the SPA/Teams surfaces (2.2/2.3): emits each tool call/result as
     * it happens, then the final answer, over SSE. Tenant and role are resolved on the request
     * thread and passed explicitly into the background worker - never re-read from thread-local
     * context there, since that context does not propagate across threads.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(ROLE_CHECK)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String role = TenantContextHolder.getContext().map(TenantContext::getRoleKey).orElse("staff");
        String prompt = request != null ? request.getOrDefault("prompt", "") : "";

        SseEmitter emitter = new SseEmitter(120_000L);
        if (prompt.isBlank()) {
            emitter.completeWithError(new IllegalArgumentException("prompt must not be blank"));
            return emitter;
        }

        Thread worker = new Thread(() -> runStream(emitter, tenantId, role, prompt), "assistant-chat-stream");
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "assistantEnabled", true,
                "tier", "PRO"
        ));
    }

    private void runStream(SseEmitter emitter, UUID tenantId, String role, String prompt) {
        try {
            assistantAgentService.answer(tenantId, role, prompt, new AssistantProgressListener() {
                @Override
                public void onToolCall(String tool, String query) {
                    trySend(emitter, "tool_call", Map.of("tool", tool, "query", query));
                }

                @Override
                public void onToolResult(String tool, String summary) {
                    trySend(emitter, "tool_result", Map.of("tool", tool, "summary", summary));
                }

                @Override
                public void onAnswer(AssistantAnswer answer) {
                    trySend(emitter, "answer", toBody(tenantId, answer));
                }
            });
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void trySend(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private Map<String, Object> toBody(UUID tenantId, AssistantAnswer answer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", tenantId.toString());
        body.put("answer", answer.getAnswer());
        body.put("citations", answer.getCitations());
        body.put("aiAvailable", answer.isAiAvailable());
        body.put("insufficient", answer.isInsufficient());
        body.put("tier", "PRO");
        return body;
    }
}
