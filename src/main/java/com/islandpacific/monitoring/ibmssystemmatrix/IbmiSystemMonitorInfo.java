package com.islandpacific.monitoring.ibmssystemmatrix;

public class IbmiSystemMonitorInfo {
    private final String host;
    private final double cpuUtilization;
    private final double aspUtilization;
    private final double sharedPoolUtilization;
    private final long totalJobs;
    private final long activeJobs;

    public IbmiSystemMonitorInfo(String host, double cpuUtilization, double aspUtilization,
                                 double sharedPoolUtilization, long totalJobs, long activeJobs) {
        this.host = host;
        this.cpuUtilization = cpuUtilization;
        this.aspUtilization = aspUtilization;
        this.sharedPoolUtilization = sharedPoolUtilization;
        this.totalJobs = totalJobs;
        this.activeJobs = activeJobs;
    }

    public String getHost() { return host; }
    public double getCpuUtilization() { return cpuUtilization; }
    public double getAspUtilization() { return aspUtilization; }
    public double getSharedPoolUtilization() { return sharedPoolUtilization; }
    public long getTotalJobs() { return totalJobs; }
    public long getActiveJobs() { return activeJobs; }
}
