package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.incident.IncidentDetailDto;
import com.islandpacific.sentinel.service.incident.IncidentListResult;
import com.islandpacific.sentinel.service.incident.IncidentQueryService;
import com.islandpacific.sentinel.service.incident.IncidentSummaryDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final IncidentRepository incidentRepository;
    private final IncidentQueryService incidentQueryService;
    private final QueryAuditService auditService;

    @Autowired
    public IncidentController(
            IncidentRepository incidentRepository,
            IncidentQueryService incidentQueryService,
            QueryAuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.incidentQueryService = incidentQueryService;
        this.auditService = auditService;
    }

    /** Tenant-scoped, ranked (severity desc, then recency) incident list. Optional status/platform filters. */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<List<IncidentSummaryDto>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        long start = System.currentTimeMillis();
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        IncidentListResult result = incidentQueryService.listIncidents(tenantId, status, platform, safePage, safeSize);

        recordAudit(tenantId, "INCIDENT_LIST", "GET /api/v1/incidents?status=" + status + "&platform=" + platform, start);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
                .header("X-Page", String.valueOf(result.getPage()))
                .header("X-Page-Size", String.valueOf(result.getSize()))
                .body(result.getContent());
    }

    /** Tenant-scoped incident detail: signals, root cause + cited evidence, runbook refs, similar incidents, timeline. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<IncidentDetailDto> getIncidentById(@PathVariable("id") UUID id) {
        long start = System.currentTimeMillis();
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Optional<IncidentDetailDto> detail = incidentQueryService.getIncidentDetail(tenantId, id);

        recordAudit(tenantId, "INCIDENT_DETAIL", "GET /api/v1/incidents/" + id, start);

        return detail.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).build());
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN')")
    public ResponseEntity<?> acknowledgeIncident(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Incident incident = incidentRepository.findByIdAndTenantId(id, tenantId)
                .orElse(null);

        if (incident == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", "Incident not found"
            ));
        }

        incident.setStatus("ack");
        incidentRepository.save(incident);

        return ResponseEntity.ok(Map.of(
                "id", incident.getId(),
                "status", incident.getStatus(),
                "message", "Incident acknowledged successfully"
        ));
    }

    private void recordAudit(UUID tenantId, String queryType, String query, long startMs) {
        UUID userId = TenantContextHolder.getContext().map(TenantContext::getUserId).orElse(UUID.randomUUID());
        auditService.recordQueryExecution(
                userId,
                tenantId,
                queryType,
                query,
                query,
                List.of("incident-query-service"),
                System.currentTimeMillis() - startMs,
                "SUCCESS",
                false,
                0,
                UUID.randomUUID().toString()
        );
    }
}
