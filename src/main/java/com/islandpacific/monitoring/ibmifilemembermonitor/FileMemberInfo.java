package com.islandpacific.monitoring.ibmifilemembermonitor;

/**
 * A data class to hold information about an IBM i physical file member.
 * This information is typically retrieved from QSYS2.SYSPARTITIONSTAT.
 */
public class FileMemberInfo {
    private final String fileName;
    private final String libraryName;
    private final String memberName;
    private final long numberOfRecords;
    private final long numberOfDeletedRecords;
    private final long sizeInKBytes;
    private final String creationDate; // Formatted date string (e.g., YYYY-MM-DD)
    private final String creationTime; // Formatted time string (e.g., HH.MM.SS)
    private final String textDescription;

    /**
     * Constructs a new FileMemberInfo object.
     *
     * @param fileName The name of the physical file.
     * @param libraryName The library containing the file.
     * @param memberName The name of the member within the file.
     * @param numberOfRecords The number of records (rows) in the member.
     * @param numberOfDeletedRecords The number of deleted records in the member.
     * @param sizeInKBytes The size of the member in kilobytes.
     * @param creationDate The date the member was created (YYYY-MM-DD format).
     * @param creationTime The time the member was created (HH.MM.SS format).
     * @param textDescription The text description of the member.
     */
    public FileMemberInfo(String fileName, String libraryName, String memberName,
                          long numberOfRecords, long numberOfDeletedRecords, long sizeInKBytes,
                          String creationDate, String creationTime, String textDescription) {
        this.fileName = fileName;
        this.libraryName = libraryName;
        this.memberName = memberName;
        this.numberOfRecords = numberOfRecords;
        this.numberOfDeletedRecords = numberOfDeletedRecords;
        this.sizeInKBytes = sizeInKBytes;
        this.creationDate = creationDate;
        this.creationTime = creationTime;
        this.textDescription = textDescription;
    }

    // --- Getters ---

    public String getFileName() {
        return fileName;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getMemberName() {
        return memberName;
    }

    public long getNumberOfRecords() {
        return numberOfRecords;
    }

    public long getNumberOfDeletedRecords() {
        return numberOfDeletedRecords;
    }

    public long getSizeInKBytes() {
        return sizeInKBytes;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getTextDescription() {
        return textDescription;
    }

    @Override
    public String toString() {
        return "FileMemberInfo{" +
                "fileName='" + fileName + '\'' +
                ", libraryName='" + libraryName + '\'' +
                ", memberName='" + memberName + '\'' +
                ", numberOfRecords=" + numberOfRecords +
                ", numberOfDeletedRecords=" + numberOfDeletedRecords +
                ", sizeInKBytes=" + sizeInKBytes +
                ", creationDate='" + creationDate + '\'' +
                ", creationTime='" + creationTime + '\'' +
                ", textDescription='" + textDescription + '\'' +
                '}';
    }
}
