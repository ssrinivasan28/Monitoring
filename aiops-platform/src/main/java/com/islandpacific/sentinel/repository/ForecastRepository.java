package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, UUID> {
    List<Forecast> findByTenantId(UUID tenantId);
    List<Forecast> findByTenantIdAndSeries(UUID tenantId, String series);
}
