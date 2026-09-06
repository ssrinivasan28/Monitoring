package com.islandpacific.sentinel.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20; // 160 bits
    private static final int TIME_STEP_SECONDS = 30;

    private final SecretProtector secretProtector;

    @Autowired
    public TotpService(SecretProtector secretProtector) {
        this.secretProtector = secretProtector;
    }

    public String generateRawSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String encryptSecret(String rawSecret) {
        return secretProtector.protect(rawSecret);
    }

    public String decryptSecret(String encryptedSecret) {
        return secretProtector.resolve(encryptedSecret);
    }

    public String getQrCodeUrl(String email, String rawSecret) {
        return String.format("otpauth://totp/IP%%20Sentinel:%s?secret=%s&issuer=IP%%20Sentinel", email, rawSecret);
    }

    public boolean verifyCode(String encryptedOrRawSecret, String codeStr) {
        if (codeStr == null || codeStr.trim().isEmpty() || encryptedOrRawSecret == null) {
            return false;
        }
        String rawSecret = decryptSecret(encryptedOrRawSecret);
        try {
            int targetCode = Integer.parseInt(codeStr.trim());
            long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
            
            // Allow +/- 1 time step window for clock drift
            for (int i = -1; i <= 1; i++) {
                if (generateCodeForWindow(rawSecret, currentWindow + i) == targetCode) {
                    return true;
                }
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    public int generateCodeForWindow(String rawBase32Secret, long window) {
        byte[] key = decodeBase32(rawBase32Secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (window & 0xFF);
            window >>= 8;
        }
        try {
            SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return binary % 1000000;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate TOTP code", e);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = data[0] & 0xFF;
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer = (buffer << 8) | (data[next++] & 0xFF);
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = 0x1F & (buffer >> (bitsLeft - 5));
            bitsLeft -= 5;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private byte[] decodeBase32(String secret) {
        String upper = secret.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] out = new byte[upper.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : upper.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return out;
    }
}
