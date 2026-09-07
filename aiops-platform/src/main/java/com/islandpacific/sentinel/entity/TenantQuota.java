package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_quotas")
public class TenantQuota {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "daily_token_limit", nullable = false)
    private int dailyTokenLimit = 100000;

    @Column(name = "monthly_token_limit", nullable = false)
    private int monthlyTokenLimit = 2000000;

    @Column(name = "daily_cost_limit", nullable = false, precision = 10, scale = 4)
    private BigDecimal dailyCostLimit = new BigDecimal("50.0000");

    @Column(name = "monthly_cost_limit", nullable = false, precision = 10, scale = 4)
    private BigDecimal monthlyCostLimit = new BigDecimal("1000.0000");

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TenantQuota() {}

    public TenantQuota(UUID tenantId, int dailyTokenLimit, int monthlyTokenLimit, BigDecimal dailyCostLimit, BigDecimal monthlyCostLimit) {
        this.tenantId = tenantId;
        this.dailyTokenLimit = dailyTokenLimit;
        this.monthlyTokenLimit = monthlyTokenLimit;
        this.dailyCostLimit = dailyCostLimit;
        this.monthlyCostLimit = monthlyCostLimit;
        this.updatedAt = Instant.now();
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public int getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(int dailyTokenLimit) { this.dailyTokenLimit = dailyTokenLimit; }

    public int getMonthlyTokenLimit() { return monthlyTokenLimit; }
    public void setMonthlyTokenLimit(int monthlyTokenLimit) { this.monthlyTokenLimit = monthlyTokenLimit; }

    public BigDecimal getDailyCostLimit() { return dailyCostLimit; }
    public void setDailyCostLimit(BigDecimal dailyCostLimit) { this.dailyCostLimit = dailyCostLimit; }

    public BigDecimal getMonthlyCostLimit() { return monthlyCostLimit; }
    public void setMonthlyCostLimit(BigDecimal monthlyCostLimit) { this.monthlyCostLimit = monthlyCostLimit; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
