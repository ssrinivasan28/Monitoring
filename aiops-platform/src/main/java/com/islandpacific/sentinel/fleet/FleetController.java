package com.islandpacific.sentinel.fleet;

import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping
public class FleetController {

    private final FleetHeadroomService fleetHeadroomService;
    private final QueryAuditService auditService;

    @Autowired
    public FleetController(FleetHeadroomService fleetHeadroomService, QueryAuditService auditService) {
        this.fleetHeadroomService = fleetHeadroomService;
        this.auditService = auditService;
    }

    @GetMapping({"/api/fleet", "/api/v1/fleet"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TenantFleetDto>> getFleetOverview() {
        long start = System.currentTimeMillis();

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

        UUID tenantId = null;
        UUID userId = null;

        if (ctxOpt.isPresent()) {
            tenantId = ctxOpt.get().getTenantId();
            userId = ctxOpt.get().getUserId();
        }

        UUID filterTenantId = isStaff ? null : tenantId;

        List<TenantFleetDto> fleetOverview = fleetHeadroomService.getFleetOverview(filterTenantId, userId, isStaff);

        // Record audit entry for fleet access
        long latencyMs = System.currentTimeMillis() - start;
        auditService.recordQueryExecution(
                userId != null ? userId : UUID.randomUUID(),
                tenantId != null ? tenantId : (fleetOverview.isEmpty() ? UUID.randomUUID() : fleetOverview.get(0).getTenantId()),
                "FLEET_OVERVIEW",
                "GET /api/fleet",
                "GET /api/fleet",
                List.of("fleet-headroom-service"),
                latencyMs,
                "SUCCESS",
                false,
                0,
                UUID.randomUUID().toString()
        );

        return ResponseEntity.ok(fleetOverview);
    }
}
