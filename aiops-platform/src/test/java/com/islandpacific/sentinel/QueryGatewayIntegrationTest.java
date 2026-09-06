package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.Tenant;
import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.entity.User;
import com.islandpacific.sentinel.query.*;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.repository.TenantRepository;
import com.islandpacific.sentinel.repository.UserRepository;
import com.islandpacific.sentinel.security.JwtTokenService;
import com.islandpacific.sentinel.security.UserPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class QueryGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantDatasourceRepository datasourceRepository;

    @Autowired
    private AuthAuditLogRepository auditLogRepository;

    @Autowired
    private PromQlAstSanitizer promQlSanitizer;

    @Autowired
    private LogQlAstSanitizer logQlSanitizer;

    @Autowired
    private SsrfProtectionValidator ssrfValidator;

    @Autowired
    private com.islandpacific.sentinel.repository.RoleRepository roleRepository;

    @Autowired
    private com.islandpacific.sentinel.repository.UserTenantRoleRepository userTenantRoleRepository;

    @Autowired
    private com.islandpacific.sentinel.security.UserAuthCache userAuthCache;

    private UUID tenant1Id;
    private UUID tenant2Id;

    private String user1Token;
    private String admin1Token;
    private String superAdminToken;

    @BeforeEach
    public void setUp() {
        datasourceRepository.deleteAll();
        auditLogRepository.deleteAll();

        Tenant t1 = tenantRepository.save(new Tenant("Tenant One", "BASIC"));
        tenant1Id = t1.getId();

        Tenant t2 = tenantRepository.save(new Tenant("Tenant Two", "PRO"));
        tenant2Id = t2.getId();

        com.islandpacific.sentinel.entity.Role rViewer = roleRepository.save(new com.islandpacific.sentinel.entity.Role("customer-viewer"));
        com.islandpacific.sentinel.entity.Role rAdmin = roleRepository.save(new com.islandpacific.sentinel.entity.Role("tenant-admin"));
        com.islandpacific.sentinel.entity.Role rSuper = roleRepository.save(new com.islandpacific.sentinel.entity.Role("super-admin"));

        User user1 = userRepository.save(new User("user1@tenant1.com", "User One", "local"));
        User admin1 = userRepository.save(new User("admin1@tenant1.com", "Admin One", "local"));
        User superAdmin = userRepository.save(new User("super@islandpacific.com", "Super Admin", "azure_ad"));

        userTenantRoleRepository.save(new com.islandpacific.sentinel.entity.UserTenantRole(user1, t1, rViewer));
        userTenantRoleRepository.save(new com.islandpacific.sentinel.entity.UserTenantRole(admin1, t1, rAdmin));
        userTenantRoleRepository.save(new com.islandpacific.sentinel.entity.UserTenantRole(superAdmin, t1, rSuper));

        user1Token = "Bearer " + createToken(user1, tenant1Id, t1.getName(), t1.getClientInstanceId(), "customer-viewer", "basic");
        admin1Token = "Bearer " + createToken(admin1, tenant1Id, t1.getName(), t1.getClientInstanceId(), "tenant-admin", "basic");
        superAdminToken = "Bearer " + createToken(superAdmin, tenant1Id, t1.getName(), t1.getClientInstanceId(), "super-admin", "pro");
    }

    private String createToken(User user, UUID tenantId, String tenantName, String clientInstanceId, String role, String tier) {
        UserPrincipal.TenantAccess access = new UserPrincipal.TenantAccess(tenantId, tenantName, clientInstanceId, role, tier);
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), user.getSource(), true, "ACCESS", java.util.List.of(access));
        return jwtTokenService.createAccessToken(principal);
    }

    // 1. PromQL AST Lexer & Tenant Injection Tests
    @Test
    public void testPromQlAstSanitizer_InjectsAuthoritativeTenant() {
        String query = "http_requests_total{job=\"api\"}";
        String sanitized = promQlSanitizer.sanitize(query, tenant1Id.toString());
        assertTrue(sanitized.contains("tenant_id=\"" + tenant1Id + "\""));
        assertTrue(sanitized.contains("job=\"api\""));
    }

    @Test
    public void testPromQlAstSanitizer_MultipleSelectorsInExpression() {
        String query = "http_requests_total{job=\"api\"} / http_requests_failed_total";
        String sanitized = promQlSanitizer.sanitize(query, tenant1Id.toString());
        assertTrue(sanitized.contains("http_requests_total{job=\"api\",tenant_id=\"" + tenant1Id + "\"}"));
        assertTrue(sanitized.contains("http_requests_failed_total{tenant_id=\"" + tenant1Id + "\"}"));
    }

    @Test
    public void testPromQlAstSanitizer_RejectsClientTenantIdMatchers() {
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{tenant_id=\"other\"}", tenant1Id.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{tenant_id!=\"other\"}", tenant1Id.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{tenant_id=~\".*\"}", tenant1Id.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{tenant_id!~\".*\"}", tenant1Id.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{__sentinel_source=\"custom\"}", tenant1Id.toString())
        );
    }

    @Test
    public void testPromQlAstSanitizer_FailsClosedOnMalformedSyntax() {
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{job=\"api\"", tenant1Id.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("rate(http_requests_total[5m]", tenant1Id.toString())
        );
    }

    // 2. LogQL AST Lexer & Tenant Injection Tests
    @Test
    public void testLogQlAstSanitizer_InjectsAuthoritativeTenant() {
        String query = "{app=\"frontend\"} |= \"error tenant_id=ignore\"";
        String sanitized = logQlSanitizer.sanitize(query, tenant1Id.toString());
        assertTrue(sanitized.contains("{app=\"frontend\",tenant_id=\"" + tenant1Id + "\"}"));
        assertTrue(sanitized.contains("|= \"error tenant_id=ignore\""));
    }

    @Test
    public void testLogQlAstSanitizer_RejectsClientTenantIdMatchers() {
        assertThrows(InvalidTenantQueryException.class, () ->
                logQlSanitizer.sanitize("{app=\"frontend\",tenant_id=\"other\"}", tenant1Id.toString())
        );
    }

    // 3. SSRF Protection Tests
    @Test
    public void testSsrfProtection_BlocksProhibitedUrls() {
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("http://localhost:8080/metrics", "prometheus")
        );
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("http://127.0.0.1:9090", "prometheus")
        );
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("http://169.254.169.254/latest/meta-data/", "prometheus")
        );
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("http://10.0.0.1:9090", "prometheus")
        );
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("http://192.168.1.1:9090", "prometheus")
        );
        assertThrows(SecurityException.class, () ->
                ssrfValidator.validateUrl("ftp://example.com/metrics", "prometheus")
        );
    }

    // 4. Datasource Admin API & RBAC Tests
    @Test
    public void testDatasourceAdmin_UserRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/datasources")
                        .header("Authorization", user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDatasourceAdmin_TenantAdminCreateAndMasking() throws Exception {
        String createJson = String.format("""
                {
                    "tenantId": "%s",
                    "name": "ds-1",
                    "kind": "prometheus",
                    "url": "https://prometheus.io",
                    "authType": "token",
                    "credentialsRef": "my-secret-token",
                    "enabled": true
                }
                """, tenant1Id);

        mockMvc.perform(post("/api/v1/admin/datasources")
                        .header("Authorization", admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("ds-1")))
                .andExpect(jsonPath("$.credentialsRefMasked", is("********")));
    }

    @Test
    public void testDatasourceAdmin_CrossTenantCreationForbidden() throws Exception {
        String createJson = String.format("""
                {
                    "tenantId": "%s",
                    "name": "ds-2",
                    "kind": "prometheus",
                    "url": "https://prometheus.example.com",
                    "enabled": true
                }
                """, tenant2Id);

        mockMvc.perform(post("/api/v1/admin/datasources")
                        .header("Authorization", admin1Token) // admin1 belongs to tenant1
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDatasourceAdmin_TestConnectionSsrfBlocked() throws Exception {
        String testJson = """
                {
                    "name": "test",
                    "kind": "prometheus",
                    "url": "http://169.254.169.254/metadata"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/datasources/test-connection")
                        .header("Authorization", admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UNREACHABLE")))
                .andExpect(jsonPath("$.message", containsString("SSRF Blocked")));

        // Verify test connection does NOT persist to database
        assertEquals(0, datasourceRepository.count());
    }

    // 5. Query Gateway REST API & Audit Verification Tests
    @Test
    public void testQueryGateway_RejectsCrossTenantAttempt() throws Exception {
        mockMvc.perform(get("/api/v1/query/promql")
                        .header("Authorization", user1Token)
                        .param("query", "up{tenant_id=\"other-tenant\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Reserved label 'tenant_id'")));
    }

    @Test
    public void testQueryGateway_CentralFallbackAuditRecorded() throws Exception {
        // Tenant 1 has 0 datasources registered -> Fallback to central
        mockMvc.perform(get("/api/v1/query/promql")
                        .header("Authorization", user1Token)
                        .header("X-Correlation-ID", "test-corr-123")
                        .param("query", "up"))
                .andExpect(status().is(anyOf(is(200), is(502))))
                .andExpect(header().string("X-Correlation-ID", "test-corr-123"));

        // Verify service-layer audit log was produced
        assertTrue(auditLogRepository.count() >= 1);
        var logs = auditLogRepository.findAll();
        var lastLog = logs.get(logs.size() - 1);
        assertEquals("QUERY_EXECUTED", lastLog.getEventType());
        assertEquals(tenant1Id, lastLog.getTenantId());
        assertTrue(lastLog.getDetail().contains("test-corr-123"));
        assertTrue(lastLog.getDetail().contains("central-prometheus"));
    }
}
