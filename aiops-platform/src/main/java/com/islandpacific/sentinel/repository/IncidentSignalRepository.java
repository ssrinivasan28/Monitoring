package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.IncidentSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentSignalRepository extends JpaRepository<IncidentSignal, UUID> {
    List<IncidentSignal> findByTenantId(UUID tenantId);
    List<IncidentSignal> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    boolean existsByTenantIdAndAlertId(UUID tenantId, UUID alertId);
}
