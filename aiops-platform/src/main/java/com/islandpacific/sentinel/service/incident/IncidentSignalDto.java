package com.islandpacific.sentinel.service.incident;

import java.util.UUID;

public class IncidentSignalDto {

    private UUID id;
    private UUID monitorId;
    private UUID alertId;
    private String platform;
    private String detailJson;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }

    public UUID getAlertId() { return alertId; }
    public void setAlertId(UUID alertId) { this.alertId = alertId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
}
