package com.islandpacific.sentinel.security;

import com.sun.jna.platform.win32.Crypt32Util;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SecretProtector abstraction for resolving DPAPI or key/environment-protected values.
 */
public interface SecretProtector {

    String REDACTED = "[REDACTED]";

    String resolve(String value);
    String protect(String plaintext);

    static boolean containsSecret(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        return lower.contains("password") || lower.contains("bearer ") || lower.contains("dpapi(");
    }

    static String redact(String value) {
        return REDACTED;
    }


    @Component
    class DefaultSecretProtector implements SecretProtector {
        private static final String DPAPI_PREFIX = "DPAPI(";
        private static final String DPAPI_SUFFIX = ")";

        private static final int CRYPTPROTECT_UI_FORBIDDEN = 0x1;
        private static final int CRYPTPROTECT_LOCAL_MACHINE = 0x4;

        @Override
        public String resolve(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (!trimmed.startsWith(DPAPI_PREFIX) || !trimmed.endsWith(DPAPI_SUFFIX)) {
                return value; // plaintext passthrough
            }
            String base64 = trimmed.substring(DPAPI_PREFIX.length(), trimmed.length() - DPAPI_SUFFIX.length());
            try {
                byte[] blob = Base64.getDecoder().decode(base64);
                byte[] plain = Crypt32Util.cryptUnprotectData(blob, CRYPTPROTECT_LOCAL_MACHINE | CRYPTPROTECT_UI_FORBIDDEN);
                return new String(plain, StandardCharsets.UTF_8);
            } catch (Throwable e) {
                // Fallback for non-Windows environments or unencryptable values
                return base64;
            }
        }

        @Override
        public String protect(String plaintext) {
            if (plaintext == null) return null;
            try {
                byte[] blob = Crypt32Util.cryptProtectData(
                        plaintext.getBytes(StandardCharsets.UTF_8),
                        CRYPTPROTECT_LOCAL_MACHINE | CRYPTPROTECT_UI_FORBIDDEN);
                return DPAPI_PREFIX + Base64.getEncoder().encodeToString(blob) + DPAPI_SUFFIX;
            } catch (Throwable e) {
                // Fallback base64 wrapping if DPAPI isn't available
                return DPAPI_PREFIX + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8)) + DPAPI_SUFFIX;
            }
        }
    }
}
