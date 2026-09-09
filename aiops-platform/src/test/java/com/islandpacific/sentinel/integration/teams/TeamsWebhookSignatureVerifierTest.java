package com.islandpacific.sentinel.integration.teams;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TeamsWebhookSignatureVerifierTest {

    private static final String SECRET_B64 = Base64.getEncoder().encodeToString("super-secret-webhook-key".getBytes(StandardCharsets.UTF_8));

    private String sign(byte[] body, String secretB64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(secretB64);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(body));
    }

    @Test
    void verify_correctSignature_returnsTrue() throws Exception {
        byte[] body = "{\"text\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET_B64);

        assertThat(TeamsWebhookSignatureVerifier.verify(body, "HMAC " + signature, SECRET_B64)).isTrue();
    }

    @Test
    void verify_tamperedBody_returnsFalse() throws Exception {
        byte[] originalBody = "{\"text\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(originalBody, SECRET_B64);
        byte[] tamperedBody = "{\"text\":\"hello!!\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(TeamsWebhookSignatureVerifier.verify(tamperedBody, "HMAC " + signature, SECRET_B64)).isFalse();
    }

    @Test
    void verify_wrongSecret_returnsFalse() throws Exception {
        byte[] body = "{\"text\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET_B64);
        String wrongSecret = Base64.getEncoder().encodeToString("a-completely-different-key".getBytes(StandardCharsets.UTF_8));

        assertThat(TeamsWebhookSignatureVerifier.verify(body, "HMAC " + signature, wrongSecret)).isFalse();
    }

    @Test
    void verify_missingHmacPrefix_returnsFalse() throws Exception {
        byte[] body = "{\"text\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        String signature = sign(body, SECRET_B64);

        assertThat(TeamsWebhookSignatureVerifier.verify(body, signature, SECRET_B64)).isFalse();
    }

    @Test
    void verify_nullOrBlankInputs_returnFalseWithoutThrowing() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        assertThat(TeamsWebhookSignatureVerifier.verify(null, "HMAC abc", SECRET_B64)).isFalse();
        assertThat(TeamsWebhookSignatureVerifier.verify(body, null, SECRET_B64)).isFalse();
        assertThat(TeamsWebhookSignatureVerifier.verify(body, "HMAC abc", null)).isFalse();
        assertThat(TeamsWebhookSignatureVerifier.verify(body, "HMAC ", SECRET_B64)).isFalse();
    }

    @Test
    void verify_malformedBase64Secret_returnsFalseWithoutThrowing() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        assertThat(TeamsWebhookSignatureVerifier.verify(body, "HMAC abc", "not-valid-base64!!!")).isFalse();
    }
}
