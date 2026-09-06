package com.islandpacific.sentinel.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QueryPolicyService {

    @Value("${sentinel.datasource.limits.max-query-length:2048}")
    private int maxQueryLength;

    @Value("${sentinel.datasource.limits.max-range-duration-days:30}")
    private int maxRangeDurationDays;

    @Value("${sentinel.datasource.limits.min-step-seconds:1}")
    private long minStepSeconds;

    @Value("${sentinel.datasource.limits.max-points-per-series:11000}")
    private long maxPointsPerSeries;

    @Value("${sentinel.datasource.limits.max-concurrent-queries-global:100}")
    private int maxConcurrentQueriesGlobal;

    @Value("${sentinel.datasource.limits.max-concurrent-queries-per-tenant:20}")
    private int maxConcurrentQueriesPerTenant;

    private final AtomicInteger activeGlobalQueries = new AtomicInteger(0);
    private final Map<UUID, Semaphore> tenantSemaphores = new ConcurrentHashMap<>();

    public void validateInstantQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new QueryPolicyException("Query string cannot be empty");
        }
        if (query.length() > maxQueryLength) {
            throw new QueryPolicyException("Query length " + query.length() + " exceeds maximum permitted " + maxQueryLength);
        }
    }

    public void validateRangeQuery(String query, double startTimestamp, double endTimestamp, String stepStr) {
        validateInstantQuery(query);

        if (endTimestamp <= startTimestamp) {
            throw new QueryPolicyException("Query range end timestamp must be greater than start timestamp");
        }

        double durationSeconds = endTimestamp - startTimestamp;
        double maxDurationSeconds = maxRangeDurationDays * 86400.0;
        if (durationSeconds > maxDurationSeconds) {
            throw new QueryPolicyException("Query range duration exceeds maximum allowed of " + maxRangeDurationDays + " days");
        }

        long stepSeconds = parseStepToSeconds(stepStr);
        if (stepSeconds < minStepSeconds) {
            throw new QueryPolicyException("Query step " + stepSeconds + "s is smaller than minimum allowed " + minStepSeconds + "s");
        }

        long estimatedPoints = Math.round(durationSeconds / stepSeconds);
        if (estimatedPoints > maxPointsPerSeries) {
            throw new QueryPolicyException("Estimated query points " + estimatedPoints + " exceeds maximum allowed " + maxPointsPerSeries);
        }
    }

    public void acquireSlots(UUID tenantId) {
        // 1. Check global limit
        if (activeGlobalQueries.incrementAndGet() > maxConcurrentQueriesGlobal) {
            activeGlobalQueries.decrementAndGet();
            throw new QueryPolicyException("Global query concurrency limit (" + maxConcurrentQueriesGlobal + ") reached");
        }

        // 2. Check tenant limit
        Semaphore semaphore = tenantSemaphores.computeIfAbsent(tenantId, id -> new Semaphore(maxConcurrentQueriesPerTenant));
        if (!semaphore.tryAcquire()) {
            activeGlobalQueries.decrementAndGet();
            throw new QueryPolicyException("Per-tenant query concurrency limit (" + maxConcurrentQueriesPerTenant + ") reached for tenant " + tenantId);
        }
    }

    public void releaseSlots(UUID tenantId) {
        Semaphore semaphore = tenantSemaphores.get(tenantId);
        if (semaphore != null) {
            semaphore.release();
        }
        activeGlobalQueries.decrementAndGet();
    }

    private long parseStepToSeconds(String stepStr) {
        if (stepStr == null || stepStr.isBlank()) {
            return 14L; // Default 14s if step omitted
        }
        String s = stepStr.trim();
        try {
            if (s.endsWith("s") || s.endsWith("S")) {
                return Long.parseLong(s.substring(0, s.length() - 1));
            } else if (s.endsWith("m") || s.endsWith("M")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 60L;
            } else if (s.endsWith("h") || s.endsWith("H")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 3600L;
            } else if (s.endsWith("d") || s.endsWith("D")) {
                return Long.parseLong(s.substring(0, s.length() - 1)) * 86400L;
            } else {
                return Math.round(Double.parseDouble(s));
            }
        } catch (NumberFormatException e) {
            throw new QueryPolicyException("Invalid query step format: " + stepStr);
        }
    }
}
