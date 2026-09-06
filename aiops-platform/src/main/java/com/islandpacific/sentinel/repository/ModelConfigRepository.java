package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, UUID> {
    List<ModelConfig> findByTenantId(UUID tenantId);
    List<ModelConfig> findByTenantIdOrTenantIdIsNull(UUID tenantId);
    Optional<ModelConfig> findByTenantIdAndActiveIsTrue(UUID tenantId);
}
