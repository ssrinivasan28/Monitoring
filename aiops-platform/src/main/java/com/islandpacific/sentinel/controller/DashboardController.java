package com.islandpacific.sentinel.controller;

import com.islandpacific.sentinel.security.TenantContextHolder;
import com.islandpacific.sentinel.service.dashboard.DashboardDefinitionDto;
import com.islandpacific.sentinel.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<List<DashboardDefinitionDto>> listDashboards() {
        UUID tenantId = TenantContextHolder.getTenantId();
        List<DashboardDefinitionDto> dashboards = dashboardService.getDashboardsForTenant(tenantId);
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'STAFF', 'CUSTOMER_ADMIN', 'CUSTOMER_VIEWER')")
    public ResponseEntity<?> getDashboard(@PathVariable("id") String id) {
        UUID tenantId = TenantContextHolder.getTenantId();
        return dashboardService.getDashboard(id, tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN')")
    public ResponseEntity<DashboardDefinitionDto> saveDashboard(@RequestBody DashboardDefinitionDto dto) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DashboardDefinitionDto saved = dashboardService.saveCustomDashboard(tenantId, dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDashboard(@PathVariable("id") String id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        dashboardService.deleteCustomDashboard(id, tenantId);
        return ResponseEntity.ok(Map.of("message", "Dashboard deleted successfully", "id", id));
    }

    @PostMapping("/import/grafana")
    @PreAuthorize("hasAnyRole('STAFF_ADMIN', 'STAFF_OPERATOR', 'CUSTOMER_ADMIN')")
    public ResponseEntity<DashboardDefinitionDto> importGrafanaDashboard(@RequestBody String grafanaJson) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        DashboardDefinitionDto imported = dashboardService.importGrafanaDashboard(tenantId, grafanaJson);
        return ResponseEntity.ok(imported);
    }
}
