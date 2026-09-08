package com.islandpacific.sentinel.correlation;

import com.islandpacific.sentinel.entity.Alert;
import com.islandpacific.sentinel.entity.Correlation;
import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.entity.Monitor;
import com.islandpacific.sentinel.entity.TopologyLink;
import com.islandpacific.sentinel.repository.AlertRepository;
import com.islandpacific.sentinel.repository.CorrelationRepository;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import com.islandpacific.sentinel.repository.MonitorRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.TopologyLinkRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 1.1 correlation engine — pure unit tests against in-memory fakes of the repositories (no Spring
 * context, matching this codebase's FleetHeadroomServiceTest precedent). Covers: cross-platform
 * grouping into one incident, tenant isolation, consecutive-breach debounce, and flap suppression.
 */
class CorrelationEngineServiceTest {

    private List<Monitor> monitors;
    private List<Alert> alerts;
    private List<Incident> incidents;
    private List<IncidentSignal> signals;
    private List<Correlation> correlations;
    private List<IncidentTimeline> timelineEntries;
    private List<TopologyLink> topologyLinks;

    private AlertRepository alertRepository;
    private MonitorRepository monitorRepository;
    private IncidentRepository incidentRepository;
    private IncidentSignalRepository incidentSignalRepository;
    private CorrelationRepository correlationRepository;
    private IncidentTimelineRepository incidentTimelineRepository;
    private TopologyLinkRepository topologyLinkRepository;
    private TenantRepository tenantRepository;
    private CorrelationEngineProperties properties;
    private CorrelationEngineService service;

    @BeforeEach
    void setUp() {
        monitors = new ArrayList<>();
        alerts = new ArrayList<>();
        incidents = new ArrayList<>();
        signals = new ArrayList<>();
        correlations = new ArrayList<>();
        timelineEntries = new ArrayList<>();
        topologyLinks = new ArrayList<>();

        tenantRepository = mock(TenantRepository.class);

        monitorRepository = mock(MonitorRepository.class);
        when(monitorRepository.findByTenantId(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return monitors.stream().filter(m -> m.getTenantId().equals(tenantId)).collect(Collectors.toList());
        });
        when(monitorRepository.save(any())).thenAnswer(inv -> {
            Monitor m = inv.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            monitors.removeIf(x -> x.getId().equals(m.getId()));
            monitors.add(m);
            return m;
        });

        alertRepository = mock(AlertRepository.class);
        when(alertRepository.save(any())).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            alerts.removeIf(x -> x.getId().equals(a.getId()));
            alerts.add(a);
            return a;
        });
        when(alertRepository.findByTenantId(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return alerts.stream().filter(a -> a.getTenantId().equals(tenantId)).collect(Collectors.toList());
        });
        when(alertRepository.findByTenantIdAndClearedAtIsNullOrderByFiredAtAsc(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return alerts.stream()
                    .filter(a -> a.getTenantId().equals(tenantId) && a.getClearedAt() == null)
                    .sorted(Comparator.comparing(Alert::getFiredAt))
                    .collect(Collectors.toList());
        });

        topologyLinkRepository = mock(TopologyLinkRepository.class);
        when(topologyLinkRepository.findByTenantIdAndEnabledTrue(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return topologyLinks.stream().filter(l -> l.getTenantId().equals(tenantId) && l.isEnabled()).collect(Collectors.toList());
        });

        incidentRepository = mock(IncidentRepository.class);
        when(incidentRepository.save(any())).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            if (i.getId() == null) i.setId(UUID.randomUUID());
            incidents.removeIf(x -> x.getId().equals(i.getId()));
            incidents.add(i);
            return i;
        });
        when(incidentRepository.findByTenantId(any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            return incidents.stream().filter(i -> i.getTenantId().equals(tenantId)).collect(Collectors.toList());
        });
        when(incidentRepository.findByTenantIdAndStatus(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            String status = inv.getArgument(1);
            return incidents.stream()
                    .filter(i -> i.getTenantId().equals(tenantId) && status.equals(i.getStatus()))
                    .collect(Collectors.toList());
        });

        incidentSignalRepository = mock(IncidentSignalRepository.class);
        when(incidentSignalRepository.save(any())).thenAnswer(inv -> {
            IncidentSignal s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            signals.removeIf(x -> x.getId().equals(s.getId()));
            signals.add(s);
            return s;
        });
        when(incidentSignalRepository.findByTenantIdAndIncidentId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID incidentId = inv.getArgument(1);
            return signals.stream()
                    .filter(s -> s.getTenantId().equals(tenantId) && s.getIncidentId().equals(incidentId))
                    .collect(Collectors.toList());
        });
        when(incidentSignalRepository.existsByTenantIdAndAlertId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID alertId = inv.getArgument(1);
            return signals.stream().anyMatch(s -> s.getTenantId().equals(tenantId) && alertId.equals(s.getAlertId()));
        });

        correlationRepository = mock(CorrelationRepository.class);
        when(correlationRepository.save(any())).thenAnswer(inv -> {
            Correlation c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            correlations.add(c);
            return c;
        });

        incidentTimelineRepository = mock(IncidentTimelineRepository.class);
        when(incidentTimelineRepository.save(any())).thenAnswer(inv -> {
            IncidentTimeline t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            timelineEntries.add(t);
            return t;
        });
        when(incidentTimelineRepository.findByTenantIdAndIncidentId(any(), any())).thenAnswer(inv -> {
            UUID tenantId = inv.getArgument(0);
            UUID incidentId = inv.getArgument(1);
            return timelineEntries.stream()
                    .filter(t -> t.getTenantId().equals(tenantId) && t.getIncidentId().equals(incidentId))
                    .collect(Collectors.toList());
        });

        properties = new CorrelationEngineProperties();

        service = new CorrelationEngineService(
                tenantRepository, alertRepository, monitorRepository, incidentRepository,
                incidentSignalRepository, correlationRepository, incidentTimelineRepository,
                topologyLinkRepository, properties);
    }

    private Monitor monitor(UUID tenantId, String name, String platform) {
        return monitorRepository.save(new Monitor(tenantId, name, "TestMonitor", 3010, platform));
    }

    private Alert alert(UUID tenantId, UUID monitorId, String metric, Instant firedAt) {
        return alertRepository.save(new Alert(tenantId, monitorId, metric, 99.0, 90.0, firedAt));
    }

    @Test
    void crossPlatformSimultaneousBreaches_collapseIntoOneIncidentWithSignalsAndTimeline() {
        properties.setConsecutiveBreachThreshold(1);

        UUID tenantId = UUID.randomUUID();
        Monitor winMonitor = monitor(tenantId, "WINHOST01", "windows");
        Monitor ibmiMonitor = monitor(tenantId, "IBMI01", "ibmi");
        topologyLinks.add(new TopologyLink(tenantId, "WINHOST01", "IBMI01"));

        Instant now = Instant.now();
        alert(tenantId, winMonitor.getId(), "windows_disk_usage_percent", now);
        alert(tenantId, ibmiMonitor.getId(), "ibmi_asp_utilization_percent", now.plusSeconds(30));

        service.processTenant(tenantId);

        List<Incident> tenantIncidents = incidentRepository.findByTenantId(tenantId);
        assertThat(tenantIncidents).hasSize(1);

        Incident incident = tenantIncidents.get(0);
        List<IncidentSignal> incidentSignals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        assertThat(incidentSignals).hasSize(2);
        assertThat(incidentSignals).extracting(IncidentSignal::getPlatform).containsExactlyInAnyOrder("windows", "ibmi");
        assertThat(incident.getSeverity()).isEqualTo("critical");

        List<IncidentTimeline> timeline = incidentTimelineRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        assertThat(timeline).hasSize(2);
        assertThat(timeline).extracting(IncidentTimeline::getEventType)
                .containsExactlyInAnyOrder("incident_opened", "signal_added");
    }

    @Test
    void twoTenantsBreachingSimultaneously_noCrossTenantMerge() {
        properties.setConsecutiveBreachThreshold(1);

        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // Deliberately identical monitor name/metric/time across tenants to prove isolation
        // isn't accidental — same-looking breaches must never be merged across tenants.
        Instant now = Instant.now();
        Monitor monitorA = monitor(tenantA, "WINHOST01", "windows");
        Monitor monitorB = monitor(tenantB, "WINHOST01", "windows");
        alert(tenantA, monitorA.getId(), "windows_disk_usage_percent", now);
        alert(tenantB, monitorB.getId(), "windows_disk_usage_percent", now);

        service.processTenant(tenantA);
        service.processTenant(tenantB);

        List<Incident> incidentsA = incidentRepository.findByTenantId(tenantA);
        List<Incident> incidentsB = incidentRepository.findByTenantId(tenantB);
        assertThat(incidentsA).hasSize(1);
        assertThat(incidentsB).hasSize(1);
        assertThat(incidentsA.get(0).getId()).isNotEqualTo(incidentsB.get(0).getId());
        assertThat(incidentsA.get(0).getTenantId()).isEqualTo(tenantA);
        assertThat(incidentsB.get(0).getTenantId()).isEqualTo(tenantB);
    }

    @Test
    void consecutiveBreachThreshold_debouncesSingleCycleBlip() {
        properties.setConsecutiveBreachThreshold(3);

        UUID tenantId = UUID.randomUUID();
        Monitor monitor = monitor(tenantId, "WINHOST02", "windows");
        alert(tenantId, monitor.getId(), "windows_cpu_usage_percent", Instant.now());

        service.processTenant(tenantId);
        assertThat(incidentRepository.findByTenantId(tenantId)).isEmpty();

        service.processTenant(tenantId);
        assertThat(incidentRepository.findByTenantId(tenantId)).isEmpty();

        service.processTenant(tenantId);
        assertThat(incidentRepository.findByTenantId(tenantId)).hasSize(1);
    }

    @Test
    void flappingSignal_suppressedNotRepeatedIncidents() {
        properties.setConsecutiveBreachThreshold(1);
        properties.setFlapWindowMinutes(15);
        properties.setFlapThreshold(2);

        UUID tenantId = UUID.randomUUID();
        Monitor monitor = monitor(tenantId, "WINHOST03", "windows");

        // Episode 1: breach fires and is later cleared.
        Alert firstAlert = alert(tenantId, monitor.getId(), "windows_disk_usage_percent", Instant.now());
        service.processTenant(tenantId);

        List<Incident> afterFirst = incidentRepository.findByTenantId(tenantId);
        assertThat(afterFirst).hasSize(1);
        UUID firstIncidentId = afterFirst.get(0).getId();

        firstAlert.setClearedAt(Instant.now());
        alertRepository.save(firstAlert);

        // Episode 2: a brand new alert row for the SAME monitor+metric, well outside the normal
        // correlation window (20 min > default 5 min) — a plain time-window join would NOT match,
        // but rapid re-breaching (flap) must still land on the SAME incident, not a new one.
        alert(tenantId, monitor.getId(), "windows_disk_usage_percent", Instant.now().plus(20, ChronoUnit.MINUTES));
        service.processTenant(tenantId);

        List<Incident> afterSecond = incidentRepository.findByTenantId(tenantId);
        assertThat(afterSecond).hasSize(1);
        assertThat(afterSecond.get(0).getId()).isEqualTo(firstIncidentId);

        List<IncidentSignal> incidentSignals = incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, firstIncidentId);
        assertThat(incidentSignals).hasSize(2);

        List<IncidentTimeline> timeline = incidentTimelineRepository.findByTenantIdAndIncidentId(tenantId, firstIncidentId);
        assertThat(timeline).anyMatch(t -> t.getNote() != null && t.getNote().toLowerCase().contains("flap"));
    }
}
