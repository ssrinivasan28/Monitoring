package com.islandpacific.sentinel.demo;

import java.util.List;
import java.util.UUID;

public class CrossPlatformDemoResult {

    private UUID tenantId;
    private UUID incidentId;
    private String severity;
    private List<String> platforms;
    private String rootCauseJson; // null if the LLM is unavailable/disabled - correlation still succeeded

    public CrossPlatformDemoResult() {}

    public CrossPlatformDemoResult(UUID tenantId, UUID incidentId, String severity, List<String> platforms, String rootCauseJson) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.severity = severity;
        this.platforms = platforms;
        this.rootCauseJson = rootCauseJson;
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }

    public String getRootCauseJson() { return rootCauseJson; }
    public void setRootCauseJson(String rootCauseJson) { this.rootCauseJson = rootCauseJson; }
}
