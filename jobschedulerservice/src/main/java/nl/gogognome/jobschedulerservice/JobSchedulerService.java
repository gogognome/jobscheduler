package nl.gogognome.jobschedulerservice;

import com.google.gson.Gson;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.dataaccess.transaction.RequireTransaction;
import nl.gogognome.jobscheduler.jobingester.database.*;
import nl.gogognome.jobscheduler.jobpersister.database.DatabaseJobPersister;
import nl.gogognome.jobscheduler.jobpersister.database.DatabaseJobPersisterProperties;
import nl.gogognome.jobscheduler.jobpersister.database.ScheduledJobDAO;
import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobScheduler;
import nl.gogognome.jobscheduler.scheduler.ReadonlyScheduledJob;
import nl.gogognome.jobscheduler.scheduler.RunnableJobFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static nl.gogognome.jobscheduler.jobingester.database.Command.*;

public class JobSchedulerService {

    private final static Logger LOGGER = LoggerFactory.getLogger(JobSchedulerService.class);
    private final static Gson GSON = new Gson();
    private final static Charset CHARSET = Charset.forName("UTF-8");
    private final static String JOB_ID_PREFIX = Long.toString(System.currentTimeMillis()) + '-';

    private final JobScheduler jobScheduler;
    private final JobCommandDAO jobCommandDAO;
    private final JobIngesterRunner jobIngesterRunner;
    private final ExecutorService executorService;
    private final int threadPoolSize;
    
    private AtomicInteger nextId = new AtomicInteger(1);
    private AtomicBoolean started = new AtomicBoolean(false);

    public JobSchedulerService(RunnableJobFinder runnableJobFinder, JobIngesterProperties jobIngesterProperties,
                               DatabaseJobPersisterProperties databaseJobPersisterProperties, int threadPoolSize) {
        DatabaseJobPersister databaseJobPersister = new DatabaseJobPersister(databaseJobPersisterProperties, new ScheduledJobDAO(databaseJobPersisterProperties));
        this.jobScheduler = new JobScheduler(runnableJobFinder, databaseJobPersister);
        this.jobCommandDAO = new JobCommandDAO(jobIngesterProperties);
        JobIngester jobIngester = new JobIngester(jobScheduler, jobCommandDAO);
        this.jobIngesterRunner = new JobIngesterRunner(jobIngesterProperties, jobIngester);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.threadPoolSize = threadPoolSize;
    }

    public void startProcessingJobs() {
        LOGGER.trace("Start processing jobs with " + threadPoolSize + " threads for handling jobs");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Processing jobs has been started before. First stop processing before starting processing again.");
        }
        jobIngesterRunner.start();
        for (int i=0; i<threadPoolSize; i++) {
            String requesterId = "requester-" + (i + 1);
            executorService.submit(() -> jobHandlerLoop(requesterId));
        }
        LOGGER.trace("Processing jobs started");
    }

    public void stopProcessingJobs() {
        LOGGER.trace("Stop processing jobs");
        if (!started.compareAndSet(true, false)) {
            throw new IllegalStateException("Processing jobs has not been started before. First start processing before stopping processing.");
        }

        jobScheduler.unblockThreadsWithingOnNextRunnableJobImmediately(true);
        try {
            jobIngesterRunner.stop();
            executorService.shutdown();
            if (!executorService.awaitTermination(1, MINUTES)) {
                LOGGER.warn("Not all threads handling jobs have terminated!");
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Ignored interrupted exception: " + e.getMessage(), e);
        } finally {
            jobScheduler.unblockThreadsWithingOnNextRunnableJobImmediately(false);
        }
        LOGGER.trace("Stopped processing jobs");
    }

    /**
     * Schedules a job to execute the #Runnable as immediately.
     * @param runnable the #Runnable to be executed
     * @return the id of the job that has been scheduled
     */
    public String schedule(Runnable runnable) {
        return RequireTransaction.returns(() -> {
            validateParameters(runnable);

            Job job = new Job(JOB_ID_PREFIX + nextId.getAndIncrement());
            job.setScheduledAtInstant(Instant.now());
            job.setType(runnable.getClass().getName());
            job.setData(GSON.toJson(runnable).getBytes(CHARSET));

            jobCommandDAO.create(new JobCommand(SCHEDULE, job));
            LOGGER.trace("Scheduled job with type " + job.getType() + " and id " + job.getId());

            return job.getId();
        });
    }

    /**
     * Removes the job with the specified id.
     * @param jobId id of the job
     */
    public void remove(String jobId) {
        RequireTransaction.runs(() -> {
            jobCommandDAO.create(new JobCommand(REMOVE, new Job(jobId)));
            LOGGER.trace("Removed job with id " + jobId);
        });
    }

    private void validateParameters(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable must not be null");
        }
        if (runnable.getClass().getName() == null) {
            throw new IllegalArgumentException("Instance of " + runnable.getClass() + " cannot be stored as job data.");
        }
    }

    private void jobHandlerLoop(String requesterId) {
        LOGGER.trace("Requester " + requesterId + " starts handling jobs");
        while (started.get()) {
            Job job = jobScheduler.startNextRunnableJob(requesterId, 10 * 1000);
            if (job != null) {
                executeJob(requesterId, job);
            }
        }
        LOGGER.trace("Requester " + requesterId + " stops handling jobs");
    }

    private void executeJob(String requesterId, Job job) {
        LOGGER.trace("Requester " + requesterId + " starts handling job with type " + job.getType() + " and id " + job.getId());
        try {
            NewTransaction.runs(() -> {
                Class<?> clazz = Class.forName(job.getType());
                if (!Runnable.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("The type " + job.getType() + " of job " + job.getId() + " cannot be instantiated");
                }
                Runnable runnable = GSON.fromJson(new String(job.getData(), CHARSET), (Class<? extends Runnable>) clazz);
                runnable.run();
                jobCommandDAO.create(new JobCommand(JOB_FINISHED, job));
                LOGGER.trace("Requester " + requesterId + " handled successfully job with type " + job.getType() + " and id " + job.getId());
            });
        } catch (Throwable throwable) {
            NewTransaction.runs(() -> {
                jobCommandDAO.create(new JobCommand(JOB_FAILED, job));
                LOGGER.trace("Requester " + requesterId + " failed to handle job with type " + job.getType() + " and id " + job.getId());
            });
        }
    }

    public List<ReadonlyScheduledJob> findAllJobs() {
        return jobScheduler.findAllJobs();
    }
}
