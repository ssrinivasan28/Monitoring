package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.ItsmSyncFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItsmSyncFailureRepository extends JpaRepository<ItsmSyncFailure, UUID> {
    Optional<ItsmSyncFailure> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<ItsmSyncFailure> findByTenantId(UUID tenantId);
}
