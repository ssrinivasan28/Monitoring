package com.islandpacific.sentinel.repository;

import com.islandpacific.sentinel.entity.ToolCall;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only repository for SOC 2 audit logging.
 * UPDATE and DELETE methods are intentionally omitted and blocked by DB triggers.
 */
public interface ToolCallRepository extends Repository<ToolCall, UUID> {
    ToolCall save(ToolCall toolCall);
    Optional<ToolCall> findById(UUID id);
    List<ToolCall> findByTenantId(UUID tenantId);
    List<ToolCall> findByTenantIdAndAgentRunId(UUID tenantId, UUID agentRunId);
}
