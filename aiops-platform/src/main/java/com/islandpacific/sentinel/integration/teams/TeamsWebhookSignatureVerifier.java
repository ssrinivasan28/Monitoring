package com.islandpacific.sentinel.integration.teams;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 2.3 - validates the {@code Authorization: HMAC <signature>} header Microsoft Teams sends on every
 * Outgoing Webhook request: {@code base64(HMACSHA256(rawRequestBody, base64Decode(sharedSecret)))}.
 * Stateless/pure - no Spring wiring needed, matching {@code RoleMapper}'s plain-utility convention.
 */
public final class TeamsWebhookSignatureVerifier {

    private static final String HEADER_PREFIX = "HMAC ";

    private TeamsWebhookSignatureVerifier() {}

    /**
     * @param rawBody           the exact bytes of the request body, unmodified by any JSON parsing
     * @param authorizationHeader the request's {@code Authorization} header value, e.g. {@code "HMAC abc123=="}
     * @param base64Secret       the tenant's Teams-issued shared secret (base64-encoded), as stored
     *                           in {@code TeamsChannelConfig.chatOpsHmacSecret}
     * @return true only if the header is present, well-formed, and matches the computed signature
     */
    public static boolean verify(byte[] rawBody, String authorizationHeader, String base64Secret) {
        if (rawBody == null || authorizationHeader == null || base64Secret == null
                || !authorizationHeader.startsWith(HEADER_PREFIX)) {
            return false;
        }
        String providedSignature = authorizationHeader.substring(HEADER_PREFIX.length()).trim();
        if (providedSignature.isEmpty()) {
            return false;
        }

        String expectedSignature;
        try {
            byte[] secretKeyBytes = Base64.getDecoder().decode(base64Secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody);
            expectedSignature = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // Malformed secret (not valid base64) or an unexpected MAC failure - never a match.
            return false;
        }

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
