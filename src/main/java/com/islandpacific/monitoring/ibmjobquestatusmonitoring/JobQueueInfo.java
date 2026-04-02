package com.islandpacific.monitoring.ibmjobquestatusmonitoring; // Updated package name to match EmailService


public class JobQueueInfo {
    private String name;
    private String library;
    private String status; // e.g., "HELD", "RELEASED"
    private int numberOfJobs; // Total jobs in the queue
    private int activeJobs;   // Jobs from this JQ currently active in a subsystem
    private String subsystemName; // Subsystem this JQ is associated with
    private String subsystemLibraryName; // Library of the associated subsystem
    private String textDescription;

   
    private int threshold;


    public JobQueueInfo(String name, String library, String status, int numberOfJobs, int activeJobs,
                        String subsystemName, String subsystemLibraryName, String textDescription) {
        this.name = name;
        this.library = library;
        this.status = status;
        this.numberOfJobs = numberOfJobs;
        this.activeJobs = activeJobs;
        this.subsystemName = subsystemName;
        this.subsystemLibraryName = subsystemLibraryName;
        this.textDescription = textDescription;
        this.threshold = 0; // Default threshold, can be set later if needed
    }

    // Overloaded constructor to include threshold, useful when creating JQInfo from config
    public JobQueueInfo(String name, String library, String status, int numberOfJobs, int activeJobs,
                        String subsystemName, String subsystemLibraryName, String textDescription, int threshold) {
        this(name, library, status, numberOfJobs, activeJobs, subsystemName, subsystemLibraryName, textDescription);
        this.threshold = threshold;
    }


    // --- Getters ---
    public String getName() {
        return name;
    }

    public String getLibrary() {
        return library;
    }

    public String getStatus() {
        return status;
    }

    public int getNumberOfJobs() {
        return numberOfJobs;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public String getSubsystemLibraryName() {
        return subsystemLibraryName;
    }

    public String getTextDescription() {
        return textDescription;
    }

    public int getThreshold() {
        return threshold;
    }


    public boolean isActive() {
        return "RELEASED".equalsIgnoreCase(status) && subsystemName != null && !subsystemName.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "JobQueueInfo{" +
               "name='" + name + '\'' +
               ", library='" + library + '\'' +
               ", status='" + status + '\'' +
               ", numberOfJobs=" + numberOfJobs +
               ", activeJobs=" + activeJobs +
               ", subsystemName='" + subsystemName + '\'' +
               ", subsystemLibraryName='" + subsystemLibraryName + '\'' +
               ", textDescription='" + textDescription + '\'' +
               ", threshold=" + threshold +
               '}';
    }
}
