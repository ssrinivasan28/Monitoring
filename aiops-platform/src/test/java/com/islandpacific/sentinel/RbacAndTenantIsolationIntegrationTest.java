package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import com.islandpacific.sentinel.security.*;
import com.islandpacific.sentinel.service.EntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RbacAndTenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTenantRoleRepository userTenantRoleRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private AuthAuditLogRepository authAuditLogRepository;

    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private EntitlementService entitlementService;

    private Tenant tenantA;
    private Tenant tenantB;

    private Role staffAdminRole;
    private Role staffOperatorRole;
    private Role customerAdminRole;
    private Role customerViewerRole;

    private User staffUser;
    private User customerUserA;
    private User viewerUserA;
    private User customerUserB;

    private Incident incidentA;
    private Incident incidentB;

    private String staffToken;
    private String customerAToken;
    private String viewerAToken;
    private String customerBToken;

    @BeforeEach
    void setupTestData() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("TRUNCATE TABLE user_tokens, auth_audit_log, user_tenant_roles, incidents, entitlements, users, roles, tenants CASCADE");
        }

        tenantA = tenantRepository.save(new Tenant("Tenant Alpha", "CLIENT-ALPHA"));
        tenantB = tenantRepository.save(new Tenant("Tenant Beta", "CLIENT-BETA"));

        entitlementRepository.save(new Entitlement(tenantA.getId(), "basic"));
        entitlementRepository.save(new Entitlement(tenantB.getId(), "pro"));

        staffAdminRole = roleRepository.save(new Role("staff-admin"));
        staffOperatorRole = roleRepository.save(new Role("staff-operator"));
        customerAdminRole = roleRepository.save(new Role("customer-admin"));
        customerViewerRole = roleRepository.save(new Role("customer-viewer"));

        // Staff user spanning Tenant A and Tenant B
        staffUser = userRepository.save(new User("staff.admin@islandpacific.com", "Staff Admin", "azure_ad"));
        userTenantRoleRepository.save(new UserTenantRole(staffUser, tenantA, staffAdminRole));
        userTenantRoleRepository.save(new UserTenantRole(staffUser, tenantB, staffAdminRole));

        // Customer Admin A (Tenant A only)
        customerUserA = userRepository.save(new User("admin@alphacorp.com", "Alpha Admin", "local"));
        userTenantRoleRepository.save(new UserTenantRole(customerUserA, tenantA, customerAdminRole));

        // Customer Viewer A (Tenant A only)
        viewerUserA = userRepository.save(new User("viewer@alphacorp.com", "Alpha Viewer", "local"));
        userTenantRoleRepository.save(new UserTenantRole(viewerUserA, tenantA, customerViewerRole));

        // Customer Admin B (Tenant B only)
        customerUserB = userRepository.save(new User("admin@betacorp.com", "Beta Admin", "local"));
        userTenantRoleRepository.save(new UserTenantRole(customerUserB, tenantB, customerAdminRole));

        // Seed incidents
        incidentA = incidentRepository.save(new Incident(tenantA.getId(), "HIGH", "open", "Alpha Outage Incident"));
        incidentB = incidentRepository.save(new Incident(tenantB.getId(), "CRITICAL", "open", "Beta Outage Incident"));

        // Generate Access Tokens
        staffToken = createAccessToken(staffUser, List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "staff-admin", "basic"),
                new UserPrincipal.TenantAccess(tenantB.getId(), tenantB.getName(), tenantB.getClientInstanceId(), "staff-admin", "pro")
        ));

        customerAToken = createAccessToken(customerUserA, List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "customer-admin", "basic")
        ));

        viewerAToken = createAccessToken(viewerUserA, List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "customer-viewer", "basic")
        ));

        customerBToken = createAccessToken(customerUserB, List.of(
                new UserPrincipal.TenantAccess(tenantB.getId(), tenantB.getName(), tenantB.getClientInstanceId(), "customer-admin", "pro")
        ));
    }

    private String createAccessToken(User user, List<UserPrincipal.TenantAccess> tenants) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getSource(), true, "ACCESS", tenants);
        return jwtTokenService.createAccessToken(principal);
    }

    @Test
    @DisplayName("1. Single-tenant user with no X-Tenant-Id header auto-selects assigned tenant")
    void singleTenantUser_noHeader_autoSelectsTenant() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentA.getId().toString()));
    }

    @Test
    @DisplayName("2. Multi-tenant user with no X-Tenant-Id header receives 400 Bad Request")
    void multiTenantUser_noHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-Tenant-Id header required for multi-tenant users"));
    }

    @Test
    @DisplayName("3. Administrative endpoint path tenantId mismatching X-Tenant-Id header receives 400 Bad Request")
    void adminEndpoint_headerVsPathMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tenants/" + tenantB.getId() + "/entitlement")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Path tenant ID does not match X-Tenant-Id header"));
    }

    @Test
    @DisplayName("4. User requesting unassigned tenant via X-Tenant-Id header receives 403 Forbidden")
    void unassignedTenantHeader_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied to target tenant"));
    }

    @Test
    @DisplayName("5. Cross-tenant incident read/write queries return 404 Not Found to prevent existence leakage")
    void crossTenantIncidentAccess_returnsNotFound() throws Exception {
        // GET incident B using Tenant A context -> 404
        mockMvc.perform(get("/api/v1/incidents/" + incidentB.getId())
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isNotFound());

        // POST acknowledge incident B using Tenant A context -> 404
        mockMvc.perform(post("/api/v1/incidents/" + incidentB.getId() + "/acknowledge")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Unauthenticated requests receive 401 Unauthorized")
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("7. Customer-admin attempting staff-admin endpoint receives 403 Forbidden")
    void roleEscalation_customerAdminCallingStaffAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tenants/" + tenantA.getId() + "/entitlement")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Customer-viewer attempting incident mutation receives 403 Forbidden")
    void readOnlyViewer_incidentMutation_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/" + incidentA.getId() + "/acknowledge")
                        .header("Authorization", "Bearer " + viewerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Basic tier tenant calling Assistant endpoint receives 403 Forbidden with clear message")
    void basicTenant_assistantEndpoint_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Analyze fleet root cause\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This feature requires PRO entitlement tier. Current tier: BASIC."));
    }

    @Test
    @DisplayName("10. Pro tier tenant calling Assistant endpoint succeeds")
    void proTenant_assistantEndpoint_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + customerBToken)
                        .header("X-Tenant-Id", tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Analyze fleet root cause\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.tier").value("PRO"));
    }

    @Test
    @DisplayName("11. Transactional upgrade from Basic -> Pro updates entitlement and creates immutable audit record")
    void transactionalUpgrade_updatesEntitlementAndAudits() throws Exception {
        // Verify initial Basic tier blocks Assistant
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Test Assistant\"}"))
                .andExpect(status().isForbidden());

        // Staff Admin upgrades Tenant A to PRO via admin endpoint
        mockMvc.perform(put("/api/v1/admin/tenants/" + tenantA.getId() + "/entitlement")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tier\":\"pro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"));

        // Subsequent call to Assistant by Tenant A user now succeeds dynamically!
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Test Assistant\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"));

        // Verify immutable audit log record was written
        List<AuthAuditLog> auditLogs = authAuditLogRepository.findAll();
        assertThat(auditLogs).anyMatch(log ->
                "ENTITLEMENT_UPDATE".equals(log.getEventType()) &&
                        tenantA.getId().equals(log.getTenantId()) &&
                        log.getDetail().contains("BASIC to PRO")
        );
    }

    @Test
    @DisplayName("12. Sequential tenant switching on same thread prevents context leakage")
    void sequentialTenantSwitching_sameThread_preventsContextLeak() throws Exception {
        // Request 1: Staff user operates in Tenant A context
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentA.getId().toString()));

        assertTrue(TenantContextHolder.getContext().isEmpty());

        // Request 2: Staff user operates in Tenant B context
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentB.getId().toString()));

        assertTrue(TenantContextHolder.getContext().isEmpty());
    }

    @Test
    @DisplayName("13. Exceptions during request processing still clear ThreadLocal context")
    void exceptionHandling_clearsThreadLocalContext() {
        try {
            mockMvc.perform(get("/api/v1/incidents/invalid-uuid")
                    .header("Authorization", "Bearer " + customerAToken)
                    .header("X-Tenant-Id", tenantA.getId().toString()));
        } catch (Exception ignored) {}

        assertTrue(TenantContextHolder.getContext().isEmpty());
    }
}
