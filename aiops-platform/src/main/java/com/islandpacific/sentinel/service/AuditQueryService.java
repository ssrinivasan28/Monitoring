package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.CostLedger;
import com.islandpacific.sentinel.entity.ToolCall;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.CostLedgerRepository;
import com.islandpacific.sentinel.repository.ToolCallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditQueryService {

    private final AgentRunRepository agentRunRepository;
    private final ToolCallRepository toolCallRepository;
    private final CostLedgerRepository costLedgerRepository;

    public AuditQueryService(AgentRunRepository agentRunRepository,
                             ToolCallRepository toolCallRepository,
                             CostLedgerRepository costLedgerRepository) {
        this.agentRunRepository = agentRunRepository;
        this.toolCallRepository = toolCallRepository;
        this.costLedgerRepository = costLedgerRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentRun> getAgentRuns(UUID tenantId, Instant since) {
        if (tenantId != null && since != null) {
            return agentRunRepository.findByTenantIdAndCreatedAtGreaterThanEqual(tenantId, since);
        } else if (tenantId != null) {
            return agentRunRepository.findByTenantId(tenantId);
        } else if (since != null) {
            return agentRunRepository.findByCreatedAtGreaterThanEqual(since);
        } else {
            return agentRunRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public Optional<AgentRun> getAgentRunDetail(UUID runId) {
        return agentRunRepository.findById(runId);
    }

    @Transactional(readOnly = true)
    public List<ToolCall> getToolCallsForRun(UUID tenantId, UUID agentRunId) {
        if (tenantId != null) {
            return toolCallRepository.findByTenantIdAndAgentRunId(tenantId, agentRunId);
        }
        return toolCallRepository.findByTenantId(agentRunId); // fallback or find by run id
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTenantCostSummary(UUID tenantId, Instant since) {
        List<CostLedger> entries;
        if (tenantId != null && since != null) {
            entries = costLedgerRepository.findByTenantIdAndCreatedAtGreaterThanEqual(tenantId, since);
        } else if (tenantId != null) {
            entries = costLedgerRepository.findByTenantId(tenantId);
        } else if (since != null) {
            entries = costLedgerRepository.findByCreatedAtGreaterThanEqual(since);
        } else {
            entries = costLedgerRepository.findAll();
        }

        int totalTokens = entries.stream().mapToInt(CostLedger::getTokens).sum();
        BigDecimal totalCost = entries.stream()
                .map(CostLedger::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        summary.put("tenantId", tenantId != null ? tenantId.toString() : "all");
        summary.put("totalEntries", entries.size());
        summary.put("totalTokens", totalTokens);
        summary.put("totalCost", totalCost);
        summary.put("since", since != null ? since.toString() : "all_time");

        return summary;
    }
}
