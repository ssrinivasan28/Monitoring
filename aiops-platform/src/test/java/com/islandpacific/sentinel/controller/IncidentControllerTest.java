package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.entity.Incident;
import com.islandpacific.sentinel.integration.itsm.ItsmSyncOutcome;
import com.islandpacific.sentinel.integration.itsm.ItsmSyncService;
import com.islandpacific.sentinel.integration.teams.TeamsNotificationService;
import com.islandpacific.sentinel.integration.teams.TeamsSyncOutcome;
import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.repository.IncidentRepository;
import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.incident.IncidentDetailDto;
import com.islandpacific.sentinel.service.incident.IncidentListResult;
import com.islandpacific.sentinel.service.incident.IncidentQueryService;
import com.islandpacific.sentinel.service.incident.IncidentSummaryDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class IncidentControllerTest {

    private IncidentRepository incidentRepository;
    private IncidentQueryService incidentQueryService;
    private QueryAuditService auditService;
    private TeamsNotificationService teamsNotificationService;
    private ItsmSyncService itsmSyncService;
    private IncidentController controller;

    private final UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        incidentQueryService = mock(IncidentQueryService.class);
        auditService = mock(QueryAuditService.class);
        teamsNotificationService = mock(TeamsNotificationService.class);
        itsmSyncService = mock(ItsmSyncService.class);
        controller = new IncidentController(incidentRepository, incidentQueryService, auditService, teamsNotificationService, itsmSyncService);
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void listIncidents_returnsRankedContentWithPagingHeadersAndAudits() {
        IncidentSummaryDto dto = new IncidentSummaryDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Disk pressure");
        IncidentListResult result = new IncidentListResult(List.of(dto), 7, 0, 50);
        when(incidentQueryService.listIncidents(eq(tenantId), isNull(), isNull(), eq(0), eq(50)))
                .thenReturn(result);

        ResponseEntity<List<IncidentSummaryDto>> response = controller.listIncidents(null, null, 0, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Disk pressure", response.getBody().get(0).getTitle());
        assertEquals("7", response.getHeaders().getFirst("X-Total-Count"));
        assertEquals("0", response.getHeaders().getFirst("X-Page"));
        assertEquals("50", response.getHeaders().getFirst("X-Page-Size"));

        verify(auditService).recordQueryExecution(
                any(), eq(tenantId), eq("INCIDENT_LIST"), anyString(), anyString(), anyList(), anyLong(), eq("SUCCESS"), eq(false), eq(0), anyString());
    }

    @Test
    void listIncidents_clampsPageSizeToUpperBound() {
        when(incidentQueryService.listIncidents(eq(tenantId), any(), any(), eq(0), eq(200)))
                .thenReturn(new IncidentListResult(List.of(), 0, 0, 200));

        controller.listIncidents(null, null, 0, 100_000);

        verify(incidentQueryService).listIncidents(tenantId, null, null, 0, 200);
    }

    @Test
    void listIncidents_clampsNegativePageToZero() {
        when(incidentQueryService.listIncidents(eq(tenantId), any(), any(), eq(0), eq(50)))
                .thenReturn(new IncidentListResult(List.of(), 0, 0, 50));

        controller.listIncidents(null, null, -5, 50);

        verify(incidentQueryService).listIncidents(tenantId, null, null, 0, 50);
    }

    @Test
    void listIncidents_passesStatusAndPlatformFilters() {
        when(incidentQueryService.listIncidents(eq(tenantId), eq("open"), eq("windows"), eq(0), eq(50)))
                .thenReturn(new IncidentListResult(List.of(), 0, 0, 50));

        controller.listIncidents("open", "windows", 0, 50);

        verify(incidentQueryService).listIncidents(tenantId, "open", "windows", 0, 50);
    }

    @Test
    void getIncidentById_found_returnsDetailAndAudits() {
        UUID incidentId = UUID.randomUUID();
        IncidentDetailDto detail = new IncidentDetailDto();
        detail.setId(incidentId);
        when(incidentQueryService.getIncidentDetail(tenantId, incidentId)).thenReturn(Optional.of(detail));

        ResponseEntity<IncidentDetailDto> response = controller.getIncidentById(incidentId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(incidentId, response.getBody().getId());
        verify(auditService).recordQueryExecution(
                any(), eq(tenantId), eq("INCIDENT_DETAIL"), anyString(), anyString(), anyList(), anyLong(), eq("SUCCESS"), eq(false), eq(0), anyString());
    }

    @Test
    void getIncidentById_notFound_returns404WithoutLeakingExistence() {
        UUID incidentId = UUID.randomUUID();
        when(incidentQueryService.getIncidentDetail(tenantId, incidentId)).thenReturn(Optional.empty());

        ResponseEntity<IncidentDetailDto> response = controller.getIncidentById(incidentId);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void resolveIncident_notFound_returns404() {
        UUID incidentId = UUID.randomUUID();
        when(incidentRepository.findByIdAndTenantId(incidentId, tenantId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.resolveIncident(incidentId);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void resolveIncident_found_setsStatusResolvedAndResolvedAt() {
        Incident incident = new Incident(tenantId, "high", "ack", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));

        ResponseEntity<?> response = controller.resolveIncident(incident.getId());

        assertEquals(200, response.getStatusCode().value());
        assertEquals("resolved", incident.getStatus());
        assertNotNull(incident.getResolvedAt());
        verify(incidentRepository).save(incident);
    }

    @Test
    void pushIncidentToItsm_notFound_returns404AndSkipsItsmCall() {
        UUID incidentId = UUID.randomUUID();
        when(incidentRepository.findByIdAndTenantId(incidentId, tenantId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.pushIncidentToItsm(incidentId);

        assertEquals(404, response.getStatusCode().value());
        verify(itsmSyncService, never()).syncIncident(any(), any());
    }

    @Test
    void pushIncidentToItsm_created_returns200() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(itsmSyncService.syncIncident(tenantId, incident)).thenReturn(ItsmSyncOutcome.created());

        ResponseEntity<?> response = controller.pushIncidentToItsm(incident.getId());

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void pushIncidentToItsm_notConfigured_returns400() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(itsmSyncService.syncIncident(tenantId, incident)).thenReturn(ItsmSyncOutcome.skippedNotConfigured());

        ResponseEntity<?> response = controller.pushIncidentToItsm(incident.getId());

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void pushIncidentToItsm_failed_returns502() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(itsmSyncService.syncIncident(tenantId, incident)).thenReturn(ItsmSyncOutcome.failed("ServiceNow unreachable"));

        ResponseEntity<?> response = controller.pushIncidentToItsm(incident.getId());

        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void pushIncidentToTeams_notFound_returns404AndSkipsTeamsCall() {
        UUID incidentId = UUID.randomUUID();
        when(incidentRepository.findByIdAndTenantId(incidentId, tenantId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.pushIncidentToTeams(incidentId);

        assertEquals(404, response.getStatusCode().value());
        verify(teamsNotificationService, never()).pushIncidentCard(any(), any());
    }

    @Test
    void pushIncidentToTeams_created_returns200() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(teamsNotificationService.pushIncidentCard(tenantId, incident)).thenReturn(TeamsSyncOutcome.created());

        ResponseEntity<?> response = controller.pushIncidentToTeams(incident.getId());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of(
                        "id", incident.getId(),
                        "teamsStatus", "CREATED",
                        "message", "Pushed to Teams"),
                response.getBody());
    }

    @Test
    void pushIncidentToTeams_notConfigured_returns400() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(teamsNotificationService.pushIncidentCard(tenantId, incident)).thenReturn(TeamsSyncOutcome.skippedNotConfigured());

        ResponseEntity<?> response = controller.pushIncidentToTeams(incident.getId());

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void pushIncidentToTeams_failed_returns502() {
        Incident incident = new Incident(tenantId, "high", "open", "Disk pressure");
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findByIdAndTenantId(incident.getId(), tenantId)).thenReturn(Optional.of(incident));
        when(teamsNotificationService.pushIncidentCard(tenantId, incident)).thenReturn(TeamsSyncOutcome.failed("Microsoft Graph request failed"));

        ResponseEntity<?> response = controller.pushIncidentToTeams(incident.getId());

        assertEquals(502, response.getStatusCode().value());
    }
}
