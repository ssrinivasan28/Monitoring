package com.islandpacific.sentinel.kpi;

import java.time.Instant;
import java.util.UUID;

public class AlertNoiseKpiDto {

    private UUID tenantId;
    private Instant periodStart;
    private Instant periodEnd;
    private long rawAlertCount;
    private long correlatedIncidentCount;
    private double noiseReductionRatio; // 0.0 - 1.0: 1 - (incidents / rawAlerts), guarded for rawAlerts == 0

    public AlertNoiseKpiDto() {}

    public AlertNoiseKpiDto(UUID tenantId, Instant periodStart, Instant periodEnd,
                             long rawAlertCount, long correlatedIncidentCount, double noiseReductionRatio) {
        this.tenantId = tenantId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.rawAlertCount = rawAlertCount;
        this.correlatedIncidentCount = correlatedIncidentCount;
        this.noiseReductionRatio = noiseReductionRatio;
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }

    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }

    public long getRawAlertCount() { return rawAlertCount; }
    public void setRawAlertCount(long rawAlertCount) { this.rawAlertCount = rawAlertCount; }

    public long getCorrelatedIncidentCount() { return correlatedIncidentCount; }
    public void setCorrelatedIncidentCount(long correlatedIncidentCount) { this.correlatedIncidentCount = correlatedIncidentCount; }

    public double getNoiseReductionRatio() { return noiseReductionRatio; }
    public void setNoiseReductionRatio(double noiseReductionRatio) { this.noiseReductionRatio = noiseReductionRatio; }
}
