package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/datasources")
public class DatasourceAdminController {

    private final DatasourceAdminService adminService;

    @Autowired
    public DatasourceAdminController(DatasourceAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    public ResponseEntity<List<TenantDatasourceResponseDto>> listDatasources(@RequestParam(required = false) UUID tenantId) {
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        UUID effectiveTenantId = resolveTenantId(ctx, tenantId);
        List<TenantDatasourceResponseDto> result = adminService.listDatasources(effectiveTenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    public ResponseEntity<TenantDatasourceResponseDto> getDatasource(@PathVariable UUID id) {
        TenantDatasourceResponseDto dto = adminService.getDatasource(id);
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        verifyTenantAccess(ctx, dto.getTenantId());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantDatasourceResponseDto> createDatasource(@RequestBody TenantDatasourceCreateDto dto) {
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        if (dto.getTenantId() == null) {
            dto.setTenantId(ctx.getTenantId());
        }
        verifyTenantAccess(ctx, dto.getTenantId());

        TenantDatasourceResponseDto created = adminService.createDatasource(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantDatasourceResponseDto> updateDatasource(@PathVariable UUID id, @RequestBody TenantDatasourceUpdateDto dto) {
        TenantDatasourceResponseDto existing = adminService.getDatasource(id);
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        verifyTenantAccess(ctx, existing.getTenantId());

        TenantDatasourceResponseDto updated = adminService.updateDatasource(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteDatasource(@PathVariable UUID id) {
        TenantDatasourceResponseDto existing = adminService.getDatasource(id);
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        verifyTenantAccess(ctx, existing.getTenantId());

        adminService.deleteDatasource(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody TenantDatasourceCreateDto dto) {
        TenantContext ctx = TenantContextHolder.getContext().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Missing tenant context"));
        if (dto.getTenantId() != null) {
            verifyTenantAccess(ctx, dto.getTenantId());
        }
        Map<String, Object> result = adminService.testConnection(dto);
        return ResponseEntity.ok(result);
    }

    private UUID resolveTenantId(TenantContext ctx, UUID requestedTenantId) {
        if ("SUPER_ADMIN".equalsIgnoreCase(ctx.getRoleKey())) {
            return requestedTenantId;
        }
        return ctx.getTenantId();
    }

    private void verifyTenantAccess(TenantContext ctx, UUID targetTenantId) {
        if ("SUPER_ADMIN".equalsIgnoreCase(ctx.getRoleKey())) {
            return;
        }
        if (!ctx.getTenantId().equals(targetTenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Cross-tenant datasource access prohibited");
        }
    }
}
