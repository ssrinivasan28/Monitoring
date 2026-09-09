package com.islandpacific.sentinel.service.incident;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.entity.IncidentSignal;
import com.islandpacific.sentinel.entity.IncidentTimeline;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.repository.IncidentSignalRepository;
import com.islandpacific.sentinel.repository.IncidentTimelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit-level coverage for the 1.3 incident query/ranking/detail-assembly logic. Pure Mockito (no
 * Spring context / DB) so this can run in any environment - the tenant-isolation and HTTP-level
 * behavior is covered separately by IncidentApiIntegrationTest.
 */
class IncidentQueryServiceTest {

    private IncidentRepository incidentRepository;
    private IncidentSignalRepository incidentSignalRepository;
    private IncidentTimelineRepository incidentTimelineRepository;
    private IncidentQueryService service;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        incidentRepository = Mockito.mock(IncidentRepository.class);
        incidentSignalRepository = Mockito.mock(IncidentSignalRepository.class);
        incidentTimelineRepository = Mockito.mock(IncidentTimelineRepository.class);
        service = new IncidentQueryService(incidentRepository, incidentSignalRepository, incidentTimelineRepository);

        when(incidentSignalRepository.findByTenantId(tenantId)).thenReturn(List.of());
    }

    private Incident incident(String severity, String status, String title, Instant openedAt) {
        Incident incident = new Incident(tenantId, severity, status, title);
        incident.setId(UUID.randomUUID());
        incident.setOpenedAt(openedAt);
        return incident;
    }

    @Test
    void listIncidents_ranksBySeverityThenRecency() {
        Instant now = Instant.now();
        Incident oldCritical = incident("critical", "open", "old critical", now.minus(2, ChronoUnit.HOURS));
        Incident newCritical = incident("critical", "open", "new critical", now.minus(1, ChronoUnit.HOURS));
        Incident high = incident("high", "open", "high sev", now);
        Incident unknownSeverity = incident("weird", "open", "unrecognized severity", now);

        when(incidentRepository.findForTenant(tenantId, null, null))
                .thenReturn(List.of(high, oldCritical, unknownSeverity, newCritical));

        IncidentListResult result = service.listIncidents(tenantId, null, null, 0, 50);

        List<IncidentSummaryDto> content = result.getContent();
        assertEquals(4, result.getTotalElements());
        assertEquals("new critical", content.get(0).getTitle(), "most recent critical ranks first");
        assertEquals("old critical", content.get(1).getTitle(), "older critical ranks after newer critical");
        assertEquals("high sev", content.get(2).getTitle(), "high ranks below critical");
        assertEquals("unrecognized severity", content.get(3).getTitle(), "unrecognized severity ranks lowest");
    }

    @Test
    void listIncidents_passesFiltersThroughToRepository() {
        when(incidentRepository.findForTenant(tenantId, "open", "ibmi")).thenReturn(List.of());

        service.listIncidents(tenantId, "open", "ibmi", 0, 50);

        Mockito.verify(incidentRepository).findForTenant(tenantId, "open", "ibmi");
    }

    @Test
    void listIncidents_paginatesResults() {
        Instant now = Instant.now();
        List<Incident> incidents = List.of(
                incident("critical", "open", "a", now),
                incident("high", "open", "b", now),
                incident("medium", "open", "c", now));
        when(incidentRepository.findForTenant(tenantId, null, null)).thenReturn(incidents);

        IncidentListResult page0 = service.listIncidents(tenantId, null, null, 0, 2);
        assertEquals(2, page0.getContent().size());
        assertEquals(3, page0.getTotalElements());
        assertEquals(2, page0.getTotalPages());

        IncidentListResult page1 = service.listIncidents(tenantId, null, null, 1, 2);
        assertEquals(1, page1.getContent().size());
        assertEquals("c", page1.getContent().get(0).getTitle());
    }

    @Test
    void listIncidents_pageBeyondResults_returnsEmptyNotError() {
        when(incidentRepository.findForTenant(tenantId, null, null))
                .thenReturn(List.of(incident("low", "open", "only", Instant.now())));

        IncidentListResult result = service.listIncidents(tenantId, null, null, 5, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getIncidentDetail_missingIncident_returnsEmpty() {
        UUID incidentId = UUID.randomUUID();
        when(incidentRepository.findByTenantIdAndId(tenantId, incidentId)).thenReturn(Optional.empty());

        assertTrue(service.getIncidentDetail(tenantId, incidentId).isEmpty());
    }

    @Test
    void getIncidentDetail_noRootCause_degradesGracefully() {
        // Simulates the LLM-disabled / triage-not-yet-run case: rootCauseJson is null.
        Incident incident = incident("high", "open", "no ai yet", Instant.now());
        when(incidentRepository.findByTenantIdAndId(tenantId, incident.getId())).thenReturn(Optional.of(incident));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentRepository.findByTenantIdAndIdNot(tenantId, incident.getId())).thenReturn(List.of());

        IncidentDetailDto detail = service.getIncidentDetail(tenantId, incident.getId()).orElseThrow();

        assertNull(detail.getRootCause());
        assertTrue(detail.getRunbookReferences().isEmpty());
        assertEquals("no ai yet", detail.getTitle());
    }

    @Test
    void getIncidentDetail_malformedRootCauseJson_omittedNotThrown() {
        Incident incident = incident("high", "open", "bad json", Instant.now());
        incident.setRootCauseJson("{not valid json");
        when(incidentRepository.findByTenantIdAndId(tenantId, incident.getId())).thenReturn(Optional.of(incident));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentRepository.findByTenantIdAndIdNot(tenantId, incident.getId())).thenReturn(List.of());

        IncidentDetailDto detail = service.getIncidentDetail(tenantId, incident.getId()).orElseThrow();

        assertNull(detail.getRootCause());
    }

    @Test
    void getIncidentDetail_extractsRunbookReferencesFromEvidence() {
        Incident incident = incident("high", "open", "with root cause", Instant.now());
        incident.setRootCauseJson("{"
                + "\"root_cause_hypothesis\":\"disk pressure\","
                + "\"evidence\":["
                + "{\"source\":\"promql_query\",\"query\":\"disk_pct\",\"snippet\":\"92%\"},"
                + "{\"source\":\"kb_search\",\"query\":\"disk cleanup runbook\",\"snippet\":\"Runbook: clear temp files\"}"
                + "],"
                + "\"severity\":\"high\",\"suggested_checks\":[],\"confidence\":0.8}");
        when(incidentRepository.findByTenantIdAndId(tenantId, incident.getId())).thenReturn(Optional.of(incident));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, incident.getId())).thenReturn(List.of());
        when(incidentRepository.findByTenantIdAndIdNot(tenantId, incident.getId())).thenReturn(List.of());

        IncidentDetailDto detail = service.getIncidentDetail(tenantId, incident.getId()).orElseThrow();

        assertNotNull(detail.getRootCause());
        assertEquals("disk pressure", detail.getRootCause().getRootCauseHypothesis());
        assertEquals(2, detail.getRootCause().getEvidence().size(), "full evidence list stays nested under rootCause");
        assertEquals(1, detail.getRunbookReferences().size());
        assertEquals("kb_search", detail.getRunbookReferences().get(0).getSource());
    }

    @Test
    void getIncidentDetail_signalsAndTimelineAreMapped() {
        Incident incident = incident("high", "open", "with signals", Instant.now());
        IncidentSignal signal = new IncidentSignal(incident.getId(), tenantId, "windows");
        signal.setId(UUID.randomUUID());
        signal.setDetailJson("{\"cpu\":90}");
        IncidentTimeline entry = new IncidentTimeline(incident.getId(), tenantId, "correlation-engine", "opened", "initial breach");
        entry.setId(UUID.randomUUID());

        when(incidentRepository.findByTenantIdAndId(tenantId, incident.getId())).thenReturn(Optional.of(incident));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())).thenReturn(List.of(signal));
        when(incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, incident.getId())).thenReturn(List.of(entry));
        when(incidentRepository.findByTenantIdAndIdNot(tenantId, incident.getId())).thenReturn(List.of());

        IncidentDetailDto detail = service.getIncidentDetail(tenantId, incident.getId()).orElseThrow();

        assertEquals(1, detail.getSignals().size());
        assertEquals("windows", detail.getSignals().get(0).getPlatform());
        assertEquals(1, detail.getTimeline().size());
        assertEquals("opened", detail.getTimeline().get(0).getEventType());
    }

    @Test
    void getIncidentDetail_similarIncidents_scoredBySeverityAndPlatformOverlap() {
        Incident base = incident("critical", "open", "base incident", Instant.now());
        IncidentSignal baseSignal = new IncidentSignal(base.getId(), tenantId, "ibmi");

        Incident strongMatch = incident("critical", "resolved", "same severity and platform", Instant.now().minus(1, ChronoUnit.HOURS));
        Incident weakMatch = incident("critical", "resolved", "same severity only", Instant.now().minus(2, ChronoUnit.HOURS));
        Incident noMatch = incident("low", "resolved", "unrelated", Instant.now());

        IncidentSignal strongMatchSignal = new IncidentSignal(strongMatch.getId(), tenantId, "ibmi");
        IncidentSignal noMatchSignal = new IncidentSignal(noMatch.getId(), tenantId, "windows");

        when(incidentRepository.findByTenantIdAndId(tenantId, base.getId())).thenReturn(Optional.of(base));
        when(incidentSignalRepository.findByTenantIdAndIncidentId(tenantId, base.getId())).thenReturn(List.of(baseSignal));
        when(incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByAtAsc(tenantId, base.getId())).thenReturn(List.of());
        when(incidentRepository.findByTenantIdAndIdNot(tenantId, base.getId()))
                .thenReturn(List.of(strongMatch, weakMatch, noMatch));
        when(incidentSignalRepository.findByTenantId(tenantId))
                .thenReturn(List.of(baseSignal, strongMatchSignal, noMatchSignal));

        IncidentDetailDto detail = service.getIncidentDetail(tenantId, base.getId()).orElseThrow();

        List<IncidentSummaryDto> similar = detail.getSimilarIncidents();
        assertEquals(2, similar.size(), "the zero-overlap incident is excluded, not just deprioritized");
        assertEquals("same severity and platform", similar.get(0).getTitle(), "highest score (severity + platform) ranks first");
        assertEquals("same severity only", similar.get(1).getTitle());
    }
}
