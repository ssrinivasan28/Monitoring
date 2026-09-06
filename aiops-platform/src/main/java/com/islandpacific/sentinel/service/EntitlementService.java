package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.Entitlement;
import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.repository.EntitlementRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.security.EntitlementDeniedException;
import com.islandpacific.sentinel.security.EntitlementTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final TenantRepository tenantRepository;
    private final AuthAuditService authAuditService;

    @Autowired
    public EntitlementService(EntitlementRepository entitlementRepository,
                              TenantRepository tenantRepository,
                              AuthAuditService authAuditService) {
        this.entitlementRepository = entitlementRepository;
        this.tenantRepository = tenantRepository;
        this.authAuditService = authAuditService;
    }

    @Transactional(readOnly = true)
    public EntitlementTier getEntitlementTier(UUID tenantId) {
        if (tenantId == null) {
            return EntitlementTier.BASIC;
        }
        Optional<Entitlement> entOpt = entitlementRepository.findByTenantId(tenantId);
        return entOpt.map(e -> EntitlementTier.fromString(e.getTier())).orElse(EntitlementTier.BASIC);
    }

    @Transactional(readOnly = true)
    public void checkEntitlement(UUID tenantId, EntitlementTier requiredTier) {
        EntitlementTier currentTier = getEntitlementTier(tenantId);
        if (!currentTier.satisfies(requiredTier)) {
            throw new EntitlementDeniedException(currentTier, requiredTier);
        }
    }

    @Transactional
    public EntitlementTier updateEntitlementTier(UUID tenantId, EntitlementTier newTier, UUID actorUserId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        Optional<Entitlement> entOpt = entitlementRepository.findByTenantId(tenantId);
        EntitlementTier oldTier = entOpt.map(e -> EntitlementTier.fromString(e.getTier())).orElse(EntitlementTier.BASIC);

        Entitlement entitlement;
        if (entOpt.isPresent()) {
            entitlement = entOpt.get();
            entitlement.setTier(newTier.name().toLowerCase());
        } else {
            entitlement = new Entitlement(tenant.getId(), newTier.name().toLowerCase());
        }
        entitlementRepository.save(entitlement);

        // Record immutable audit event transactionally
        authAuditService.logEvent(
                "ENTITLEMENT_UPDATE",
                actorUserId,
                tenantId,
                true,
                "Entitlement tier changed from " + oldTier.name() + " to " + newTier.name()
        );

        return newTier;
    }
}
