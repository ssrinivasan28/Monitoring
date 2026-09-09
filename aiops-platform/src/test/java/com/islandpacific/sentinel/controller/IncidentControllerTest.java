package com.islandpacific.sentinel.controller;

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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class IncidentControllerTest {

    private IncidentRepository incidentRepository;
    private IncidentQueryService incidentQueryService;
    private QueryAuditService auditService;
    private IncidentController controller;

    private final UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        incidentQueryService = mock(IncidentQueryService.class);
        auditService = mock(QueryAuditService.class);
        controller = new IncidentController(incidentRepository, incidentQueryService, auditService);
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
}
