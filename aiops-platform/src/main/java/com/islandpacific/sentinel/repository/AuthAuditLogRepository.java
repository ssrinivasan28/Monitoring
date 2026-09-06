package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {
    List<AuthAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<AuthAuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
