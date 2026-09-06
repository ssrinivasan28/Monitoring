package com.islandpacific.monitoring.common;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.SkipException;
import org.testng.annotations.Test;

public class CredentialProtectorTest {

    @Test
    public void nullPassesThrough() {
        assertNull(CredentialProtector.resolve(null));
    }

    @Test
    public void plaintextPassesThroughUnchanged() {
        assertEquals(CredentialProtector.resolve("myPlainPassword"), "myPlainPassword");
        assertEquals(CredentialProtector.resolve(""), "");
        assertEquals(CredentialProtector.resolve("DPAPI"), "DPAPI");
        assertEquals(CredentialProtector.resolve("not(DPAPI)"), "not(DPAPI)");
    }

    @Test
    public void protectThenResolveRoundTrips() {
        requireWindows();
        String secret = "s3cr3t!Pa$$word";
        String wrapped = CredentialProtector.protect(secret);
        assertTrue(wrapped.startsWith("DPAPI("));
        assertTrue(wrapped.endsWith(")"));
        assertEquals(CredentialProtector.resolve(wrapped), secret);
    }

    @Test
    public void resolveTrimsWhitespaceAroundWrapper() {
        requireWindows();
        String wrapped = CredentialProtector.protect("abc");
        assertEquals(CredentialProtector.resolve("  " + wrapped + "  "), "abc");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void corruptBlobThrowsIllegalState() {
        requireWindows();
        CredentialProtector.resolve("DPAPI(AAAA)");
    }

    private static void requireWindows() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            throw new SkipException("DPAPI tests require Windows");
        }
    }
}
