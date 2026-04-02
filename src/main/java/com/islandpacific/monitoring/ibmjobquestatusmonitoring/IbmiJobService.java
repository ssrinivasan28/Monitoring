package com.islandpacific.monitoring.ibmjobquestatusmonitoring;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.JobList;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IbmiJobService {

    private static final Logger logger = Logger.getLogger(IbmiJobService.class.getName());

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;

    public IbmiJobService(String ibmiHost, String ibmiUser, String ibmiPassword) {
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
    }

    public JobInfo getJobInfo(String jobName, String jobUser, String expectedSubsystem) throws Exception {
        AS400 system = null;
        try {
            system = new AS400(ibmiHost, ibmiUser, ibmiPassword);
            system.connectService(AS400.COMMAND); // Connect to a service, e.g., Command service

            JobList jobList = new JobList(system);

            jobList.addJobSelectionCriteria(JobList.SELECTION_JOB_NAME, jobName);
            jobList.addJobSelectionCriteria(JobList.SELECTION_USER_NAME, jobUser);

            logger.info(String.format(
                    "Loading job list for JobName='%s', JobUser='%s', ExpectedSubsystem='%s' (manual status and subsystem filtering)",
                    jobName, jobUser, (expectedSubsystem != null ? expectedSubsystem : "any")));

            jobList.load(); // Load all jobs matching name/user, then filter manually

            Enumeration<?> jobs = jobList.getJobs();
            while (jobs.hasMoreElements()) {
                Job job = (Job) jobs.nextElement();
                String currentJobName = job.getName();
                String currentJobUser = job.getUser();
                String currentSubsystem = job.getSubsystem();
                String currentStatus = job.getStatus();

                // Log every job found by JobList.load() for debugging
                logger.info(
                        String.format("DEBUG: Found job by JobList.load(): Name=%s, User=%s, Status=%s, Subsystem=%s",
                                currentJobName, currentJobUser, currentStatus, currentSubsystem));

                // Manual check for active status
                // Added "*ACTIVE" to the list of active statuses as observed in logs.
                boolean isActiveStatus = currentStatus != null &&
                        (currentStatus.trim().equalsIgnoreCase("RUN") ||
                                currentStatus.trim().equalsIgnoreCase("MSGW") ||
                                currentStatus.trim().equalsIgnoreCase("DEQW") ||
                                currentStatus.trim().equalsIgnoreCase("CNDW") ||
                                currentStatus.trim().equalsIgnoreCase("THDW") ||
                                currentStatus.trim().equalsIgnoreCase("HLD") ||
                                currentStatus.trim().equalsIgnoreCase("DEQN") || // From previous fix
                                currentStatus.trim().equalsIgnoreCase("*ACTIVE")); // Added *ACTIVE

                if (!isActiveStatus) {
                    logger.info(String.format("DEBUG: Job %s/%s in %s is not considered active (Status: %s). Skipping.",
                            currentJobName, currentJobUser, currentSubsystem, currentStatus));
                    continue; // Skip jobs that are not considered active
                }

                // If an expected subsystem is specified, filter by it. Otherwise, consider any
                // subsystem.
                // Modified to check if currentSubsystem contains expectedSubsystem
                // (case-insensitive)
                boolean subsystemMatches = (expectedSubsystem == null || expectedSubsystem.trim().isEmpty() ||
                        (currentSubsystem != null
                                && currentSubsystem.toUpperCase().contains(expectedSubsystem.trim().toUpperCase())));

                if (!subsystemMatches) {
                    logger.info(String.format("DEBUG: Job %s/%s in %s does not match expected subsystem %s. Skipping.",
                            currentJobName, currentJobUser, currentSubsystem, expectedSubsystem));
                    continue; // Skip if subsystem doesn't match
                }

                String currentJobNumber = null;
                String currentJobType = null;
                String currentFunctionType = null;
                String currentFunction = null;

                logger.info(String.format("Found matching active job: Name=%s, User=%s, Status=%s, Subsystem=%s.",
                        currentJobName, currentJobUser, currentStatus, currentSubsystem));

                return new JobInfo(currentJobName, currentJobUser, currentJobNumber, currentStatus,
                        currentJobType, currentSubsystem, currentFunctionType, currentFunction, 0); // Pass 0 for
                                                                                                    // cpuUsed
            }

            logger.info(String.format("Job %s/%s (in %s) not found or not active after all filters.", jobName, jobUser,
                    (expectedSubsystem != null ? expectedSubsystem : "any subsystem")));
            return null; // Job not found or not active in the specified subsystem

        } catch (AS400SecurityException e) {
            logger.log(Level.SEVERE, "IBM i Security Error fetching job info for " + jobName + "/" + jobUser
                    + ": Check user ID and password/authority. " + e.getMessage(), e);
            throw e;
        } catch (AS400Exception e) {
            logger.log(Level.SEVERE,
                    "IBM i System Error fetching job info for " + jobName + "/" + jobUser + ": " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while fetching job info for " + jobName + "/"
                    + jobUser + ": " + e.getMessage(), e);
            throw e;
        } finally {
            if (system != null) {
                try {
                    system.disconnectAllServices();
                    logger.fine("Disconnected from IBM i system.");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error disconnecting from IBM i system: " + e.getMessage(), e);
                }
            }
        }
    }
}
