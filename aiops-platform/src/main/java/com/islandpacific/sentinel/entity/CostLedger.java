package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cost_ledger")
public class CostLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "agent_run_id")
    private UUID agentRunId;

    @Column(nullable = false)
    private int tokens;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal cost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CostLedger() {}

    public CostLedger(UUID tenantId, UUID agentRunId, int tokens, BigDecimal cost) {
        this.tenantId = tenantId;
        this.agentRunId = agentRunId;
        this.tokens = tokens;
        this.cost = cost;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getAgentRunId() { return agentRunId; }
    public void setAgentRunId(UUID agentRunId) { this.agentRunId = agentRunId; }

    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
