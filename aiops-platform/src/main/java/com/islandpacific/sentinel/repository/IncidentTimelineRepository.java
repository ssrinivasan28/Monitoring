package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.IncidentTimeline;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only repository for SOC 2 compliance.
 * UPDATE and DELETE methods are intentionally omitted and blocked by DB triggers.
 */
public interface IncidentTimelineRepository extends Repository<IncidentTimeline, UUID> {
    IncidentTimeline save(IncidentTimeline timeline);
    Optional<IncidentTimeline> findById(UUID id);
    List<IncidentTimeline> findByTenantId(UUID tenantId);
    List<IncidentTimeline> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<IncidentTimeline> findByTenantIdAndIncidentIdOrderByAtAsc(UUID tenantId, UUID incidentId);
}
