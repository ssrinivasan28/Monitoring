package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.ItsmTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItsmTicketRepository extends JpaRepository<ItsmTicket, UUID> {
    Optional<ItsmTicket> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<ItsmTicket> findByTenantId(UUID tenantId);
}
