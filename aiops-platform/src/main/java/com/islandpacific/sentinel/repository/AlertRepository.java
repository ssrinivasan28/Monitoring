package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByTenantId(UUID tenantId);
    Optional<Alert> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Alert> findByTenantIdAndFiredAtBetween(UUID tenantId, Instant start, Instant end);
    List<Alert> findByTenantIdAndClearedAtIsNullOrderByFiredAtAsc(UUID tenantId);
}
