package com.islandpacific.sentinel.fleet;

import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.query.QueryGatewayService;
import com.islandpacific.sentinel.query.ResponseMerger;
import com.islandpacific.sentinel.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class FleetHeadroomServiceTest {

    private TenantRepository tenantRepository;
    private QueryGatewayService queryGatewayService;
    private FleetHeadroomProperties properties;
    private FleetHeadroomService fleetService;

    private Tenant sampleTenant;

    @BeforeEach
    void setUp() {
        tenantRepository = Mockito.mock(TenantRepository.class);
        queryGatewayService = Mockito.mock(QueryGatewayService.class);
        properties = new FleetHeadroomProperties();

        // Defaults: asp=0.40, disk=0.25, cpu=0.20, memJobq=0.15, critical=95.0, amber=75.0, red cutoff=90.0
        fleetService = new FleetHeadroomService(tenantRepository, queryGatewayService, properties);

        sampleTenant = new Tenant("Test Customer", "TEST-INST-01");
        sampleTenant.setId(UUID.randomUUID());
    }

    @Test
    void testWeightedBlendHeadroomScoreCalculation() {
        // Mock standard metric responses: ASP 50%, Disk 40%, CPU 30%, Mem 20%
        mockMetricQuery("ibmi_asp_utilization_percent", 50.0);
        mockMetricQuery("windows_disk_usage_percent", 40.0);
        mockMetricQuery("ibmi_cpu_utilization_percent", 30.0);
        mockMetricQuery("windows_cpu_usage_percent", 20.0);
        mockMetricQuery("windows_memory_usage_percent", 20.0);
        mockMetricQuery("job_queue_monitor_waiting_jobs", 0.0);
        mockMetricQuery("job_queue_monitor_threshold", 10.0);

        TenantFleetDto dto = fleetService.computeTenantHeadroom(sampleTenant, UUID.randomUUID());

        assertNotNull(dto);
        assertEquals(sampleTenant.getId(), dto.getTenantId());
        assertEquals("GREEN", dto.getBand());
        // Expected weighted blend: (50*0.40 + 40*0.25 + 30*0.20 + 20*0.15) = 20 + 10 + 6 + 3 = 39.0
        assertEquals(39.0, dto.getScore(), 0.1);
        assertEquals("asp", dto.getWorstInput());
        assertEquals(4, dto.getInputs().size());
    }

    @Test
    void testHardCriticalOverride_SingleCriticalInputTurnsTenantRed() {
        // ASP = 96.0% (CRITICAL override >= 95.0%), others are very healthy (10%)
        mockMetricQuery("ibmi_asp_utilization_percent", 96.0);
        mockMetricQuery("windows_disk_usage_percent", 10.0);
        mockMetricQuery("ibmi_cpu_utilization_percent", 10.0);
        mockMetricQuery("windows_cpu_usage_percent", 10.0);
        mockMetricQuery("windows_memory_usage_percent", 10.0);
        mockMetricQuery("job_queue_monitor_waiting_jobs", 0.0);

        TenantFleetDto dto = fleetService.computeTenantHeadroom(sampleTenant, UUID.randomUUID());

        assertNotNull(dto);
        // Single critical input MUST turn overall tenant RED regardless of low average blend
        assertEquals("RED", dto.getBand(), "Hard critical override must force tenant band to RED");
        assertEquals("asp", dto.getWorstInput());
        assertTrue(dto.getScore() >= 96.0, "Score should reflect critical pressure level");
    }

    @Test
    void testStaleAndMissingSeriesMarkedUnknownNotHealthyZero() {
        // Simulate missing metrics (gateway returns empty or 404)
        when(queryGatewayService.executePromQlInstant(anyString(), any(), anyString()))
                .thenReturn(new ResponseMerger.MergedResult(200, "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[]}}"));

        TenantFleetDto dto = fleetService.computeTenantHeadroom(sampleTenant, UUID.randomUUID());

        assertNotNull(dto);
        assertEquals("UNKNOWN", dto.getBand());
        assertEquals(0.0, dto.getScore());
        assertNull(dto.getWorstInput());

        for (TenantFleetDto.InputScoreDto input : dto.getInputs()) {
            assertTrue(input.isStale(), "Missing inputs must be marked stale");
            assertNull(input.getValue(), "Stale inputs must have null value");
            assertEquals("UNKNOWN", input.getBand(), "Stale inputs must be marked UNKNOWN band");
        }
    }

    private void mockMetricQuery(String metricName, double value) {
        String json = String.format(
                "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[{\"metric\":{},\"value\":[1700000000,\"%.2f\"]}]}}",
                value
        );
        when(queryGatewayService.executePromQlInstant(eq(metricName), any(), anyString()))
                .thenReturn(new ResponseMerger.MergedResult(200, json));
    }
}
