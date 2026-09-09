package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.TeamsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamsNotificationRepository extends JpaRepository<TeamsNotification, UUID> {
    Optional<TeamsNotification> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<TeamsNotification> findByTenantId(UUID tenantId);
}
