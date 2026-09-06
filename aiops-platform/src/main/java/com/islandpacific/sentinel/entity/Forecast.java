package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "forecasts")
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String series;

    @Column(name = "projected_at", nullable = false)
    private Instant projectedAt;

    @Column(name = "projected_value", nullable = false)
    private double projectedValue;

    private String driver;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Forecast() {}

    public Forecast(UUID tenantId, String series, Instant projectedAt, double projectedValue, String driver) {
        this.tenantId = tenantId;
        this.series = series;
        this.projectedAt = projectedAt;
        this.projectedValue = projectedValue;
        this.driver = driver;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public Instant getProjectedAt() { return projectedAt; }
    public void setProjectedAt(Instant projectedAt) { this.projectedAt = projectedAt; }

    public double getProjectedValue() { return projectedValue; }
    public void setProjectedValue(double projectedValue) { this.projectedValue = projectedValue; }

    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
