package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.UserTenantRole;
import com.islandpacific.sentinel.entity.UserTenantRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTenantRoleRepository extends JpaRepository<UserTenantRole, UserTenantRoleId> {
    List<UserTenantRole> findByTenantId(UUID tenantId);
    List<UserTenantRole> findByTenantIdAndUserId(UUID tenantId, UUID userId);
    List<UserTenantRole> findByUserId(UUID userId);
}
