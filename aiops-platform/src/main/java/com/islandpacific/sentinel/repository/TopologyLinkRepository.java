package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.TopologyLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopologyLinkRepository extends JpaRepository<TopologyLink, UUID> {
    List<TopologyLink> findByTenantId(UUID tenantId);
    List<TopologyLink> findByTenantIdAndEnabledTrue(UUID tenantId);
}
