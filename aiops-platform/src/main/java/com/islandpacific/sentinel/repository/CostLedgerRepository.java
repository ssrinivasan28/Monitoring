package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.CostLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CostLedgerRepository extends JpaRepository<CostLedger, UUID> {
    List<CostLedger> findByTenantId(UUID tenantId);
}
