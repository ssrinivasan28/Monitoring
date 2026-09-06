package com.islandpacific.sentinel;

import com.islandpacific.sentinel.query.SsrfProtectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SsrfSecurityTest {

    private SsrfProtectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfProtectionValidator();
    }

    @Test
    void testRejectPrivateIpV4TenNetwork() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://10.0.0.1:9090", "prometheus"));
    }

    @Test
    void testRejectPrivateIpV4172Network() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://172.16.0.1:9090", "prometheus"));
    }

    @Test
    void testRejectPrivateIpV4192Network() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://192.168.1.1:9090", "prometheus"));
    }

    @Test
    void testRejectPrivateUniqueLocalIpV6() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://[fd00::1]:9090", "prometheus"));
    }

    @Test
    void testRejectIpV4MappedIpV6Loopback() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://[::ffff:127.0.0.1]:9090", "prometheus"));
    }

    @Test
    void testRejectIpV4MappedIpV6Private() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://[::ffff:10.0.0.1]:9090", "prometheus"));
    }

    @Test
    void testRejectLoopbackIpV4() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://127.0.0.1:9090", "prometheus"));
    }

    @Test
    void testRejectLoopbackIpV6() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://[::1]:9090", "prometheus"));
    }

    @Test
    void testRejectLinkLocalCloudMetadata() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://169.254.169.254/latest/meta-data/", "prometheus"));
    }

    @Test
    void testRejectDecimalIntegerIp() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://2130706433:9090", "prometheus"));
    }

    @Test
    void testRejectHexadecimalIp() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://0x7f000001:9090", "prometheus"));
    }

    @Test
    void testRejectOctalIp() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://0177.0.0.1:9090", "prometheus"));
    }

    @Test
    void testRejectLocalhostDomainAlias() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://localhost:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://sub.localhost:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://localhost.localdomain:9090", "prometheus"));
    }

    @Test
    void testRejectCredentialsEmbeddedInUrl() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://admin:secret@prometheus.io:9090", "prometheus"));
    }

    @Test
    void testRejectUnsupportedSchemes() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("ftp://prometheus.io:9090", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("file:///etc/passwd", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("gopher://prometheus.io:9090", "prometheus"));
    }

    @Test
    void testRejectProhibitedPortsForKind() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("https://prometheus.io:22", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("https://prometheus.io:3306", "prometheus"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("https://prometheus.io:8080", "prometheus"));
    }

    @Test
    void testValidPublicUrlPassesValidation() {
        assertDoesNotThrow(() -> validator.validateUrl("https://prometheus.io:443", "prometheus"));
    }
}
