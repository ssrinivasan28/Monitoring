package com.islandpacific.monitoring.ibmjobquecountmonitoring;


public class JobQueueInfo {
    private final String id;      // Unique ID for this job queue configuration (e.g., "batch_jobq")
    private final String name;    // Name of the job queue (e.g., "QBATCH")
    private final String library; // Library where the job queue resides (e.g., "QGPL")
    private final int threshold;  // Threshold for waiting jobs to trigger an alert


    public JobQueueInfo(String id, String name, String library, int threshold) {
        this.id = id;
        this.name = name;
        this.library = library;
        this.threshold = threshold;
    }

 
    public String getId() {
        return id;
    }

   
    public String getName() {
        return name;
    }

    public String getLibrary() {
        return library;
    }


    public int getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "JobQueueInfo{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", library='" + library + '\'' +
               ", threshold=" + threshold +
               '}';
    }
}
