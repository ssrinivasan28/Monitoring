package com.islandpacific.sentinel.kpi;

import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 1.8 — exposes the alert-noise reduction KPI (raw alerts vs. correlated incidents) for a tenant
 * over a period. Basic-tier (not Pro/AI-gated): consumed by the Fleet Overview and, later, by
 * reports (3.4). Staff may query any tenant; customers may only ever see their own.
 */
@RestController
public class AlertNoiseKpiController {

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private final AlertNoiseKpiService kpiService;
    private final QueryAuditService auditService;

    @Autowired
    public AlertNoiseKpiController(AlertNoiseKpiService kpiService, QueryAuditService auditService) {
        this.kpiService = kpiService;
        this.auditService = auditService;
    }

    @GetMapping({"/api/kpi/alert-noise", "/api/v1/kpi/alert-noise"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertNoiseKpiDto> getAlertNoiseKpi(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        long requestStart = System.currentTimeMillis();

        Optional<TenantContext> ctxOpt = TenantContextHolder.getContext();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isStaff = false;
        if (auth != null && auth.getAuthorities() != null) {
            isStaff = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().startsWith("ROLE_STAFF") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        }
        if (!isStaff && ctxOpt.isPresent()) {
            String roleKey = ctxOpt.get().getRoleKey();
            if (roleKey != null && (roleKey.contains("staff") || roleKey.contains("super"))) {
                isStaff = true;
            }
        }

        UUID ownTenantId = ctxOpt.map(TenantContext::getTenantId).orElse(null);
        UUID userId = ctxOpt.map(TenantContext::getUserId).orElse(null);

        UUID resolvedTenantId;
        if (isStaff) {
            resolvedTenantId = tenantId != null ? tenantId : ownTenantId;
        } else {
            // Strict tenant isolation: a non-staff caller can never read another tenant's KPI,
            // even if they pass a foreign tenantId explicitly.
            if (ownTenantId == null || (tenantId != null && !tenantId.equals(ownTenantId))) {
                return ResponseEntity.status(403).build();
            }
            resolvedTenantId = ownTenantId;
        }

        if (resolvedTenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        Instant periodEnd = end != null ? end : Instant.now();
        Instant periodStart = start != null ? start : periodEnd.minus(Duration.ofDays(DEFAULT_PERIOD_DAYS));

        AlertNoiseKpiDto dto = kpiService.computeForPeriod(resolvedTenantId, periodStart, periodEnd);

        long latencyMs = System.currentTimeMillis() - requestStart;
        auditService.recordQueryExecution(
                userId != null ? userId : UUID.randomUUID(),
                resolvedTenantId,
                "ALERT_NOISE_KPI",
                "GET /api/kpi/alert-noise",
                "GET /api/kpi/alert-noise",
                List.of("alert-noise-kpi-service"),
                latencyMs,
                "SUCCESS",
                false,
                0,
                UUID.randomUUID().toString()
        );

        return ResponseEntity.ok(dto);
    }
}
