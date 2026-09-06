package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threshold_recommendations")
public class ThresholdRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String series;

    @Column(name = "current", nullable = false)
    private double current;

    @Column(name = "recommended", nullable = false)
    private double recommended;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ThresholdRecommendation() {}

    public ThresholdRecommendation(UUID tenantId, String series, double current, double recommended, String rationale, String status) {
        this.tenantId = tenantId;
        this.series = series;
        this.current = current;
        this.recommended = recommended;
        this.rationale = rationale;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public double getCurrent() { return current; }
    public void setCurrent(double current) { this.current = current; }

    public double getRecommended() { return recommended; }
    public void setRecommended(double recommended) { this.recommended = recommended; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
