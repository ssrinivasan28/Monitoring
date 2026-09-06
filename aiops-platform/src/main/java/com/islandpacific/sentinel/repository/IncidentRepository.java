package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByTenantId(UUID tenantId);
    Optional<Incident> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Incident> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Incident> findByTenantIdAndStatus(UUID tenantId, String status);
    List<Incident> findByTenantIdAndOpenedAtBetween(UUID tenantId, Instant start, Instant end);
}
