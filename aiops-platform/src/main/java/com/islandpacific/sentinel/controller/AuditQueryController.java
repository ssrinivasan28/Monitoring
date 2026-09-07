package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.TenantQuota;
import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.service.AuditQueryService;
import com.islandpacific.sentinel.service.QuotaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * REST Controller exposing staff-only audit querying and SOC 2 governance evidence APIs.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditQueryController {

    private final AuditQueryService auditQueryService;
    private final QuotaService quotaService;

    public AuditQueryController(AuditQueryService auditQueryService, QuotaService quotaService) {
        this.auditQueryService = auditQueryService;
        this.quotaService = quotaService;
    }

    /**
     * Lists agent runs with optional tenant and date filters. Staff-only.
     */
    @GetMapping("/agent-runs")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF_VIEWER', 'SUPER_ADMIN')")
    public ResponseEntity<List<AgentRun>> getAgentRuns(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        List<AgentRun> runs = auditQueryService.getAgentRuns(tenantId, since);
        return ResponseEntity.ok(runs);
    }

    /**
     * Retrieves specific agent run details including tool execution traces. Staff-only.
     */
    @GetMapping("/agent-runs/{id}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF_VIEWER', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAgentRunDetail(@PathVariable("id") UUID id) {
        Optional<AgentRun> runOpt = auditQueryService.getAgentRunDetail(id);
        if (runOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AgentRun run = runOpt.get();
        List<ToolCall> toolCalls = auditQueryService.getToolCallsForRun(run.getTenantId(), run.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("agentRun", run);
        response.put("toolCalls", toolCalls);

        return ResponseEntity.ok(response);
    }

    /**
     * Summarizes token cost and usage per tenant. Staff-only.
     */
    @GetMapping("/cost-summary")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF_VIEWER', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCostSummary(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        Map<String, Object> summary = auditQueryService.getTenantCostSummary(tenantId, since);
        return ResponseEntity.ok(summary);
    }

    /**
     * Gets quota configuration for tenant. Staff-only.
     */
    @GetMapping("/tenant-quotas/{tenantId}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF_VIEWER', 'SUPER_ADMIN')")
    public ResponseEntity<TenantQuota> getTenantQuota(@PathVariable("tenantId") UUID tenantId) {
        TenantQuota quota = quotaService.getTenantQuota(tenantId);
        return ResponseEntity.ok(quota);
    }

    /**
     * Updates quota configuration for tenant. Restricted to STAFF_ADMIN or SUPER_ADMIN.
     */
    @PutMapping("/tenant-quotas/{tenantId}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TenantQuota> updateTenantQuota(
            @PathVariable("tenantId") UUID tenantId,
            @RequestBody QuotaUpdateRequest request) {
        TenantQuota updated = quotaService.updateTenantQuota(
                tenantId,
                request.dailyTokenLimit,
                request.monthlyTokenLimit,
                request.dailyCostLimit,
                request.monthlyCostLimit
        );
        return ResponseEntity.ok(updated);
    }

    public static class QuotaUpdateRequest {
        public int dailyTokenLimit;
        public int monthlyTokenLimit;
        public BigDecimal dailyCostLimit;
        public BigDecimal monthlyCostLimit;
    }
}
