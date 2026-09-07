package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.CostLedger;
import com.islandpacific.sentinel.repository.CostLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CostLedgerService {

    private final CostLedgerRepository costLedgerRepository;

    public CostLedgerService(CostLedgerRepository costLedgerRepository) {
        this.costLedgerRepository = costLedgerRepository;
    }

    @Transactional
    public CostLedger recordCost(UUID tenantId, UUID agentRunId, int tokens, BigDecimal cost) {
        CostLedger ledger = new CostLedger(tenantId, agentRunId, tokens, cost);
        return costLedgerRepository.save(ledger);
    }

    @Transactional(readOnly = true)
    public List<CostLedger> getCostLedgerForTenant(UUID tenantId) {
        return costLedgerRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<CostLedger> getCostLedgerSince(Instant since) {
        return costLedgerRepository.findByCreatedAtGreaterThanEqual(since);
    }
}
