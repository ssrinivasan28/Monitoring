package com.islandpacific.monitoring.ibmjobstatusmonitoring;

public class JobStatusInfo {

    private final String jobName;
    private final String jobUser;
    private final String jobNumber;
    private final String subsystem;
    private final String status;
    private final long elapsedSeconds;

    public JobStatusInfo(String jobName, String jobUser, String jobNumber,
                         String subsystem, String status, long elapsedSeconds) {
        this.jobName = jobName;
        this.jobUser = jobUser;
        this.jobNumber = jobNumber;
        this.subsystem = subsystem;
        this.status = status;
        this.elapsedSeconds = elapsedSeconds;
    }

    public String getJobName()       { return jobName; }
    public String getJobUser()       { return jobUser; }
    public String getJobNumber()     { return jobNumber; }
    public String getSubsystem()     { return subsystem; }
    public String getStatus()        { return status; }
    public long getElapsedSeconds()  { return elapsedSeconds; }
    public String getFullJobId()     { return jobNumber + "/" + jobUser + "/" + jobName; }

    @Override
    public String toString() {
        return getFullJobId() + " [" + status + "] subsystem=" + subsystem + " elapsed=" + elapsedSeconds + "s";
    }
}
