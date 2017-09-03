package nl.gogognome.jobschedulerservice;

import com.zaxxer.hikari.HikariDataSource;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.jobingester.database.JobIngesterProperties;
import nl.gogognome.jobscheduler.jobpersister.database.DatabaseJobPersisterProperties;
import nl.gogognome.jobscheduler.runnablejobfinder.FifoRunnableJobFinder;
import nl.gogognome.test.AssertExtensions.RunnableThrowingException;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static nl.gogognome.jobscheduler.scheduler.JobState.ERROR;
import static nl.gogognome.test.AssertExtensions.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobSchedulerServiceTest {

    private JobSchedulerService jobSchedulerService;

    private static Semaphore nrExecutions;
    private static Instant lastJobExecutedAt;

    @Before
    public void initDataSource() {
        JobIngesterProperties jobIngesterProperties = new JobIngesterProperties();
        DatabaseJobPersisterProperties databaseJobPersisterProperties = new DatabaseJobPersisterProperties();

        HikariDataSource dataSource = buildDataSource();
        CompositeDatasourceTransaction.registerDataSource(jobIngesterProperties.getConnectionName(), dataSource);
        CompositeDatasourceTransaction.registerDataSource(databaseJobPersisterProperties.getConnectionName(), dataSource);
        jobSchedulerService = new JobSchedulerService(new FifoRunnableJobFinder(), jobIngesterProperties, databaseJobPersisterProperties, 4);

        initDatabase(jobIngesterProperties);

        nrExecutions = new Semaphore(0);
        lastJobExecutedAt = null;
    }

    private void initDatabase(JobIngesterProperties jobIngesterProperties) {
        NewTransaction.runs(() -> new DatabaseMigratorDAO(jobIngesterProperties.getConnectionName()).applyMigrationsFromResource("/database/_migrations.txt"));
    }

    private HikariDataSource buildDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:" + Thread.currentThread().getName() + "-" + System.nanoTime());
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Test
    public void startAndStopSucceeds() {
        jobSchedulerService.startProcessingJobs();
        jobSchedulerService.stopProcessingJobs();
    }

    @Test
    public void startTwiceFails() {
        jobSchedulerService.startProcessingJobs();
        assertThrows(IllegalStateException.class, () -> jobSchedulerService.startProcessingJobs());
    }

    @Test
    public void stopWithoutStartingFails() {
        assertThrows(IllegalStateException.class, () -> jobSchedulerService.stopProcessingJobs());
    }

    @Test
    public void runJob_succeeds() throws Exception {
        withRunningJobs(() -> {
            SuccessfulJobRunner successfulJobRunner = new SuccessfulJobRunner();

            jobSchedulerService.schedule(successfulJobRunner);
            assertThatEventually(() -> jobSchedulerService.findAllJobs().isEmpty());
        });
    }

    @Test
    public void runTenJobs_succeeds() throws Exception {
        withRunningJobs(() -> {
            int nrJobs = 10;
            SuccessfulJobRunner successfulJobRunner = new SuccessfulJobRunner();
            for (int i = 0; i < 10; i++) {
                jobSchedulerService.schedule(successfulJobRunner);
            }
            nrExecutions.tryAcquire(nrJobs, 10, SECONDS);
            assertThatEventually(() -> jobSchedulerService.findAllJobs().isEmpty());
        });
    }

    @Test
    public void scheduleJob_jobFails_removeJob_scheduleHasNoJobsAnymore_succeeds() throws Exception {
        withRunningJobs(() -> {
            String jobId = jobSchedulerService.schedule(new FailingJobRunner());
            assertThatEventually(() -> jobSchedulerService.findAllJobs().stream().anyMatch(scheduledJob -> scheduledJob.getState() == ERROR));

            jobSchedulerService.remove(jobId);
            assertThatEventually(() -> jobSchedulerService.findAllJobs().isEmpty());
            assertTrue(jobSchedulerService.findAllJobs().isEmpty());
        });
    }

    @Test
    public void scheduleJobWithDelay_jobIsExecutedAfterDelay() throws Exception {
        withRunningJobs(() -> {
            Instant scheduledAtInstant = Instant.now().plus(Duration.ofSeconds(1));
            jobSchedulerService.schedule(new SuccessfulJobRunner(), scheduledAtInstant);
            assertThatEventually(() -> lastJobExecutedAt != null);
            assertTrue(lastJobExecutedAt.isAfter(scheduledAtInstant.minusMillis(100)));
            assertTrue(lastJobExecutedAt.isBefore(scheduledAtInstant.plusMillis(100)));
        });
    }

    @Test
    public void scheduleJobWithDelay_removeJob_scheduledJobIsNoExecutedAfterDelay() throws Exception {
        withRunningJobs(() -> {
            String jobId = jobSchedulerService.schedule(new SuccessfulJobRunner(), Instant.now().plus(Duration.ofSeconds(1)));
            jobSchedulerService.remove(jobId);

            assertFalse("Job should have been removed before it was started", nrExecutions.tryAcquire(2, SECONDS));
            assertThatEventually(() -> jobSchedulerService.findAllJobs().isEmpty());
        });
    }

    @Test
    public void scheduleJobWithDelay_stopAndStartProcess_jobIsExecutedAfterDelay() throws Exception {
        withRunningJobs(() -> {
            jobSchedulerService.schedule(new SuccessfulJobRunner(), Instant.now().plus(Duration.ofSeconds(1)));
            assertThatEventually(() -> !jobSchedulerService.findAllJobs().isEmpty());
        });

        withRunningJobs(() -> assertThatEventually(() -> jobSchedulerService.findAllJobs().isEmpty()));
    }

    private void withRunningJobs(RunnableThrowingException runnable) throws Exception {
        jobSchedulerService.startProcessingJobs();
        try {
            runnable.run();
        } finally {
            jobSchedulerService.stopProcessingJobs();
        }
    }

    private void assertThatEventually(Supplier<Boolean> condition) throws InterruptedException {
        long endTime = System.currentTimeMillis() + 30_000;
        while (!condition.get() && System.currentTimeMillis() < endTime) {
            Thread.sleep(10);
        }
        assert(condition.get());
    }

    private static class SuccessfulJobRunner implements Runnable {
        @Override
        public void run() {
            nrExecutions.release();
            lastJobExecutedAt = Instant.now();
        }
    }

    private static class FailingJobRunner implements Runnable {
        @Override
        public void run() {
            nrExecutions.release();
            lastJobExecutedAt = Instant.now();
            throw new RuntimeException("Something went wrong while running the job");
        }
    }
}