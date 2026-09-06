package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import com.islandpacific.sentinel.entity.TenantDatasource;
import com.islandpacific.sentinel.query.*;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import com.islandpacific.sentinel.repository.TenantDatasourceRepository;
import com.islandpacific.sentinel.security.SecretProtector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class QueryGatewaySecurityHardeningTest {

    @Autowired
    private PromQlAstSanitizer promQlSanitizer;

    @Autowired
    private LogQlAstSanitizer logQlSanitizer;

    @Autowired
    private SsrfProtectionValidator ssrfValidator;

    @Autowired
    private ResponseMerger responseMerger;

    @Autowired
    private QueryAuditService auditService;

    @MockBean
    private AuthAuditLogRepository mockAuditRepository;

    private UUID tenantId;

    @BeforeEach
    public void setUp() {
        tenantId = UUID.randomUUID();
    }

    // 1. PromQL AST Security & Operator Tests
    @Test
    public void testPromQl_EveryMatcherOperator_RejectsClientTenantId() {
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("metric{tenant_id=\"val\"}", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("metric{tenant_id!=\"val\"}", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("metric{tenant_id=~\".*\"}", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("metric{tenant_id!~\".*\"}", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("metric{__sentinel_source=\"spoof\"}", tenantId.toString())
        );
    }

    @Test
    public void testPromQl_NestedSelectors_InFunctionsAndBinaryOps() {
        String query = "histogram_quantile(0.95, sum(rate(http_requests_total[5m])) by (le)) / sum(rate(http_requests_failed[5m])) by (le)";
        String sanitized = promQlSanitizer.sanitize(query, tenantId.toString());

        assertTrue(sanitized.contains("http_requests_total{tenant_id=\"" + tenantId + "\"}"));
        assertTrue(sanitized.contains("http_requests_failed{tenant_id=\"" + tenantId + "\"}"));
        assertTrue(sanitized.startsWith("histogram_quantile"));
    }

    @Test
    public void testPromQl_SubqueriesOffsetsAndAtModifiers() {
        String query = "rate(http_requests_total{job=\"web\"}[5m:30s]) offset 10m @ 1600000000";
        String sanitized = promQlSanitizer.sanitize(query, tenantId.toString());

        assertTrue(sanitized.contains("http_requests_total{job=\"web\",tenant_id=\"" + tenantId + "\"}"));
        assertTrue(sanitized.contains("offset 10m @ 1600000000"));
    }

    @Test
    public void testPromQl_MalformedQueries_RejectsFailClosed() {
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{job=\"web\"", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("sum(rate(http_requests_total[5m])", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                promQlSanitizer.sanitize("http_requests_total{job=\"web'", tenantId.toString())
        );
    }

    // 2. LogQL AST Security & Pipeline Tests
    @Test
    public void testLogQl_MultipleStreamSelectors_InjectsTenantIdIntoAll() {
        String query = "{app=\"web\"} | json | {env=\"prod\"}";
        String sanitized = logQlSanitizer.sanitize(query, tenantId.toString());

        assertTrue(sanitized.contains("{app=\"web\",tenant_id=\"" + tenantId + "\"}"));
        assertTrue(sanitized.contains("{env=\"prod\",tenant_id=\"" + tenantId + "\"}"));
    }

    @Test
    public void testLogQl_StreamSelectorsInsideMetricQueries() {
        String query = "rate({app=\"api\", job=\"web\"}[5m])";
        String sanitized = logQlSanitizer.sanitize(query, tenantId.toString());

        assertTrue(sanitized.contains("{app=\"api\", job=\"web\",tenant_id=\"" + tenantId + "\"}"));
    }

    @Test
    public void testLogQl_PipelineExpressionsWithTenantLikeText() {
        String query = "{app=\"api\"} |= \"tenant_id=fake\"";
        String sanitized = logQlSanitizer.sanitize(query, tenantId.toString());

        assertTrue(sanitized.contains("{app=\"api\",tenant_id=\"" + tenantId + "\"}"));
        assertTrue(sanitized.contains("|= \"tenant_id=fake\""));
    }

    @Test
    public void testLogQl_MalformedQueries_RejectsFailClosed() {
        assertThrows(InvalidTenantQueryException.class, () ->
                logQlSanitizer.sanitize("{app=\"api\"", tenantId.toString())
        );
        assertThrows(InvalidTenantQueryException.class, () ->
                logQlSanitizer.sanitize("rate({app=\"api\"[5m])", tenantId.toString())
        );
    }

    // 3. SSRF Security Tests
    @Test
    public void testSsrf_PrivateIPv4AndLoopback() {
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://127.0.0.1:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://10.0.0.1:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://172.16.0.1:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://192.168.1.1:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://169.254.169.254/latest", "prometheus"));
    }

    @Test
    public void testSsrf_IPv4MappedIPv6() {
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://[::ffff:127.0.0.1]:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://[::ffff:10.0.0.1]:9090", "prometheus"));
    }

    @Test
    public void testSsrf_HexAndDecimalIpRepresentations() {
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://0x7f000001:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://2130706433:9090", "prometheus"));
    }

    @Test
    public void testSsrf_LocalhostAliasesAndEmbeddedCredentials() {
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://localhost:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://localhost.localdomain:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> ssrfValidator.validateUrl("http://user:password@prometheus.io", "prometheus"));
    }

    // 4. Response Merging & Spoofing Security Tests
    @Test
    public void testResponseMerger_StripsUpstreamSpoofedSourceLabel() {
        String upstreamSpoofedJson = """
                {
                    "status": "success",
                    "data": {
                        "resultType": "vector",
                        "result": [
                            {
                                "metric": {
                                    "__name__": "http_requests_total",
                                    "job": "api",
                                    "__sentinel_source": "spoofed-fake-source"
                                },
                                "value": [1600000000, "100"]
                            }
                        ]
                    }
                }
                """;

        DatasourceResolverService.ResolvedDatasource ds = new DatasourceResolverService.ResolvedDatasource(
                UUID.randomUUID(), tenantId, "authentic-source-a", "prometheus", "https://prometheus.io", "none", null
        );
        QueryFanoutExecutor.BackendResponse resp = new QueryFanoutExecutor.BackendResponse(200, upstreamSpoofedJson, null, 15, true);

        Map<DatasourceResolverService.ResolvedDatasource, QueryFanoutExecutor.BackendResponse> map = new HashMap<>();
        map.put(ds, resp);

        ResponseMerger.MergedResult merged = responseMerger.mergeResponses(map);
        assertEquals(200, merged.getHttpStatusCode());
        assertTrue(merged.getJsonResponse().contains("\"__sentinel_source\":\"authentic-source-a\""));
        assertFalse(merged.getJsonResponse().contains("spoofed-fake-source"));
    }

    // 5. Audit Security & Fail-Closed Tests
    @Test
    public void testAuditService_FailsClosedOnAuditStorageFailure() {
        doThrow(new RuntimeException("Database connection error"))
                .when(mockAuditRepository).save(any(AuthAuditLog.class));

        assertThrows(QueryAuditException.class, () ->
                auditService.recordQueryExecution(
                        UUID.randomUUID(),
                        tenantId,
                        "PROMQL_INSTANT",
                        "up",
                        "up{tenant_id=\"xyz\"}",
                        List.of("ds-1"),
                        10L,
                        "SUCCESS",
                        false,
                        0,
                        "corr-123"
                )
        );
    }
}
