package com.islandpacific.sentinel.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 2.1 — bounds for the interactive assistant agent loop (HTTP-request scoped, not a background poll). */
@Component
@ConfigurationProperties(prefix = "sentinel.assistant")
public class AssistantAgentProperties {

    private int maxToolIterations = 8;
    private long maxWallClockMs = 60_000;

    public int getMaxToolIterations() { return maxToolIterations; }
    public void setMaxToolIterations(int maxToolIterations) { this.maxToolIterations = maxToolIterations; }

    public long getMaxWallClockMs() { return maxWallClockMs; }
    public void setMaxWallClockMs(long maxWallClockMs) { this.maxWallClockMs = maxWallClockMs; }
}
