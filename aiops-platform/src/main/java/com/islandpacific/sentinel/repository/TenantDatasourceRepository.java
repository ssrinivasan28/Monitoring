package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.TenantDatasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantDatasourceRepository extends JpaRepository<TenantDatasource, UUID> {
    List<TenantDatasource> findByTenantId(UUID tenantId);
    Optional<TenantDatasource> findByTenantIdAndName(UUID tenantId, String name);
    List<TenantDatasource> findByTenantIdAndKind(UUID tenantId, String kind);
    List<TenantDatasource> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);
}
