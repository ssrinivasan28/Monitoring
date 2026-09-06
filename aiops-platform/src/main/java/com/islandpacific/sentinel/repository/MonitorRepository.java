package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    List<Monitor> findByTenantId(UUID tenantId);
    Optional<Monitor> findByTenantIdAndId(UUID tenantId, UUID id);
}
