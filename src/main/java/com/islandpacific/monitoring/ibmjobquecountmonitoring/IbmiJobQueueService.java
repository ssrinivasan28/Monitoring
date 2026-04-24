package com.islandpacific.monitoring.ibmjobquecountmonitoring;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.JobQueue;
import com.ibm.as400.access.QSYSObjectPathName;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IbmiJobQueueService {

    private static final Logger logger = Logger.getLogger(IbmiJobQueueService.class.getName());

    private final String ibmiHost;
    private final String ibmiUser;
    private final String ibmiPassword;

    public IbmiJobQueueService(String ibmiHost, String ibmiUser, String ibmiPassword) {
        this.ibmiHost = ibmiHost;
        this.ibmiUser = ibmiUser;
        this.ibmiPassword = ibmiPassword;
    }


    @SuppressWarnings("deprecation")
    public int getNumberOfWaitingJobs(String queueName, String queueLibrary) throws Exception {
        AS400 as400 = null;
        try {
            as400 = new AS400(ibmiHost, ibmiUser, ibmiPassword);
            // Connect to command service for basic connectivity check and access to JobQueue
            as400.connectService(AS400.COMMAND);

            QSYSObjectPathName jobQueuePathName = new QSYSObjectPathName(
                queueLibrary.toUpperCase(),
                queueName.toUpperCase(),
                "JOBQ"
            );
            JobQueue jobQueue = new JobQueue(as400, jobQueuePathName);

            logger.info("Attempting to retrieve job count for job queue: " + queueLibrary + "/" + queueName);

            int waitingJobsCount = jobQueue.getNumberOfJobs();
            logger.info(String.format("Successfully retrieved job count for %s/%s: %d",
                                        queueLibrary, queueName, waitingJobsCount));
            return waitingJobsCount;

        } catch (AS400SecurityException e) {
            logger.log(Level.SEVERE, "IBM i Security Error while checking " + queueName + "/" + queueLibrary + ": " + e.getMessage(), e);
            throw e; // Re-throw to be caught by the main monitor
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred while checking job queue " + queueName + "/" + queueLibrary + ": " + e.getMessage(), e);
            throw e; // Re-throw
        } finally {
            if (as400 != null) {
                try {
                    as400.disconnectAllServices();
                    logger.fine("Disconnected from IBM i.");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error disconnecting from IBM i: " + e.getMessage(), e);
                }
            }
        }
    }
}
