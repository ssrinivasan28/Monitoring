package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.TenantSsoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSsoConfigRepository extends JpaRepository<TenantSsoConfig, UUID> {
    Optional<TenantSsoConfig> findByTenantId(UUID tenantId);
}
