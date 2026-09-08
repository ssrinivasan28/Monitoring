package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.DashboardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, String> {

    @Query("SELECT d FROM DashboardEntity d WHERE d.tenantId IS NULL OR d.tenantId = :tenantId")
    List<DashboardEntity> findAllGlobalAndTenant(@Param("tenantId") UUID tenantId);

    Optional<DashboardEntity> findByIdAndTenantId(String id, UUID tenantId);

    Optional<DashboardEntity> findByIdAndTenantIdIsNull(String id);

    void deleteByIdAndTenantId(String id, UUID tenantId);
}
