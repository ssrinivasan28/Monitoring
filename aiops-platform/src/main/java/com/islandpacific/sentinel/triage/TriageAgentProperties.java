package com.islandpacific.sentinel.triage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "sentinel.triage")
public class TriageAgentProperties {

    private long pollIntervalMs = 60_000;
    private int maxToolIterations = 6;
    private int maxSchemaRetries = 1;
    /** 1.9: hard wall-clock cap for a single incident investigation, across all tool iterations. */
    private long maxWallClockMs = 120_000;
    /** 1.9: hard cost cap (USD) for a single incident investigation, across all its LLM calls. */
    private BigDecimal maxCostPerInvestigation = new BigDecimal("0.50");

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getMaxToolIterations() { return maxToolIterations; }
    public void setMaxToolIterations(int maxToolIterations) { this.maxToolIterations = maxToolIterations; }

    public int getMaxSchemaRetries() { return maxSchemaRetries; }
    public void setMaxSchemaRetries(int maxSchemaRetries) { this.maxSchemaRetries = maxSchemaRetries; }

    public long getMaxWallClockMs() { return maxWallClockMs; }
    public void setMaxWallClockMs(long maxWallClockMs) { this.maxWallClockMs = maxWallClockMs; }

    public BigDecimal getMaxCostPerInvestigation() { return maxCostPerInvestigation; }
    public void setMaxCostPerInvestigation(BigDecimal maxCostPerInvestigation) { this.maxCostPerInvestigation = maxCostPerInvestigation; }
}
