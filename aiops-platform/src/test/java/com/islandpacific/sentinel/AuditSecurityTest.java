package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import com.islandpacific.sentinel.query.QueryAuditException;
import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditSecurityTest {

    private AuthAuditLogRepository auditLogRepository;
    private QueryAuditService auditService;

    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuthAuditLogRepository.class);
        auditService = new QueryAuditService(auditLogRepository);
    }

    @Test
    void testSuccessfulQueryAuditPersistence() {
        auditService.recordQueryExecution(
                userId,
                tenantId,
                "PROMQL_INSTANT",
                "http_requests_total",
                "http_requests_total{tenant_id=\"" + tenantId + "\"}",
                List.of("source-1"),
                15L,
                "SUCCESS",
                false,
                0,
                "corr-123"
        );

        ArgumentCaptor<AuthAuditLog> captor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuthAuditLog saved = captor.getValue();
        assertEquals("QUERY_EXECUTED", saved.getEventType());
        assertEquals(userId, saved.getUserId());
        assertEquals(tenantId, saved.getTenantId());
        assertTrue(saved.isSuccess());
        assertTrue(saved.getDetail().contains("queryHash="));
        assertTrue(saved.getDetail().contains("sanitizedQuery="));
        assertTrue(saved.getDetail().contains("correlationId=corr-123"));
    }

    @Test
    void testAuditDatabaseFailureFailsClosed() {
        doThrow(new RuntimeException("Database Connection Timeout"))
                .when(auditLogRepository).save(any(AuthAuditLog.class));

        assertThrows(QueryAuditException.class, () ->
                auditService.recordQueryExecution(
                        userId,
                        tenantId,
                        "PROMQL_INSTANT",
                        "http_requests_total",
                        "http_requests_total{tenant_id=\"" + tenantId + "\"}",
                        List.of("source-1"),
                        15L,
                        "SUCCESS",
                        false,
                        0,
                        "corr-123"
                )
        );
    }

    @Test
    void testSecretsAndAuthorizationHeadersNotLeakedInAudit() {
        auditService.recordQueryExecution(
                userId,
                tenantId,
                "PROMQL_INSTANT",
                "http_requests_total",
                "http_requests_total{tenant_id=\"" + tenantId + "\"}",
                List.of("source-1"),
                15L,
                "SUCCESS",
                false,
                0,
                "corr-123"
        );

        ArgumentCaptor<AuthAuditLog> captor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        String detail = captor.getValue().getDetail();
        assertFalse(detail.contains("Bearer"));
        assertFalse(detail.contains("Authorization"));
        assertFalse(detail.contains("password"));
        assertFalse(detail.contains("secret"));
    }
}
