package com.islandpacific.sentinel;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import com.islandpacific.sentinel.query.QueryAuditException;
import com.islandpacific.sentinel.query.QueryAuditService;
import com.islandpacific.sentinel.repository.AuthAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class AuditTamperResistanceTest {

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
    void testAuditInsertAllowed() {
        assertDoesNotThrow(() -> auditService.recordQueryExecution(
                userId,
                tenantId,
                "PROMQL_INSTANT",
                "up",
                "up{tenant_id=\"" + tenantId + "\"}",
                List.of("source-1"),
                10L,
                "SUCCESS",
                false,
                0,
                "corr-1"
        ));
    }

    @Test
    void testAuditFailureFailsClosedWithoutReturningResult() {
        doThrow(new RuntimeException("DB Persistence Exception"))
                .when(auditLogRepository).save(any(AuthAuditLog.class));

        assertThrows(QueryAuditException.class, () -> auditService.recordQueryExecution(
                userId,
                tenantId,
                "PROMQL_INSTANT",
                "up",
                "up{tenant_id=\"" + tenantId + "\"}",
                List.of("source-1"),
                10L,
                "SUCCESS",
                false,
                0,
                "corr-1"
        ));
    }
}
