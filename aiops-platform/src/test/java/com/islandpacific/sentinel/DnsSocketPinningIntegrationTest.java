package com.islandpacific.sentinel;

import com.islandpacific.sentinel.query.SsrfProtectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class DnsSocketPinningIntegrationTest {

    private SsrfProtectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfProtectionValidator();
    }

    @Test
    void testValidateAndPinUrlReturnsOriginalAuthorityAndValidatedAddress() {
        var endpoint = validator.validateAndPinUrl("https://prometheus.io:443/api/v1/query", "prometheus");

        assertNotNull(endpoint);
        assertEquals("prometheus.io", endpoint.getHostname());
        assertEquals(443, endpoint.getPort());
        assertEquals("https", endpoint.getScheme());
        assertNotNull(endpoint.getValidatedAddress());
        assertFalse(endpoint.getValidatedAddress().isLoopbackAddress());
        assertFalse(endpoint.getValidatedAddress().isSiteLocalAddress());
    }

    @Test
    void testRebindingResistancePreservesInitialValidatedAddress() throws Exception {
        // Initial DNS resolution
        InetAddress initialAddress = InetAddress.getByName("prometheus.io");
        var endpoint = validator.validateAndPinUrl("https://prometheus.io:443", "prometheus");

        // Assert that the pinned address matches the initial validated IP
        assertEquals(initialAddress, endpoint.getValidatedAddress());
        assertEquals("prometheus.io", endpoint.getHostname());
    }
}
