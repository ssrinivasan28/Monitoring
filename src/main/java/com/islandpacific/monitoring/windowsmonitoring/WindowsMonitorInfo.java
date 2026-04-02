package com.islandpacific.monitoring.windowsmonitoring;

import java.util.HashMap;
import java.util.Map;

public class WindowsMonitorInfo {
    private String hostName;
    private double cpuUtilization;
    private double memoryTotalGB;
    private double memoryUsedGB;
    private double memoryFreeGB;
    private double memoryUtilization;
    private Map<String, DiskInfo> disks;
    private Map<String, NetworkInfo> networkAdapters;
    private Map<String, Double> topProcesses; // Name -> CPU %
    private double systemUptimeHours;
    private Map<String, String> serviceStatuses = new HashMap<>();

    public WindowsMonitorInfo(String hostName) {
        this.hostName = hostName;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Getters and Setters
    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public double getCpuUtilization() {
        return round(cpuUtilization);
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public double getMemoryTotalGB() {
        return round(memoryTotalGB);
    }

    public void setMemoryTotalGB(double memoryTotalGB) {
        this.memoryTotalGB = memoryTotalGB;
    }

    public double getMemoryUsedGB() {
        return round(memoryUsedGB);
    }

    public void setMemoryUsedGB(double memoryUsedGB) {
        this.memoryUsedGB = memoryUsedGB;
    }

    public double getMemoryFreeGB() {
        return round(memoryFreeGB);
    }

    public void setMemoryFreeGB(double memoryFreeGB) {
        this.memoryFreeGB = memoryFreeGB;
    }

    public double getMemoryUtilization() {
        return round(memoryUtilization);
    }

    public void setMemoryUtilization(double memoryUtilization) {
        this.memoryUtilization = memoryUtilization;
    }

    public Map<String, DiskInfo> getDisks() {
        return disks;
    }

    public void setDisks(Map<String, DiskInfo> disks) {
        this.disks = disks;
    }

    public Map<String, NetworkInfo> getNetworkAdapters() {
        return networkAdapters;
    }

    public void setNetworkAdapters(Map<String, NetworkInfo> networkAdapters) {
        this.networkAdapters = networkAdapters;
    }

    public Map<String, Double> getTopProcesses() {
        // Round values in the map as well
        if (topProcesses == null)
            return null;
        Map<String, Double> rounded = new java.util.LinkedHashMap<>();
        topProcesses.forEach((k, v) -> rounded.put(k, round(v)));
        return rounded;
    }

    public void setTopProcesses(Map<String, Double> topProcesses) {
        this.topProcesses = topProcesses;
    }

    public double getSystemUptimeHours() {
        return round(systemUptimeHours);
    }

    public void setSystemUptimeHours(double systemUptimeHours) {
        this.systemUptimeHours = systemUptimeHours;
    }

    public Map<String, String> getServiceStatuses() {
        return serviceStatuses;
    }

    public void setServiceStatuses(Map<String, String> serviceStatuses) {
        this.serviceStatuses = serviceStatuses;
    }

    public static class DiskInfo {
        public double totalGB;
        public double usedGB;
        public double freeGB;
        public double usagePercent;

        // Nested round for convenience if needed, but we'll round in the service or
        // here
        public double getTotalGB() {
            return Math.round(totalGB * 100.0) / 100.0;
        }

        public double getUsedGB() {
            return Math.round(usedGB * 100.0) / 100.0;
        }

        public double getFreeGB() {
            return Math.round(freeGB * 100.0) / 100.0;
        }

        public double getUsagePercent() {
            return Math.round(usagePercent * 100.0) / 100.0;
        }
    }

    public static class NetworkInfo {
        public long bytesSent;
        public long bytesReceived;
    }
}
