package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthAuditService {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|token|secret|jwt|totp|bearer)\\s*[:=]\\s*[^,\\s]+"
    );

    private final AuthAuditLogRepository authAuditLogRepository;

    @Autowired
    public AuthAuditService(AuthAuditLogRepository authAuditLogRepository) {
        this.authAuditLogRepository = authAuditLogRepository;
    }

    @Transactional
    public void logEvent(String eventType, UUID userId, UUID tenantId, boolean success, String detail) {
        String sanitizedDetail = sanitize(detail);
        AuthAuditLog log = new AuthAuditLog(eventType, userId, tenantId, success, sanitizedDetail);
        authAuditLogRepository.save(log);
    }

    private String sanitize(String detail) {
        if (detail == null) return null;
        return SENSITIVE_PATTERN.matcher(detail).replaceAll("$1=[REDACTED]");
    }
}
