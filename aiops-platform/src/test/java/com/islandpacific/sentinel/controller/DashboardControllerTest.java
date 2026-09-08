package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.service.dashboard.DashboardDefinitionDto;
import com.islandpacific.sentinel.service.dashboard.DashboardService;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DashboardControllerTest {

    private DashboardService dashboardService;
    private DashboardController controller;
    private final UUID testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        controller = new DashboardController(dashboardService);
        TenantContextHolder.setTenantId(testTenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testListDashboards() {
        DashboardDefinitionDto d1 = new DashboardDefinitionDto("windows-monitor", "Windows Monitor", "Windows", List.of("tenant"), List.of());
        when(dashboardService.getDashboardsForTenant(testTenantId)).thenReturn(List.of(d1));

        ResponseEntity<List<DashboardDefinitionDto>> response = controller.listDashboards();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("windows-monitor", response.getBody().get(0).getId());
    }

    @Test
    void testGetDashboardFoundAndNotFound() {
        DashboardDefinitionDto d1 = new DashboardDefinitionDto("windows-monitor", "Windows Monitor", "Windows", List.of("tenant"), List.of());
        when(dashboardService.getDashboard("windows-monitor", testTenantId)).thenReturn(Optional.of(d1));
        when(dashboardService.getDashboard("non-existent", testTenantId)).thenReturn(Optional.empty());

        ResponseEntity<?> res1 = controller.getDashboard("windows-monitor");
        assertEquals(200, res1.getStatusCode().value());
        assertNotNull(res1.getBody());

        ResponseEntity<?> res2 = controller.getDashboard("non-existent");
        assertEquals(404, res2.getStatusCode().value());
    }

    @Test
    void testSaveDashboard() {
        DashboardDefinitionDto input = new DashboardDefinitionDto("custom-1", "Custom 1", "Custom", List.of("tenant"), List.of());
        when(dashboardService.saveCustomDashboard(eq(testTenantId), any())).thenReturn(input);

        ResponseEntity<DashboardDefinitionDto> response = controller.saveDashboard(input);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("custom-1", response.getBody().getId());
        verify(dashboardService).saveCustomDashboard(eq(testTenantId), any());
    }

    @Test
    void testImportGrafanaDashboard() {
        String grafanaJson = "{\"title\":\"Test Grafana\"}";
        DashboardDefinitionDto imported = new DashboardDefinitionDto("imported-1", "Test Grafana", "Imported", List.of("tenant"), List.of());
        when(dashboardService.importGrafanaDashboard(testTenantId, grafanaJson)).thenReturn(imported);

        ResponseEntity<DashboardDefinitionDto> response = controller.importGrafanaDashboard(grafanaJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("imported-1", response.getBody().getId());
        verify(dashboardService).importGrafanaDashboard(testTenantId, grafanaJson);
    }
}
