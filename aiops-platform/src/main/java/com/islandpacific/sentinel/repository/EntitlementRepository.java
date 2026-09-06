package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    Optional<Entitlement> findByTenantId(UUID tenantId);
}
