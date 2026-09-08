package com.islandpacific.sentinel.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islandpacific.sentinel.entity.DashboardEntity;
import com.islandpacific.sentinel.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DashboardServiceTest {

    private DashboardRepository repository;
    private GrafanaDashboardImporter importer;
    private ObjectMapper objectMapper;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        repository = mock(DashboardRepository.class);
        objectMapper = new ObjectMapper();
        importer = new GrafanaDashboardImporter(objectMapper);
        service = new DashboardService(repository, importer, objectMapper);
        service.loadBundledDashboards();
    }

    @Test
    void testBundledDashboardsLoaded() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findAllGlobalAndTenant(tenantId)).thenReturn(Collections.emptyList());

        List<DashboardDefinitionDto> dashboards = service.getDashboardsForTenant(tenantId);
        assertNotNull(dashboards);
        assertTrue(dashboards.size() >= 8, "Expected at least 8 bundled dashboards");

        Optional<DashboardDefinitionDto> win = service.getDashboard("windows-monitor", tenantId);
        assertTrue(win.isPresent());
        assertEquals("Windows System Performance Monitor", win.get().getTitle());
        assertFalse(win.get().isCustom());
    }

    @Test
    void testTenantCustomDashboardOverrideAndIsolation() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        DashboardDefinitionDto customDto = new DashboardDefinitionDto("custom-1", "Tenant A Custom", "Custom", List.of("tenant"), List.of());
        
        when(repository.findAllGlobalAndTenant(tenantA)).thenReturn(List.of(
                new DashboardEntity("custom-1", tenantA, "{\"id\":\"custom-1\",\"title\":\"Tenant A Custom\",\"category\":\"Custom\",\"variables\":[\"tenant\"],\"panels\":[]}")
        ));
        when(repository.findAllGlobalAndTenant(tenantB)).thenReturn(Collections.emptyList());

        List<DashboardDefinitionDto> dashboardsA = service.getDashboardsForTenant(tenantA);
        assertTrue(dashboardsA.stream().anyMatch(d -> d.getId().equals("custom-1") && d.isCustom()));

        List<DashboardDefinitionDto> dashboardsB = service.getDashboardsForTenant(tenantB);
        assertFalse(dashboardsB.stream().anyMatch(d -> d.getId().equals("custom-1")));
    }

    @Test
    void testSaveCustomDashboard() {
        UUID tenantId = UUID.randomUUID();
        DashboardDefinitionDto input = new DashboardDefinitionDto("custom-new", "New Custom", "Custom", List.of("tenant"), List.of());

        DashboardDefinitionDto saved = service.saveCustomDashboard(tenantId, input);

        assertNotNull(saved);
        assertTrue(saved.isCustom());
        assertEquals("custom-new", saved.getId());

        ArgumentCaptor<DashboardEntity> captor = ArgumentCaptor.forClass(DashboardEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("custom-new", captor.getValue().getId());
        assertEquals(tenantId, captor.getValue().getTenantId());
    }
}
