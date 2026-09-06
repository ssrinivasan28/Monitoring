package com.islandpacific.sentinel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService {

    private static final String DEFAULT_SECRET = "SentinelAiOpsPlatformSuperSecureJwtSigningKey2026SecretKey!";
    private static final long ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000L; // 15 mins
    private static final long MFA_PENDING_TTL_MS = 5 * 60 * 1000L; // 5 mins

    private final SecretKey key;

    public JwtTokenService(@Value("${jwt.secret:" + DEFAULT_SECRET + "}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createMfaPendingToken(UUID userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + MFA_PENDING_TTL_MS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("token_type", "MFA_PENDING")
                .audience().add("mfa_verification").and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_TTL_MS);

        return Jwts.builder()
                .subject(principal.getUserId().toString())
                .claim("email", principal.getEmail())
                .claim("displayName", principal.getDisplayName())
                .claim("source", principal.getSource())
                .claim("mfaVerified", principal.isMfaVerified())
                .claim("token_type", "ACCESS")
                .audience().add("api_access").and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidateToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isMfaPendingToken(Claims claims) {
        return "MFA_PENDING".equals(claims.get("token_type", String.class));
    }
}
