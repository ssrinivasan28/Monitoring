package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import com.islandpacific.sentinel.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 1.3 incident REST API: ranking, pagination, filtering, detail enrichment (signals/root-cause
 * evidence/runbook/similar-incidents/timeline), and tenant isolation across a multi-incident,
 * multi-tenant dataset. Complements RbacAndTenantIsolationIntegrationTest, which covers the
 * single-incident-per-tenant baseline isolation checks on the same endpoints.
 */
@AutoConfigureMockMvc
class IncidentApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTenantRoleRepository userTenantRoleRepository;
    @Autowired private EntitlementRepository entitlementRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentSignalRepository incidentSignalRepository;
    @Autowired private IncidentTimelineRepository incidentTimelineRepository;
    @Autowired private AuthAuditLogRepository authAuditLogRepository;
    @Autowired private JwtTokenService jwtTokenService;

    private Tenant tenantA;
    private Tenant tenantB;

    private Incident aCriticalNew;
    private Incident aCriticalOld;
    private Incident aHighWindows;
    private Incident aRootCaused;
    private Incident bIncident;

    private String staffToken;
    private String customerAToken;

    @BeforeEach
    void setupTestData() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("TRUNCATE TABLE user_tokens, auth_audit_log, user_tenant_roles, " +
                    "incident_timeline, incident_signals, correlations, incidents, entitlements, users, roles, tenants CASCADE");
        }

        tenantA = tenantRepository.save(new Tenant("Tenant Alpha", "CLIENT-ALPHA"));
        tenantB = tenantRepository.save(new Tenant("Tenant Beta", "CLIENT-BETA"));

        entitlementRepository.save(new Entitlement(tenantA.getId(), "basic"));
        entitlementRepository.save(new Entitlement(tenantB.getId(), "pro"));

        Role staffAdminRole = roleRepository.save(new Role("staff-admin"));
        Role customerAdminRole = roleRepository.save(new Role("customer-admin"));

        User staffUser = userRepository.save(new User("staff.admin@islandpacific.com", "Staff Admin", "azure_ad"));
        userTenantRoleRepository.save(new UserTenantRole(staffUser, tenantA, staffAdminRole));
        userTenantRoleRepository.save(new UserTenantRole(staffUser, tenantB, staffAdminRole));

        User customerUserA = userRepository.save(new User("admin@alphacorp.com", "Alpha Admin", "local"));
        userTenantRoleRepository.save(new UserTenantRole(customerUserA, tenantA, customerAdminRole));

        Instant now = Instant.now();

        aCriticalNew = incidentRepository.save(withOpenedAt(
                new Incident(tenantA.getId(), "critical", "open", "Alpha: new critical"), now.minus(1, ChronoUnit.HOURS)));
        aCriticalOld = incidentRepository.save(withOpenedAt(
                new Incident(tenantA.getId(), "critical", "open", "Alpha: old critical"), now.minus(5, ChronoUnit.HOURS)));
        aHighWindows = incidentRepository.save(withOpenedAt(
                new Incident(tenantA.getId(), "high", "resolved", "Alpha: high on windows"), now.minus(2, ChronoUnit.HOURS)));
        aRootCaused = incidentRepository.save(withOpenedAt(
                new Incident(tenantA.getId(), "high", "open", "Alpha: root-caused disk pressure"), now.minus(3, ChronoUnit.HOURS)));

        bIncident = incidentRepository.save(withOpenedAt(
                new Incident(tenantB.getId(), "critical", "open", "Beta: critical"), now));

        incidentSignalRepository.save(new IncidentSignal(aHighWindows.getId(), tenantA.getId(), "windows"));
        incidentSignalRepository.save(new IncidentSignal(aRootCaused.getId(), tenantA.getId(), "ibmi"));
        incidentSignalRepository.save(new IncidentSignal(aCriticalNew.getId(), tenantA.getId(), "ibmi"));

        aRootCaused.setRootCauseJson("{"
                + "\"root_cause_hypothesis\":\"IFS disk pressure from orphaned temp files\","
                + "\"evidence\":["
                + "{\"source\":\"promql_query\",\"query\":\"ibmi_asp_utilization_percent\",\"snippet\":\"96%\"},"
                + "{\"source\":\"kb_search\",\"query\":\"disk cleanup runbook\",\"snippet\":\"Runbook: clear /tmp on IFS\"}"
                + "],"
                + "\"severity\":\"high\",\"suggested_checks\":[\"check ASP usage\"],\"confidence\":0.9}");
        incidentRepository.save(aRootCaused);

        IncidentTimeline opened = new IncidentTimeline(
                aRootCaused.getId(), tenantA.getId(), "correlation-engine", "opened", "initial breach");
        opened.setAt(now.minus(3, ChronoUnit.HOURS));
        incidentTimelineRepository.save(opened);

        IncidentTimeline rootCauseSuggested = new IncidentTimeline(
                aRootCaused.getId(), tenantA.getId(), "triage-agent", "root_cause_suggested", "see root cause");
        rootCauseSuggested.setAt(now.minus(2, ChronoUnit.HOURS));
        incidentTimelineRepository.save(rootCauseSuggested);

        staffToken = createAccessToken(staffUser, List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "staff-admin", "basic"),
                new UserPrincipal.TenantAccess(tenantB.getId(), tenantB.getName(), tenantB.getClientInstanceId(), "staff-admin", "pro")
        ));
        customerAToken = createAccessToken(customerUserA, List.of(
                new UserPrincipal.TenantAccess(tenantA.getId(), tenantA.getName(), tenantA.getClientInstanceId(), "customer-admin", "basic")
        ));
    }

    private static Incident withOpenedAt(Incident incident, Instant openedAt) {
        incident.setOpenedAt(openedAt);
        return incident;
    }

    private String createAccessToken(User user, List<UserPrincipal.TenantAccess> tenants) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getSource(), true, "ACCESS", tenants);
        return jwtTokenService.createAccessToken(principal);
    }

    @Test
    @DisplayName("List ranks by severity (critical > high) then recency, and only ever returns the caller's tenant")
    void listIncidents_ranksBySeverityThenRecency_scopedToCallerTenant() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(aCriticalNew.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(aCriticalOld.getId().toString()))
                .andExpect(header().string("X-Total-Count", "4"))
                .andExpect(header().exists("X-Total-Pages"));

        // No tenant B incident ever appears for a tenant-A-only caller.
        String body = mockMvc.perform(get("/api/v1/incidents").header("Authorization", "Bearer " + customerAToken))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(bIncident.getId().toString());
    }

    @Test
    @DisplayName("List filters by status")
    void listIncidents_filtersByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("status", "resolved")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(aHighWindows.getId().toString()));
    }

    @Test
    @DisplayName("List filters by platform via joined signals")
    void listIncidents_filtersByPlatform() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("platform", "windows")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(aHighWindows.getId().toString()));
    }

    @Test
    @DisplayName("List paginates and reports total count via headers")
    void listIncidents_paginates() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(header().string("X-Total-Count", "4"))
                .andExpect(header().string("X-Page-Size", "2"))
                .andExpect(header().string("X-Total-Pages", "2"));
    }

    @Test
    @DisplayName("Staff can span tenants via X-Tenant-Id while a customer stays confined to their own")
    void staffSpansTenants_customerConfinedToOwnTenant() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + staffToken)
                        .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(bIncident.getId().toString()));

        // Customer A has no tenant B access at all, even by asking explicitly.
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + customerAToken)
                        .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Detail returns signals, root cause with cited evidence, runbook references, similar incidents, and timeline")
    void getIncidentDetail_returnsFullyEnrichedShape() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + aRootCaused.getId())
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aRootCaused.getId().toString()))
                .andExpect(jsonPath("$.signals", hasSize(1)))
                .andExpect(jsonPath("$.signals[0].platform").value("ibmi"))
                .andExpect(jsonPath("$.timeline", hasSize(2)))
                .andExpect(jsonPath("$.timeline[0].eventType").value("opened"))
                .andExpect(jsonPath("$.rootCause.root_cause_hypothesis").value("IFS disk pressure from orphaned temp files"))
                .andExpect(jsonPath("$.rootCause.evidence", hasSize(2)))
                .andExpect(jsonPath("$.runbookReferences", hasSize(1)))
                .andExpect(jsonPath("$.runbookReferences[0].source").value("kb_search"))
                // aHighWindows shares severity (+2) with aRootCaused; aCriticalNew only shares the
                // ibmi platform (+1); aCriticalOld shares neither and is excluded (score 0).
                .andExpect(jsonPath("$.similarIncidents", hasSize(2)))
                .andExpect(jsonPath("$.similarIncidents[0].id").value(aHighWindows.getId().toString()))
                .andExpect(jsonPath("$.similarIncidents[1].id").value(aCriticalNew.getId().toString()));
    }

    @Test
    @DisplayName("Detail degrades gracefully with no root cause yet (AI-disabled / not-yet-triaged)")
    void getIncidentDetail_noRootCauseYet_stillReturnsRestOfDetail() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + aCriticalNew.getId())
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCause").doesNotExist())
                .andExpect(jsonPath("$.runbookReferences", hasSize(0)))
                .andExpect(jsonPath("$.signals", hasSize(1)));
    }

    @Test
    @DisplayName("List and detail calls are audited")
    void incidentEndpoints_areAudited() throws Exception {
        mockMvc.perform(get("/api/v1/incidents").header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/incidents/" + aRootCaused.getId()).header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk());

        List<AuthAuditLog> logs = authAuditLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "QUERY_EXECUTED".equals(l.getEventType()) && l.getDetail().contains("INCIDENT_LIST"));
        assertThat(logs).anyMatch(l -> "QUERY_EXECUTED".equals(l.getEventType()) && l.getDetail().contains("INCIDENT_DETAIL"));
    }
}
