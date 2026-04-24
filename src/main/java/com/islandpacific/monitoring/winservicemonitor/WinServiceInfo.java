package com.islandpacific.monitoring.winservicemonitor;

import java.util.HashMap;
import java.util.Map;

public class WinServiceInfo {

    private final String serverName;
    // service Name -> status ("Running", "Stopped", "Unknown", etc.)
    private Map<String, String> serviceStatuses = new HashMap<>();
    // service Name -> DisplayName (for user-friendly alerts)
    private Map<String, String> displayNames = new HashMap<>();

    public WinServiceInfo(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() { return serverName; }

    public Map<String, String> getServiceStatuses() { return serviceStatuses; }

    public void setServiceStatuses(Map<String, String> serviceStatuses) {
        this.serviceStatuses = serviceStatuses;
    }

    public void putServiceStatus(String serviceName, String status) {
        serviceStatuses.put(serviceName, status);
    }

    public void setDisplayNames(Map<String, String> displayNames) {
        this.displayNames = displayNames;
    }

    /** Returns DisplayName if available, otherwise falls back to the service Name. */
    public String getDisplayName(String serviceName) {
        return displayNames.getOrDefault(serviceName, serviceName);
    }
}
