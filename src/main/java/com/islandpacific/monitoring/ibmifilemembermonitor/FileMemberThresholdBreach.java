package com.islandpacific.monitoring.ibmifilemembermonitor;

/**
 * Represents a specific file member that has either breached a threshold
 * or cleared a previous breach.
 */
public class FileMemberThresholdBreach {
    private final String library;
    private final String fileName;
    private final String memberName;
    private final long currentRecordCount;
    private final long threshold;
    private final String status; // e.g., "BREACHED", "CLEARED"

    public FileMemberThresholdBreach(String library, String fileName, String memberName, long currentRecordCount, long threshold, String status) {
        this.library = library;
        this.fileName = fileName;
        this.memberName = memberName;
        this.currentRecordCount = currentRecordCount;
        this.threshold = threshold;
        this.status = status;
    }

    public String getLibrary() {
        return library;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMemberName() {
        return memberName;
    }

    public long getCurrentRecordCount() {
        return currentRecordCount;
    }

    public long getThreshold() {
        return threshold;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "FileMemberThresholdBreach{" +
                "library='" + library + '\'' +
                ", fileName='" + fileName + '\'' +
                ", memberName='" + memberName + '\'' +
                ", currentRecordCount=" + currentRecordCount +
                ", threshold=" + threshold +
                ", status='" + status + '\'' +
                '}';
    }
}
