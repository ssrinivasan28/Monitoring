package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByTenantId(UUID tenantId);
    List<Report> findByTenantIdAndCadence(UUID tenantId, String cadence);
}
