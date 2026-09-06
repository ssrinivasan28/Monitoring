package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.AgentRun;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only repository for SOC 2 audit logging.
 * UPDATE and DELETE methods are intentionally omitted and blocked by DB triggers.
 */
public interface AgentRunRepository extends Repository<AgentRun, UUID> {
    AgentRun save(AgentRun agentRun);
    Optional<AgentRun> findById(UUID id);
    List<AgentRun> findByTenantId(UUID tenantId);
    Optional<AgentRun> findByTenantIdAndId(UUID tenantId, UUID id);
}
