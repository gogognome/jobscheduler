package nl.gogognome.jobscheduler.scheduler;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;
import static nl.gogognome.jobscheduler.scheduler.JobState.RUNNING;

public class JobScheduler {

    private final RunnableJobFinder runnableJobFinder;
    private final JobPersister jobPersister;

    private final Object lock = new Object();
    private final Semaphore startNextRunnableJobSemaphore = new Semaphore(1);
    private final AtomicBoolean unblockThreadsWithingOnNextRunnableJobImmediately = new AtomicBoolean(false);

    public JobScheduler(RunnableJobFinder runnableJobFinder, JobPersister jobPersister) {
        this.runnableJobFinder = runnableJobFinder;
        this.jobPersister = jobPersister;
    }

    /**
     * Replaces the current jobs by the jobs that have been persisted. This method is typicalled called when
     * your application starts and you jobs persisted in a database.
     */
    public void loadPersistedJobs() {
        synchronized (lock) {
            runnableJobFinder.removeAllScheduledJobs();
            for (ScheduledJob job : jobPersister.findAllJobs()) {
                runnableJobFinder.addJob(job);
            }
            lock.notify();
       }
    }

    /**
     * Schedules a new job. The job will have the state idle.
     * @param job the job
     */
    public void schedule(Job job) {
        ensureIsNotNull(job, "job");
        synchronized (lock) {
            ScheduledJob scheduledJob = new ScheduledJob(job, IDLE);
            runnableJobFinder.addJob(scheduledJob);
            jobPersister.create(scheduledJob);
            lock.notify();
        }
    }

    /**
     * Reschedules a job. Only allowed if job has state running.
     * @param job the job
     */
    public void reschedule(Job job) {
        ensureIsNotNull(job, "job");
        synchronized (lock) {
            ScheduledJob scheduledJob = getScheduledJob(job.getId());
            if (scheduledJob.getState() != RUNNING) {
                throw new IllegalJobStateException("Cannot stop the job with id " + job.getId() + " because its state is "
                        + scheduledJob.getState() + " instead of " + RUNNING);
            }
            scheduledJob = scheduledJob.onReschedule();
            runnableJobFinder.updateJob(scheduledJob);
            jobPersister.update(scheduledJob);
            lock.notify();
        }
    }

    /**
     * Removes a job. This method ignores the state of the job. Use this method for example to:
     * <ul>
     *     <li>remove a job when its state is running and its requester is not running anymore.</li>
     *     <li>remove a failed job that cannot be rescheduled (retried) anymore.</li>
     *     <li>remove a job that has been scheduled in the future but is no longer needed.</li>
     * </ul>
     * @param jobId the id of the job
     */
    public void remove(String jobId) {
        ensureIsNotNull(jobId, "jobId");
        synchronized (lock) {
            getScheduledJob(jobId); // ensure the job exists
            runnableJobFinder.removeJob(jobId);
            jobPersister.remove(jobId);
            lock.notify();
        }
    }

    /**
     * Notify the job scheduler about a job that finished with a failure.Only allowed if job has state running.
     * Will change the state of the job to error.
     * @param jobId the id of the job
     */
    public void jobFailed(String jobId) {
        ensureIsNotNull(jobId, "jobId");
        synchronized (lock) {
            ScheduledJob scheduledJob = getScheduledJob(jobId);
            if (scheduledJob.getState() != RUNNING) {
                throw new IllegalJobStateException("Cannot stop the job with id " + jobId + " because its state is "
                        + scheduledJob.getState() + " instead of " + RUNNING);
            }
            scheduledJob = scheduledJob.onError();
            runnableJobFinder.updateJob(scheduledJob);
            jobPersister.update(scheduledJob);
            lock.notify();
        }
    }

    /**
     * Notify the job scheduler about a job that finished successfully. Only allowed if the job has state running.
     * Will remove the job from the scheduler.
     * @param jobId the id of the job
     */
    public void jobFinished(String jobId) {
        ensureIsNotNull(jobId, "jobId");
        synchronized (lock) {
            ScheduledJob scheduledJob = getScheduledJob(jobId);
            if (scheduledJob.getState() != RUNNING) {
                throw new IllegalJobStateException("Cannot stop the job with id " + jobId + " because its state is "
                        + scheduledJob.getState() + " instead of " + RUNNING);
            }
            runnableJobFinder.removeJob(jobId);
            jobPersister.remove(jobId);
            lock.notify();
        }
    }

