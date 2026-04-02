package com.islandpacific.monitoring.ibmjobquestatusmonitoring; // Corrected package name

/**
 * Represents information about an IBM i Job.
 * This data is typically retrieved from the QSYS2.ACTIVE_JOB_INFO SQL view.
 */
public class JobInfo {
    private String jobName;
    private String jobUser;
    private String jobNumber;
    private String jobStatus; // e.g., "RUN", "MSGW", "DEQW"
    private String jobType;   // e.g., "BCH", "SBS", "INT"
    private String subsystemName;
    private String functionType; // e.g., "PGM", "CMD", "SRV"
    private String function;     // e.g., program name, command text, server type
    private long cpuUsed;        // CPU time used in milliseconds

    

    public JobInfo(String jobName, String jobUser, String jobNumber, String jobStatus,
                   String jobType, String subsystemName, String functionType,
                   String function, long cpuUsed) {
        this.jobName = jobName;
        this.jobUser = jobUser;
        this.jobNumber = jobNumber;
        this.jobStatus = jobStatus;
        this.jobType = jobType;
        this.subsystemName = subsystemName;
        this.functionType = functionType;
        this.function = function;
        this.cpuUsed = cpuUsed;
    }

    // --- Getters ---
    public String getJobName() {
        return jobName;
    }

    public String getJobUser() {
        return jobUser;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public String getJobType() {
        return jobType;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public String getFunctionType() {
        return functionType;
    }

    public String getFunction() {
        return function;
    }

    public long getCpuUsed() {
        return cpuUsed;
    }

  
    public boolean isActive() {
        
        return jobStatus != null &&
               !"END".equalsIgnoreCase(jobStatus.trim()) && // Not explicitly ended
               !"OUTQ".equalsIgnoreCase(jobStatus.trim()) && // Not on output queue
               !"JOBQ".equalsIgnoreCase(jobStatus.trim()); // Not on job queue
    }

    @Override
    public String toString() {
        return "JobInfo{" +
               "jobName='" + jobName + '\'' +
               ", jobUser='" + jobUser + '\'' +
               ", jobNumber='" + jobNumber + '\'' +
               ", jobStatus='" + jobStatus + '\'' +
               ", jobType='" + jobType + '\'' +
               ", subsystemName='" + subsystemName + '\'' +
               ", functionType='" + functionType + '\'' +
               ", function='" + function + '\'' +
               ", cpuUsed=" + cpuUsed +
               '}';
    }
}
