package com.islandpacific.sentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "monitor_id")
    private UUID monitorId;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private double threshold;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    @Column(name = "cleared_at")
    private Instant clearedAt;

    public Alert() {}

    public Alert(UUID tenantId, UUID monitorId, String metric, double value, double threshold, Instant firedAt) {
        this.tenantId = tenantId;
        this.monitorId = monitorId;
        this.metric = metric;
        this.value = value;
        this.threshold = threshold;
        this.firedAt = firedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public Instant getFiredAt() { return firedAt; }
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }

    public Instant getClearedAt() { return clearedAt; }
    public void setClearedAt(Instant clearedAt) { this.clearedAt = clearedAt; }
}
