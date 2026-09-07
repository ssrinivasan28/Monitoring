package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.*;
import com.islandpacific.sentinel.repository.*;
import com.islandpacific.sentinel.security.JwtTokenService;
import com.islandpacific.sentinel.security.UserAuthCache;
import com.islandpacific.sentinel.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AuditQueryControllerTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserTenantRoleRepository userTenantRoleRepository;
    @Autowired private AgentRunRepository agentRunRepository;
    @Autowired private CostLedgerRepository costLedgerRepository;
    @Autowired private JwtTokenService jwtTokenService;

    @Autowired private UserAuthCache userAuthCache;

    private Tenant tenant;
    private String staffAdminToken;
    private String customerAdminToken;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(new Tenant("Audit Query Tenant", "client-audit-query-01"));

        Role staffAdminRole = roleRepository.findByKey("staff-admin")
                .orElseGet(() -> roleRepository.save(new Role("staff-admin")));
        Role customerAdminRole = roleRepository.findByKey("customer-admin")
                .orElseGet(() -> roleRepository.save(new Role("customer-admin")));


        User staffUser = userRepository.save(new User("staff.audit@islandpacific.com", "Staff Auditor", "azure_ad"));
        userTenantRoleRepository.save(new UserTenantRole(staffUser, tenant, staffAdminRole));
        userAuthCache.invalidate(staffUser.getId());

        User customerUser = userRepository.save(new User("customer.admin@client.com", "Customer Admin", "local"));
        userTenantRoleRepository.save(new UserTenantRole(customerUser, tenant, customerAdminRole));
        userAuthCache.invalidate(customerUser.getId());

        UserPrincipal staffPrincipal = new UserPrincipal(
                staffUser.getId(), staffUser.getEmail(), staffUser.getDisplayName(), "azure_ad", true, "ACCESS",
                List.of(new UserPrincipal.TenantAccess(tenant.getId(), tenant.getName(), tenant.getClientInstanceId(), "staff-admin", "pro"))
        );

        UserPrincipal customerPrincipal = new UserPrincipal(
                customerUser.getId(), customerUser.getEmail(), customerUser.getDisplayName(), "local", true, "ACCESS",
                List.of(new UserPrincipal.TenantAccess(tenant.getId(), tenant.getName(), tenant.getClientInstanceId(), "customer-admin", "basic"))
        );

        staffAdminToken = jwtTokenService.createAccessToken(staffPrincipal);
        customerAdminToken = jwtTokenService.createAccessToken(customerPrincipal);

        AgentRun run = agentRunRepository.save(new AgentRun(tenant.getId(), "triage_agent", "[System] prompt redacted", "claude-3-5-sonnet", 50, 100, BigDecimal.valueOf(0.002)));
        costLedgerRepository.save(new CostLedger(tenant.getId(), run.getId(), 150, BigDecimal.valueOf(0.002)));
    }


    @Test
    void staffAdminCanQueryAgentRuns() throws Exception {
        mockMvc.perform(get("/api/v1/audit/agent-runs")
                .header("Authorization", "Bearer " + staffAdminToken)
                .header("X-Tenant-Id", tenant.getId().toString())
                .param("tenantId", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agent").value("triage_agent"))
                .andExpect(jsonPath("$[0].model").value("claude-3-5-sonnet"));
    }

    @Test
    void customerAdminIsForbiddenFromAuditQueryApi() throws Exception {
        mockMvc.perform(get("/api/v1/audit/agent-runs")
                .header("Authorization", "Bearer " + customerAdminToken)
                .header("X-Tenant-Id", tenant.getId().toString())
                .param("tenantId", tenant.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanFetchCostSummary() throws Exception {
        mockMvc.perform(get("/api/v1/audit/cost-summary")
                .header("Authorization", "Bearer " + staffAdminToken)
                .header("X-Tenant-Id", tenant.getId().toString())
                .param("tenantId", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTokens").value(150))
                .andExpect(jsonPath("$.totalEntries").value(1));
    }
}
