package com.islandpacific.sentinel.fleet;

import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.security.TenantContext;
import com.islandpacific.sentinel.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FleetControllerTest {

    private FleetHeadroomService headroomService;
    private QueryAuditService auditService;
    private FleetController fleetController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        headroomService = Mockito.mock(FleetHeadroomService.class);
        auditService = Mockito.mock(QueryAuditService.class);
        fleetController = new FleetController(headroomService, auditService);

        TenantContext ctx = new TenantContext(userId, tenantId, "TEST-CLI-01", "STAFF_ADMIN");
        TenantContextHolder.setContext(ctx);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "staff@islandpacific.com", null, List.of(new SimpleGrantedAuthority("ROLE_STAFF_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetFleetOverviewEndpointReturnsDataAndAuditsCall() {
        TenantFleetDto sampleDto = new TenantFleetDto(
                tenantId, "Sample Tenant", "TEST-CLI-01", "GREEN", 45.0, "asp", List.of()
        );
        when(headroomService.getFleetOverview(isNull(), eq(userId), eq(true)))
                .thenReturn(List.of(sampleDto));

        ResponseEntity<List<TenantFleetDto>> response = fleetController.getFleetOverview();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Sample Tenant", response.getBody().get(0).getName());

        // Verify audit logging call
        verify(auditService).recordQueryExecution(
                eq(userId), eq(tenantId), eq("FLEET_OVERVIEW"), anyString(), anyString(), anyList(), anyLong(), eq("SUCCESS"), eq(false), eq(0), anyString()
        );
    }

    @Test
    void testCustomerRoleEnforcesTenantScoping() {
        UsernamePasswordAuthenticationToken customerAuth = new UsernamePasswordAuthenticationToken(
                "customer@acme.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(customerAuth);

        TenantContext customerCtx = new TenantContext(userId, tenantId, "ACME-01", "CUSTOMER_ADMIN");
        TenantContextHolder.setContext(customerCtx);

        TenantFleetDto sampleDto = new TenantFleetDto(
                tenantId, "Acme Tenant", "ACME-01", "AMBER", 82.0, "disk", List.of()
        );
        when(headroomService.getFleetOverview(eq(tenantId), eq(userId), eq(false)))
                .thenReturn(List.of(sampleDto));

        ResponseEntity<List<TenantFleetDto>> response = fleetController.getFleetOverview();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals(tenantId, response.getBody().get(0).getTenantId());
    }
}
