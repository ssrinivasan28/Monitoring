package com.islandpacific.sentinel.kpi;

import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 1.8 — Primary success metric: alert-noise reduction. Per tenant + period, compares the raw
 * alert volume (0.x monitors) against the correlated incident count (1.1) that volume collapsed
 * into. Read-only, tenant-scoped, backfillable over any historical period from stored alerts/incidents.
 */
@Service
public class AlertNoiseKpiService {

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    @Autowired
    public AlertNoiseKpiService(AlertRepository alertRepository, IncidentRepository incidentRepository) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
    }

    /** Computes the noise-reduction KPI for a single tenant over [start, end]. Never mixes tenants. */
    public AlertNoiseKpiDto computeForPeriod(UUID tenantId, Instant start, Instant end) {
        long rawAlertCount = alertRepository.findByTenantIdAndFiredAtBetween(tenantId, start, end).size();
        long correlatedIncidentCount = incidentRepository.findByTenantIdAndOpenedAtBetween(tenantId, start, end).size();

        double ratio = rawAlertCount == 0 ? 0.0 : 1.0 - ((double) correlatedIncidentCount / rawAlertCount);

        return new AlertNoiseKpiDto(tenantId, start, end, rawAlertCount, correlatedIncidentCount, ratio);
    }
}
