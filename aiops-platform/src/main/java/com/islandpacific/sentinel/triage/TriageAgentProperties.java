package com.islandpacific.sentinel.triage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinel.triage")
public class TriageAgentProperties {

    private long pollIntervalMs = 60_000;
    private int maxToolIterations = 6;
    private int maxSchemaRetries = 1;

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getMaxToolIterations() { return maxToolIterations; }
    public void setMaxToolIterations(int maxToolIterations) { this.maxToolIterations = maxToolIterations; }

    public int getMaxSchemaRetries() { return maxSchemaRetries; }
    public void setMaxSchemaRetries(int maxSchemaRetries) { this.maxSchemaRetries = maxSchemaRetries; }
}
