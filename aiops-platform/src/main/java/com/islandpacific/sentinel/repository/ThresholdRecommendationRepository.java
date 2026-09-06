package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.ThresholdRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThresholdRecommendationRepository extends JpaRepository<ThresholdRecommendation, UUID> {
    List<ThresholdRecommendation> findByTenantId(UUID tenantId);
    List<ThresholdRecommendation> findByTenantIdAndStatus(UUID tenantId, String status);
}
