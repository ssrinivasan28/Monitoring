package com.islandpacific.sentinel;

import com.islandpacific.sentinel.security.SecretProtector;
import com.islandpacific.sentinel.security.TotpService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpAndSecretProtectorTest {

    private final SecretProtector secretProtector = new SecretProtector.DefaultSecretProtector();
    private final TotpService totpService = new TotpService(secretProtector);

    @Test
    void secretProtector_encryptsAndDecrypts() {
        String original = "super-secret-totp-key-12345";
        String protectedVal = secretProtector.protect(original);
        assertNotNull(protectedVal);
        assertTrue(protectedVal.startsWith("DPAPI("));

        String decrypted = secretProtector.resolve(protectedVal);
        assertEquals(original, decrypted);
    }

    @Test
    void secretProtector_passesThroughPlaintext() {
        String plaintext = "my-plaintext-secret";
        assertEquals(plaintext, secretProtector.resolve(plaintext));
    }

    @Test
    void totpService_generatesSecretAndVerifiesCode() {
        String rawSecret = totpService.generateRawSecret();
        assertNotNull(rawSecret);
        assertTrue(rawSecret.length() >= 16);

        String qrUrl = totpService.getQrCodeUrl("test@example.com", rawSecret);
        assertTrue(qrUrl.contains("otpauth://totp/IP%20Sentinel:test@example.com"));
        assertTrue(qrUrl.contains("secret=" + rawSecret));

        long window = System.currentTimeMillis() / 1000L / 30L;
        int code = totpService.generateCodeForWindow(rawSecret, window);
        String codeStr = String.format("%06d", code);

        String encryptedSecret = totpService.encryptSecret(rawSecret);
        assertTrue(totpService.verifyCode(encryptedSecret, codeStr));
    }

    @Test
    void totpService_rejectsInvalidCode() {
        String rawSecret = totpService.generateRawSecret();
        assertFalse(totpService.verifyCode(rawSecret, "000000"));
    }
}
