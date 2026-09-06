package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.security.EntitlementTier;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.EntitlementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class TenantAdminController {

    private final EntitlementService entitlementService;

    @Autowired
    public TenantAdminController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping("/{tenantId}/entitlement")
    @PreAuthorize("hasRole('STAFF_ADMIN')")
    public ResponseEntity<Map<String, Object>> getEntitlement(@PathVariable("tenantId") UUID tenantId) {
        EntitlementTier tier = entitlementService.getEntitlementTier(tenantId);
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "tier", tier.name()
        ));
    }

    @PutMapping("/{tenantId}/entitlement")
    @PreAuthorize("hasRole('STAFF_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateEntitlement(@PathVariable("tenantId") UUID tenantId,
                                                                 @RequestBody Map<String, String> body) {
        String tierStr = body.get("tier");
        if (tierStr == null || tierStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "error", "Bad Request", "message", "Missing tier parameter"));
        }

        EntitlementTier newTier = EntitlementTier.fromString(tierStr);
        UUID actorUserId = TenantContextHolder.getContext().map(c -> c.getUserId()).orElse(null);

        EntitlementTier updatedTier = entitlementService.updateEntitlementTier(tenantId, newTier, actorUserId);

        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "tier", updatedTier.name(),
                "status", "updated"
        ));
    }
}
