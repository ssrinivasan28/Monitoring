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
import java.util.UUID;

@Service
public class GovernanceAuditService {

    private final AgentRunRepository agentRunRepository;
    private final ToolCallRepository toolCallRepository;
    private final CostLedgerRepository costLedgerRepository;

    public GovernanceAuditService(AgentRunRepository agentRunRepository,
                                  ToolCallRepository toolCallRepository,
                                  CostLedgerRepository costLedgerRepository) {
        this.agentRunRepository = agentRunRepository;
        this.toolCallRepository = toolCallRepository;
        this.costLedgerRepository = costLedgerRepository;
    }

    @Transactional
    public AgentRun logAgentRun(UUID tenantId, String agent, String promptRedacted, String outputJson, String model, int tokensIn, int tokensOut, BigDecimal cost) {
        AgentRun run = new AgentRun(tenantId, agent, promptRedacted, model, tokensIn, tokensOut, cost);
        run.setOutputJson(outputJson);
        run = agentRunRepository.save(run);

        // Also record cost ledger entry
        CostLedger costLedger = new CostLedger(tenantId, run.getId(), tokensIn + tokensOut, cost);
        costLedgerRepository.save(costLedger);

        return run;
    }

    @Transactional
    public ToolCall logToolCall(UUID agentRunId, UUID tenantId, String tool, String argsRedactedJson, String resultSummary) {
        ToolCall toolCall = new ToolCall(agentRunId, tenantId, tool, argsRedactedJson, resultSummary);
        return toolCallRepository.save(toolCall);
    }
}
