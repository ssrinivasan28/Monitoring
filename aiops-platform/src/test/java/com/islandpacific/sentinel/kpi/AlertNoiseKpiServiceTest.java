package com.islandpacific.sentinel.kpi;

import com.islandpacific.sentinel.entity.Alert;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 1.8 alert-noise KPI - pure unit tests against in-memory fakes of the repositories (no Spring
 * context), matching this codebase's CorrelationEngineServiceTest/FleetHeadroomServiceTest
 * precedent. Covers the noise-reduction ratio formula, the divide-by-zero guard, and tenant
 * isolation of the period query.
 */
class AlertNoiseKpiServiceTest {

    private List<Alert> alerts;
    private List<Incident> incidents;

    private AlertRepository alertRepository;
    private IncidentRepository incidentRepository;
    private AlertNoiseKpiService service;

    @BeforeEach
    void setUp() {
        alerts = new ArrayList<>();
        incidents = new ArrayList<>();

        alertRepository = mock(AlertRepository.class);
        when(alertRepository.findByTenantIdAndFiredAtBetween(any(), any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            Instant start = inv.getArgument(1);
            Instant end = inv.getArgument(2);
            return alerts.stream()
                    .filter(a -> a.getTenantId().equals(tenantId)
                            && !a.getFiredAt().isBefore(start) && !a.getFiredAt().isAfter(end))
                    .toList();
        });

        incidentRepository = mock(IncidentRepository.class);
        when(incidentRepository.findByTenantIdAndOpenedAtBetween(any(), any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            Instant start = inv.getArgument(1);
            Instant end = inv.getArgument(2);
            return incidents.stream()
                    .filter(i -> i.getTenantId().equals(tenantId)
                            && !i.getOpenedAt().isBefore(start) && !i.getOpenedAt().isAfter(end))
                    .toList();
        });

        service = new AlertNoiseKpiService(alertRepository, incidentRepository);
    }

    private Alert alertAt(UUID tenantId, Instant firedAt) {
        Alert alert = new Alert(tenantId, UUID.randomUUID(), "some_metric", 99.0, 90.0, firedAt);
        alert.setId(UUID.randomUUID());
        return alert;
    }

    private Incident incidentAt(UUID tenantId, Instant openedAt) {
        Incident incident = new Incident(tenantId, "high", "open", "Correlated incident");
        incident.setId(UUID.randomUUID());
        incident.setOpenedAt(openedAt);
        return incident;
    }

    @Test
    void ratioReflectsRawAlertsCollapsingIntoFewerIncidents() {
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        // 10 raw alerts collapse into 2 correlated incidents => ratio = 1 - 2/10 = 0.8
        for (int i = 0; i < 10; i++) {
            alerts.add(alertAt(tenantId, now));
        }
        incidents.add(incidentAt(tenantId, now));
        incidents.add(incidentAt(tenantId, now));

        AlertNoiseKpiDto dto = service.computeForPeriod(tenantId, start, end);

        assertThat(dto.getTenantId()).isEqualTo(tenantId);
        assertThat(dto.getRawAlertCount()).isEqualTo(10);
        assertThat(dto.getCorrelatedIncidentCount()).isEqualTo(2);
        assertThat(dto.getNoiseReductionRatio()).isEqualTo(0.8, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void zeroRawAlerts_doesNotDivideByZero() {
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        AlertNoiseKpiDto dto = service.computeForPeriod(tenantId, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        assertThat(dto.getRawAlertCount()).isZero();
        assertThat(dto.getCorrelatedIncidentCount()).isZero();
        assertThat(dto.getNoiseReductionRatio()).isEqualTo(0.0);
    }

    @Test
    void doesNotAggregateAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        alerts.add(alertAt(tenantA, now));
        alerts.add(alertAt(tenantA, now));
        alerts.add(alertAt(tenantB, now));
        incidents.add(incidentAt(tenantA, now));
        incidents.add(incidentAt(tenantB, now));

        AlertNoiseKpiDto dtoA = service.computeForPeriod(tenantA, start, end);
        AlertNoiseKpiDto dtoB = service.computeForPeriod(tenantB, start, end);

        assertThat(dtoA.getRawAlertCount()).isEqualTo(2);
        assertThat(dtoA.getCorrelatedIncidentCount()).isEqualTo(1);
        assertThat(dtoB.getRawAlertCount()).isEqualTo(1);
        assertThat(dtoB.getCorrelatedIncidentCount()).isEqualTo(1);
    }

    @Test
    void alertsAndIncidentsOutsidePeriod_areExcluded() {
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        alerts.add(alertAt(tenantId, now)); // inside period
        alerts.add(alertAt(tenantId, now.minus(2, ChronoUnit.DAYS))); // outside period
        incidents.add(incidentAt(tenantId, now)); // inside period
        incidents.add(incidentAt(tenantId, now.plus(2, ChronoUnit.DAYS))); // outside period

        AlertNoiseKpiDto dto = service.computeForPeriod(tenantId, start, end);

        assertThat(dto.getRawAlertCount()).isEqualTo(1);
        assertThat(dto.getCorrelatedIncidentCount()).isEqualTo(1);
    }
}
