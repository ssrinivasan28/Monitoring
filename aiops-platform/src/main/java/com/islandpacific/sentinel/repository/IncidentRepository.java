package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Incident> findByTenantIdAndStatusAndRootCauseJsonIsNull(UUID tenantId, String status);
    List<Incident> findByTenantIdAndIdNot(UUID tenantId, UUID id);

    /** Tenant-scoped list filter for the 1.3 incident API: status/platform are optional (null = no filter). */
    @Query("SELECT DISTINCT i FROM Incident i WHERE i.tenantId = :tenantId "
            + "AND (:status IS NULL OR i.status = :status) "
            + "AND (:platform IS NULL OR EXISTS "
            + "(SELECT 1 FROM IncidentSignal s WHERE s.incidentId = i.id AND s.platform = :platform))")
    List<Incident> findForTenant(@Param("tenantId") UUID tenantId, @Param("status") String status, @Param("platform") String platform);
}
