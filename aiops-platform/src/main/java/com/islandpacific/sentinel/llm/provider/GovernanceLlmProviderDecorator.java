package com.islandpacific.sentinel.llm.provider;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.exception.QuotaExceededException;
import com.islandpacific.sentinel.llm.model.LlmMessage;
import com.islandpacific.sentinel.llm.model.LlmRequest;
import com.islandpacific.sentinel.llm.model.LlmResponse;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.GovernanceAuditService;
import com.islandpacific.sentinel.service.ModelPricing;
import com.islandpacific.sentinel.service.QuotaService;
import com.islandpacific.sentinel.service.RedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Decorator wrapping an LlmProvider with governance controls:
 * 1. Per-tenant quota enforcement (tokens + cost limits)
 * 2. Pre-execution secret and PII redaction on system prompt & user messages
 * 3. Token cost calculation and metering
 * 4. Immutable append-only audit logging (agent_runs + cost_ledger)
 */
public class GovernanceLlmProviderDecorator implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GovernanceLlmProviderDecorator.class);

    private final LlmProvider delegate;
    private final RedactionService redactionService;
    private final QuotaService quotaService;
    private final GovernanceAuditService auditService;

    public GovernanceLlmProviderDecorator(LlmProvider delegate,
                                           RedactionService redactionService,
                                           QuotaService quotaService,
                                           GovernanceAuditService auditService) {
        this.delegate = delegate;
        this.redactionService = redactionService;
        this.quotaService = quotaService;
        this.auditService = auditService;
    }

    @Override
    public String getProviderName() {
        return delegate != null ? delegate.getProviderName() : "governed";
    }

    @Override
    public boolean isAvailable() {
        return delegate != null && delegate.isAvailable();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        UUID tenantId = TenantContextHolder.getTenantId();

        // 1. Quota Enforcement
        if (tenantId != null) {
            try {
                quotaService.checkQuota(tenantId);
            } catch (QuotaExceededException e) {
                log.warn("Quota check failed for tenant {}: {}", tenantId, e.getMessage());
                return LlmResponse.unavailable(e.getMessage());
            }
        }

        // 2. Redaction of prompt & messages prior to LLM submission
        LlmRequest redactedRequest = sanitizeRequest(request);
        String aggregatedPrompt = extractPromptSummary(redactedRequest);

        // 3. Execute underlying provider
        LlmResponse response;
        try {
            response = delegate.chat(redactedRequest);
        } catch (Exception e) {
            log.error("Underlying LLM provider execution failed: {}", e.getMessage(), e);
            response = LlmResponse.unavailable("LLM provider execution error: " + e.getMessage());
        }

        // 4. If execution succeeded or generated response, audit & meter cost
        if (tenantId != null && response != null) {
            int promptTokens = response.getPromptTokens() != null ? response.getPromptTokens() : estimateTokens(aggregatedPrompt);
            int completionTokens = response.getCompletionTokens() != null ? response.getCompletionTokens() : estimateTokens(response.getText());
            String modelName = response.getModelName() != null ? response.getModelName() : (request.getModel() != null ? request.getModel() : "default");

            BigDecimal cost = ModelPricing.calculateCost(modelName, promptTokens, completionTokens);
            response.setPromptTokens(promptTokens);
            response.setCompletionTokens(completionTokens);

            try {
                auditService.logAgentRun(
                        tenantId,
                        getProviderName() + "_agent",
                        aggregatedPrompt,
                        response.getText(),
                        modelName,
                        promptTokens,
                        completionTokens,
                        cost
                );
            } catch (Exception auditEx) {
                log.error("Failed to write immutable audit record for tenant {}: {}", tenantId, auditEx.getMessage(), auditEx);
            }
        }

        return response;
    }

    private LlmRequest sanitizeRequest(LlmRequest original) {
        if (original == null) {
            return new LlmRequest();
        }

        LlmRequest sanitized = new LlmRequest();
        sanitized.setModel(original.getModel());
        sanitized.setMaxTokens(original.getMaxTokens());
        sanitized.setTemperature(original.getTemperature());
        sanitized.setTools(original.getTools());

        if (original.getSystemPrompt() != null) {
            sanitized.setSystemPrompt(redactionService.redact(original.getSystemPrompt()));
        }

        List<LlmMessage> sanitizedMessages = new ArrayList<>();
        if (original.getMessages() != null) {
            for (LlmMessage msg : original.getMessages()) {
                LlmMessage copy = new LlmMessage(msg.getRole(), redactionService.redact(msg.getContent()));
                copy.setName(msg.getName());
                copy.setToolCallId(msg.getToolCallId());
                copy.setToolCalls(msg.getToolCalls());
                sanitizedMessages.add(copy);
            }
        }
        sanitized.setMessages(sanitizedMessages);

        return sanitized;
    }

    private String extractPromptSummary(LlmRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getSystemPrompt() != null) {
            sb.append("[System] ").append(req.getSystemPrompt()).append("\n");
        }
        if (req.getMessages() != null) {
            for (LlmMessage msg : req.getMessages()) {
                sb.append("[").append(msg.getRole()).append("] ").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Approximate rule-of-thumb: ~4 chars per token
        return Math.max(1, text.length() / 4);
    }
}
