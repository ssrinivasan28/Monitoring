package com.islandpacific.sentinel.correlation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinel.correlation")
public class CorrelationEngineProperties {

    private long pollIntervalMs = 60_000;
    private int windowMinutes = 5;
    private int consecutiveBreachThreshold = 3;
    private int flapWindowMinutes = 15;
    private int flapThreshold = 3;

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    public int getConsecutiveBreachThreshold() { return consecutiveBreachThreshold; }
    public void setConsecutiveBreachThreshold(int consecutiveBreachThreshold) { this.consecutiveBreachThreshold = consecutiveBreachThreshold; }

    public int getFlapWindowMinutes() { return flapWindowMinutes; }
    public void setFlapWindowMinutes(int flapWindowMinutes) { this.flapWindowMinutes = flapWindowMinutes; }

    public int getFlapThreshold() { return flapThreshold; }
    public void setFlapThreshold(int flapThreshold) { this.flapThreshold = flapThreshold; }
}
