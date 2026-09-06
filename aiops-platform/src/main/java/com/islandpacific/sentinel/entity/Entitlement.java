package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entitlements")
public class Entitlement {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String tier; // basic | pro

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Entitlement() {}

    public Entitlement(UUID tenantId, String tier) {
        this.tenantId = tenantId;
        this.tier = tier;
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
