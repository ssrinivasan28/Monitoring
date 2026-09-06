package com.islandpacific.sentinel;

import com.islandpacific.sentinel.query.InvalidTenantQueryException;
import com.islandpacific.sentinel.query.PromQlAstSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromQlSecurityTest {

    private PromQlAstSanitizer sanitizer;
    private final String tenantId = "tenant-prod-123";

    @BeforeEach
    void setUp() {
        sanitizer = new PromQlAstSanitizer();
    }

    @Test
    void testSelectorWithExistingLabels() {
        String result = sanitizer.sanitize("{job=\"x\"}", tenantId);
        assertEquals("{job=\"x\",tenant_id=\"tenant-prod-123\"}", result);
    }

    @Test
    void testBareMetricName() {
        String result = sanitizer.sanitize("http_requests_total", tenantId);
        assertEquals("http_requests_total{tenant_id=\"tenant-prod-123\"}", result);
    }

    @Test
    void testMetricWithExistingSelector() {
        String result = sanitizer.sanitize("http_requests_total{job=\"api\"}", tenantId);
        assertEquals("http_requests_total{job=\"api\",tenant_id=\"tenant-prod-123\"}", result);
    }

    @Test
    void testRateFunctionWithRangeVector() {
        String result = sanitizer.sanitize("rate(http_requests_total[5m])", tenantId);
        assertEquals("rate(http_requests_total{tenant_id=\"tenant-prod-123\"}[5m])", result);
    }

    @Test
    void testSubqueryExpression() {
        String result = sanitizer.sanitize("http_requests_total[5m:30s]", tenantId);
        assertEquals("http_requests_total{tenant_id=\"tenant-prod-123\"}[5m:30s]", result);
    }

    @Test
    void testAggregationWithByClause() {
        String result = sanitizer.sanitize("sum by (job) (http_requests_total)", tenantId);
        assertEquals("sum by (job) (http_requests_total{tenant_id=\"tenant-prod-123\"})", result);
    }

    @Test
    void testHistogramQuantileFunction() {
        String result = sanitizer.sanitize("histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))", tenantId);
        assertEquals("histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{tenant_id=\"tenant-prod-123\"}[5m])) by (le))", result);
    }

    @Test
    void testBinaryOperators() {
        String result = sanitizer.sanitize("metric_one + metric_two", tenantId);
        assertEquals("metric_one{tenant_id=\"tenant-prod-123\"} + metric_two{tenant_id=\"tenant-prod-123\"}", result);
    }

    @Test
    void testOffsetModifier() {
        String result = sanitizer.sanitize("http_requests_total offset 5m", tenantId);
        assertEquals("http_requests_total{tenant_id=\"tenant-prod-123\"} offset 5m", result);
    }

    @Test
    void testAtModifier() {
        String result = sanitizer.sanitize("http_requests_total @ 1600000000", tenantId);
        assertEquals("http_requests_total{tenant_id=\"tenant-prod-123\"} @ 1600000000", result);
    }

    @Test
    void testNestedFunctionsAndMultipleSelectors() {
        String result = sanitizer.sanitize("abs(rate(cpu_usage{env=\"prod\"}[1m])) > bool (node_memory{type=\"swap\"} / 100)", tenantId);
        assertEquals("abs(rate(cpu_usage{env=\"prod\",tenant_id=\"tenant-prod-123\"}[1m])) > bool (node_memory{type=\"swap\",tenant_id=\"tenant-prod-123\"} / 100)", result);
    }

    // ------------------- SECURITY REJECTION TESTS -------------------

    @Test
    void testRejectClientEqualsTenantMatcher() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{tenant_id=\"attacker\"}", tenantId));
    }

    @Test
    void testRejectClientNotEqualsTenantMatcher() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{tenant_id!=\"current\"}", tenantId));
    }

    @Test
    void testRejectClientRegexMatchTenantMatcher() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{tenant_id=~\".*\"}", tenantId));
    }

    @Test
    void testRejectClientRegexNotMatchTenantMatcher() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{tenant_id!~\"current\"}", tenantId));
    }

    @Test
    void testRejectClientTenantMatcherWithSpaces() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{ tenant_id = \"attacker\" }", tenantId));
    }

    @Test
    void testRejectClientEqualsSentinelSource() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{__sentinel_source=\"fake\"}", tenantId));
    }

    @Test
    void testRejectClientNotEqualsSentinelSource() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{__sentinel_source!=\"other\"}", tenantId));
    }

    @Test
    void testRejectClientRegexSentinelSource() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{__sentinel_source=~\".*\"}", tenantId));
    }

    @Test
    void testRejectMalformedUnbalancedBraces() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("http_requests_total{job=\"api\"", tenantId));
    }

    @Test
    void testRejectMalformedUnclosedStringLiteral() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("http_requests_total{job=\"api}", tenantId));
    }
}
