package com.islandpacific.monitoring.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sun.jna.platform.win32.Crypt32Util;

/**
 * Encrypts/decrypts credential values using Windows DPAPI (machine scope).
 *
 * Property values wrapped as DPAPI(base64...) are decrypted at config load;
 * any other value is returned unchanged, so plaintext properties keep working.
 * Values are produced by CredTool.jar and can only be decrypted on the machine
 * where they were encrypted.
 */
public final class CredentialProtector {

    private static final String PREFIX = "DPAPI(";
    private static final String SUFFIX = ")";

    private static final int CRYPTPROTECT_UI_FORBIDDEN = 0x1;
    private static final int CRYPTPROTECT_LOCAL_MACHINE = 0x4;

    private CredentialProtector() {
    }

    /** Returns the decrypted value if wrapped as DPAPI(...), otherwise the value unchanged. */
    public static String resolve(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith(PREFIX) || !trimmed.endsWith(SUFFIX)) {
            return value;
        }
        String base64 = trimmed.substring(PREFIX.length(), trimmed.length() - SUFFIX.length());
        try {
            byte[] blob = Base64.getDecoder().decode(base64);
            byte[] plain = Crypt32Util.cryptUnprotectData(blob, CRYPTPROTECT_LOCAL_MACHINE | CRYPTPROTECT_UI_FORBIDDEN);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to decrypt DPAPI(...) credential - it must be re-encrypted on this machine using CredTool.jar", e);
        }
    }

    /** Encrypts a plaintext value with DPAPI machine scope and wraps it as DPAPI(base64...). */
    public static String protect(String plaintext) {
        byte[] blob = Crypt32Util.cryptProtectData(
                plaintext.getBytes(StandardCharsets.UTF_8),
                CRYPTPROTECT_LOCAL_MACHINE | CRYPTPROTECT_UI_FORBIDDEN);
        return PREFIX + Base64.getEncoder().encodeToString(blob) + SUFFIX;
    }
}
