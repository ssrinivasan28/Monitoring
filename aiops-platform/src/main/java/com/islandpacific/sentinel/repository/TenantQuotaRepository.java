package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.TenantQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {
}
