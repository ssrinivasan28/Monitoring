package com.islandpacific.monitoring.ibmsubsystemmonitoring;


public class SubsystemInfo {
    private final String name; // Will store SUBSYSTEM_DESCRIPTION (our identifier)
    private final String description; // Will store SUBSYSTEM_DESCRIPTION (redundant but kept for consistency)
    private final String status; // e.g., 'ACTIVE', 'INACTIVE', 'STARTING', 'ENDING'
    private final String library; // New: Library retrieved from SUBSYSTEM_DESCRIPTION_LIBRARY

   
    public SubsystemInfo(String name, String description, String status, String library) {
        this.name = name;
        this.description = description; // This will be the same as 'name' for now
        this.status = status;
        this.library = library;
    }

 
    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }

   
    public String getStatus() {
        return status;
    }

    /**
     * Returns the library of the subsystem as retrieved from the database.
     */
    public String getLibrary() {
        return library;
    }

    @Override
    public String toString() {
        return "SubsystemInfo{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", status='" + status + '\'' +
               ", library='" + library + '\'' +
               '}';
    }
}
