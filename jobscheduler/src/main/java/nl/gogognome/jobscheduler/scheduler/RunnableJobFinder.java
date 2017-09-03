package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;
import java.util.List;

/**
 * A runnable job finder maintains a collection of jobs to be run. Modifications to this collection
 * are requested by the job scheduler. Finally, the runnable job finder will return the first job
 * to be run on request of the job scheduler.
 *
 * <p>The job scheduler ensures that only one thread
 * at a time will call this method. The implementation of this class does not need to worry
 * about thread safety.
 */
public interface RunnableJobFinder {

    /**
     * Finds a scheduled job by the job id.
     * @param jobId the job id
     * @return the scheduled job if a job exists with the specified id; null otherwise
     */
    ScheduledJob findById(String jobId);

    /**
     * Adds a scheduled job.
     * @param scheduledJob the job
     * @throws DuplicateJobException if a job with the same id has already been added before
     */
    void addJob(ScheduledJob scheduledJob);

    /**
     * Updates a scheduled job.
     * @param scheduledJob the job
     * @throws UnknownJobException if the job with the same id has not been added before or has already been removed
     */
    void updateJob(ScheduledJob scheduledJob);

    /**
     * Removes a scheduled job.
     * @param jobId the id of the job
     * @throws UnknownJobException if the job with the same id has not been added before or has already been removed
     */
    void removeJob(String jobId);

    /**
     * Determine the next scheduled job that can be started. The returned scheduled job must have status idle.
     *
     * @return the next scheduled job that can be started; null if no job can be started
     */
    ScheduledJob findNextRunnableJob();

    /**
     * Gets a collection of the jobs that have been scheduled, including jobs that are currently running or have failed.
     * Ensure to return an unmodifiable collection of jobs
     * @return the jobs
     */
    List<ScheduledJob> findAllJobs();

    /**
     * Removes all scheduled jobs.
     */
    void removeAllScheduledJobs();

    /**
     * Determines the time out instant for the job. This method is called when the job is started.
     * @param jobToStart the job to start
     * @return the time out instant
     */
    Instant getTimeoutInstant(Job jobToStart);
}
