package nl.gogognome.jobscheduler.scheduler;

public interface JobPersister {

    /**
     * Persist a new job.
     * @param job the job
     */
    void create(ScheduledJob job);

    /**
     * Removes a persisted job.
     * @param jobId the id of the job
     */
    void remove(String jobId);

    /**
     * Updates an existing persited job.
     * @param job the job
     */
    void update(ScheduledJob job);

    /**
     * Gets all persisted jobs.
     * @return all persisted jobs
     */
    Iterable<ScheduledJob> findAllJobs();
}
