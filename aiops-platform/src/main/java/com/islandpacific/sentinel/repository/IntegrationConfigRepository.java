package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, UUID> {
    List<IntegrationConfig> findByTenantId(UUID tenantId);
    Optional<IntegrationConfig> findByTenantIdAndKind(UUID tenantId, String kind);
}
