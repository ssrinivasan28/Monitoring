package com.islandpacific.sentinel;

import com.islandpacific.sentinel.query.InvalidTenantQueryException;
import com.islandpacific.sentinel.query.LogQlAstSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogQlSecurityTest {

    private LogQlAstSanitizer sanitizer;
    private final String tenantId = "tenant-loki-789";

    @BeforeEach
    void setUp() {
        sanitizer = new LogQlAstSanitizer();
    }

    @Test
    void testSingleStreamSelector() {
        String result = sanitizer.sanitize("{app=\"gateway\"}", tenantId);
        assertEquals("{app=\"gateway\",tenant_id=\"tenant-loki-789\"}", result);
    }

    @Test
    void testMultipleStreamSelectors() {
        String result = sanitizer.sanitize("{app=\"foo\"} |= \"err\" or {app=\"bar\"}", tenantId);
        assertEquals("{app=\"foo\",tenant_id=\"tenant-loki-789\"} |= \"err\" or {app=\"bar\",tenant_id=\"tenant-loki-789\"}", result);
    }

    @Test
    void testNestedLogPipelineExpressions() {
        String result = sanitizer.sanitize("{container=\"nginx\"} | json | line_format \"{{.msg}}\" | status >= 500", tenantId);
        assertEquals("{container=\"nginx\",tenant_id=\"tenant-loki-789\"} | json | line_format \"{{.msg}}\" | status >= 500", result);
    }

    @Test
    void testEmptyStreamSelector() {
        String result = sanitizer.sanitize("{}", tenantId);
        assertEquals("{tenant_id=\"tenant-loki-789\"}", result);
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
    void testRejectQueryWithoutStreamSelector() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("just a raw string without selector", tenantId));
    }

    @Test
    void testRejectMalformedUnbalancedBraces() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{app=\"loki\"", tenantId));
    }

    @Test
    void testRejectMalformedUnterminatedBacktick() {
        assertThrows(InvalidTenantQueryException.class, () -> sanitizer.sanitize("{app=\"loki\"} | `unclosed backtick", tenantId));
    }
}
