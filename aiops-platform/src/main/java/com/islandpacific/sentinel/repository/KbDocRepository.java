package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.KbDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KbDocRepository extends JpaRepository<KbDoc, UUID> {
    List<KbDoc> findByTenantId(UUID tenantId);
}