    private ScheduledJob getScheduledJob(String jobId) {
        ScheduledJob scheduledJob = runnableJobFinder.findById(jobId);
        if (scheduledJob == null) {
            throw new UnknownJobException("No job exists with the id " + jobId);
        }
        return scheduledJob;
    }

    /**
     * Start a job if a job is runnable at the moment. If no job is runnable, then this method returns null immdediately.
     * @param jobRequesterId the identifier of the the application that will execute the job
     * @return the started job or null if no job can be started right now
     */
    public Job tryStartNextRunnableJob(String jobRequesterId) {
        ensureIsNotNull(jobRequesterId, "jobRequesterId");
        synchronized (lock) {
            return tryStartNextRunnableJobUnsynchronized(jobRequesterId);
        }
    }

    /**
     * Starts a job. If no job is runnable at the moment, wait at most the specified amount in milliseconds for
     * a job to become runnable.
     * @param jobRequesterId the identifier of the the application that will execute the job. Must be at least zero.
     * @return the started job or null if no job can be started right now
     */
    public Job startNextRunnableJob(String jobRequesterId, long timeoutMilliseconds) {
        ensureIsNotNull(jobRequesterId, "jobRequesterId");
        if (timeoutMilliseconds < 0) {
            throw new IllegalArgumentException("timeoutMilliseconds must be at least zero");
        }

        long endTime = System.currentTimeMillis() + timeoutMilliseconds;
        // The semaphore is used to ensure only one thread at a time tries to start a job. If no job is runnable now,
        // then busy waiting is with exponential backoff is applied, and that is something that should be done by
        // at most one thread.
        try {
            boolean acquiredPermit = startNextRunnableJobSemaphore.tryAcquire(timeoutMilliseconds, TimeUnit.MILLISECONDS);
            if (!acquiredPermit) {
                return null;
            }
        } catch (InterruptedException e) {
            return null;
        }
        try {
            long delay = 10;
            synchronized (lock) {
                while (!unblockThreadsWithingOnNextRunnableJobImmediately.get()) {
                    Job job = tryStartNextRunnableJobUnsynchronized(jobRequesterId);
                    if (job != null || System.currentTimeMillis() >= endTime) {
                        return job;
                    }
                    try {
                        lock.wait(delay);
                    } catch (InterruptedException e) {
                        return null;
                    }
                    delay = Math.min(2 * delay, 1000);
                }
            }
        } finally {
            startNextRunnableJobSemaphore.release();
        }
        return null;
    }

    private Job tryStartNextRunnableJobUnsynchronized(String jobRequesterId) {
        ScheduledJob scheduledJob = runnableJobFinder.findNextRunnableJob();
        if (scheduledJob == null) {
			return null;
		}

        Job jobToStart = scheduledJob.getJob();
        if (scheduledJob.getState() != IDLE) {
			throw new IllegalJobStateException("Cannot start job with id " + jobToStart.getId() + " because its state is "
					+ scheduledJob.getState() + " instead of " + IDLE);
		}
        scheduledJob = scheduledJob.onStart(jobRequesterId, runnableJobFinder.getTimeoutInstant(jobToStart));
        runnableJobFinder.updateJob(scheduledJob);
        jobPersister.update(scheduledJob);
        return jobToStart;
    }

    public void runBatch(Runnable runnable) {
        synchronized (lock) {
            runnable.run();
        }
    }

    /**
     * Gets a list of the jobs that have been scheduled, including jobs that are currently running or have failed.
     * @return the jobs
     */
    public List<ScheduledJob> findAllJobs() {
        synchronized (lock) {
            return runnableJobFinder.findAllJobs();
        }
    }

    public void unblockThreadsWithingOnNextRunnableJobImmediately(boolean unlockThreadsImmediately) {
        synchronized (lock) {
            unblockThreadsWithingOnNextRunnableJobImmediately.set(unlockThreadsImmediately);
            lock.notifyAll();
        }
    }

    private void ensureIsNotNull(Object object, String variableName) {
        if (object == null) {
            throw new NullPointerException(variableName);
        }
    }
}
