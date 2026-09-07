package com.islandpacific.sentinel.service;

import com.islandpacific.sentinel.entity.AgentRun;
import com.islandpacific.sentinel.entity.TenantQuota;
import com.islandpacific.sentinel.exception.QuotaExceededException;
import com.islandpacific.sentinel.repository.AgentRunRepository;
import com.islandpacific.sentinel.repository.TenantQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class QuotaService {

    private final TenantQuotaRepository tenantQuotaRepository;
    private final AgentRunRepository agentRunRepository;

    public QuotaService(TenantQuotaRepository tenantQuotaRepository, AgentRunRepository agentRunRepository) {
        this.tenantQuotaRepository = tenantQuotaRepository;
        this.agentRunRepository = agentRunRepository;
    }

    /**
     * Checks if tenant has remaining quota for LLM executions.
     * Throws QuotaExceededException if daily or monthly limits are breached.
     *
     * @param tenantId tenant identifier
     */
    @Transactional(readOnly = true)
    public void checkQuota(UUID tenantId) {
        if (tenantId == null) {
            return;
        }

        TenantQuota quota = tenantQuotaRepository.findById(tenantId)
                .orElseGet(() -> new TenantQuota(tenantId, 100000, 2000000, new BigDecimal("50.0000"), new BigDecimal("1000.0000")));

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AgentRun> dailyRuns = agentRunRepository.findByTenantIdAndCreatedAtGreaterThanEqual(tenantId, startOfDay);
        List<AgentRun> monthlyRuns = agentRunRepository.findByTenantIdAndCreatedAtGreaterThanEqual(tenantId, startOfMonth);

        int dailyTokens = dailyRuns.stream().mapToInt(r -> r.getTokensIn() + r.getTokensOut()).sum();
        int monthlyTokens = monthlyRuns.stream().mapToInt(r -> r.getTokensIn() + r.getTokensOut()).sum();

        BigDecimal dailyCost = dailyRuns.stream()
                .map(AgentRun::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyCost = monthlyRuns.stream()
                .map(AgentRun::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (dailyTokens >= quota.getDailyTokenLimit()) {
            throw new QuotaExceededException("Daily token quota exceeded for tenant " + tenantId + " (" + dailyTokens + "/" + quota.getDailyTokenLimit() + ")");
        }

        if (monthlyTokens >= quota.getMonthlyTokenLimit()) {
            throw new QuotaExceededException("Monthly token quota exceeded for tenant " + tenantId + " (" + monthlyTokens + "/" + quota.getMonthlyTokenLimit() + ")");
        }

        if (dailyCost.compareTo(quota.getDailyCostLimit()) >= 0) {
            throw new QuotaExceededException("Daily cost quota exceeded for tenant " + tenantId + " ($" + dailyCost + "/$" + quota.getDailyCostLimit() + ")");
        }

        if (monthlyCost.compareTo(quota.getMonthlyCostLimit()) >= 0) {
            throw new QuotaExceededException("Monthly cost quota exceeded for tenant " + tenantId + " ($" + monthlyCost + "/$" + quota.getMonthlyCostLimit() + ")");
        }
    }

    /**
     * Retrieves quota settings for tenant.
     */
    @Transactional(readOnly = true)
    public TenantQuota getTenantQuota(UUID tenantId) {
        return tenantQuotaRepository.findById(tenantId)
                .orElseGet(() -> new TenantQuota(tenantId, 100000, 2000000, new BigDecimal("50.0000"), new BigDecimal("1000.0000")));
    }

    /**
     * Updates quota settings for tenant.
     */
    @Transactional
    public TenantQuota updateTenantQuota(UUID tenantId, int dailyTokenLimit, int monthlyTokenLimit, BigDecimal dailyCostLimit, BigDecimal monthlyCostLimit) {
        TenantQuota quota = tenantQuotaRepository.findById(tenantId)
                .orElseGet(() -> new TenantQuota(tenantId, dailyTokenLimit, monthlyTokenLimit, dailyCostLimit, monthlyCostLimit));

        quota.setDailyTokenLimit(dailyTokenLimit);
        quota.setMonthlyTokenLimit(monthlyTokenLimit);
        quota.setDailyCostLimit(dailyCostLimit);
        quota.setMonthlyCostLimit(monthlyCostLimit);
        quota.setUpdatedAt(Instant.now());

        return tenantQuotaRepository.save(quota);
    }
}
