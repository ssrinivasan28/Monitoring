package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.Correlation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorrelationRepository extends JpaRepository<Correlation, UUID> {
    List<Correlation> findByIncidentId(UUID incidentId);
}
