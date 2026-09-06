package com.islandpacific.sentinel.query;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class QueryAuditService {

    private static final Logger log = LoggerFactory.getLogger(QueryAuditService.class);

    private final AuthAuditLogRepository auditLogRepository;

    @Autowired
    public QueryAuditService(AuthAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void recordQueryExecution(
            UUID userId,
            UUID tenantId,
            String queryType,
            String originalQuery,
            String sanitizedQuery,
            List<String> resolvedDatasourceNames,
            long executionTimeMs,
            String status,
            boolean partialFailure,
            int failureCount,
            String correlationId) {

        String queryHash = hashQuery(originalQuery);

        String detailStr = String.format(
                "queryType=%s | queryHash=%s | sanitizedQuery=%s | datasources=%s | timeMs=%d | status=%s | partialFailure=%b | failureCount=%d | correlationId=%s",
                queryType,
                queryHash,
                sanitizedQuery,
                resolvedDatasourceNames != null ? String.join(",", resolvedDatasourceNames) : "none",
                executionTimeMs,
                status,
                partialFailure,
                failureCount,
                correlationId != null ? correlationId : "none"
        );

        try {
            AuthAuditLog auditLog = new AuthAuditLog(
                    "QUERY_EXECUTED",
                    userId,
                    tenantId,
                    "SUCCESS".equalsIgnoreCase(status) || "PARTIAL_FAILURE".equalsIgnoreCase(status),
                    detailStr
            );
            auditLogRepository.save(auditLog);
            log.debug("Recorded query audit record for tenant {}", tenantId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to persist query audit log for tenant {}", tenantId, e);
            throw new QueryAuditException("Query audit log failed to persist; failing closed for compliance", e);
        }
    }

    private String hashQuery(String query) {
        if (query == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(query.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
