package nl.gogognome.jobschedulerservice;

import com.zaxxer.hikari.HikariDataSource;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.jobingester.database.JobIngesterProperties;
import nl.gogognome.jobscheduler.jobpersister.database.DatabaseJobPersisterProperties;
import nl.gogognome.jobscheduler.runnablejobfinder.FifoRunnableJobFinder;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static nl.gogognome.test.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;

public class JobSchedulerServiceTest {

    private JobSchedulerService jobSchedulerService;

    private static volatile int nrExecutions;

    @Before
    public void initDataSource() {
        JobIngesterProperties jobIngesterProperties = new JobIngesterProperties();
        DatabaseJobPersisterProperties databaseJobPersisterProperties = new DatabaseJobPersisterProperties();

        HikariDataSource dataSource = buildDataSource();
        CompositeDatasourceTransaction.registerDataSource(jobIngesterProperties.getConnectionName(), dataSource);
        CompositeDatasourceTransaction.registerDataSource(databaseJobPersisterProperties.getConnectionName(), dataSource);
        jobSchedulerService = new JobSchedulerService(new FifoRunnableJobFinder(), jobIngesterProperties, databaseJobPersisterProperties, 4);

        initDatabase(jobIngesterProperties);

        nrExecutions = 0;
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
    public void start_runJob_stop_succeeds() throws Exception {
        SuccessfulJobRunner successfulJobRunner = new SuccessfulJobRunner();
        jobSchedulerService.startProcessingJobs();
        jobSchedulerService.schedule(successfulJobRunner);
        waitFor(() -> successfulJobRunner.getNrExecutions() >= 1);
        jobSchedulerService.stopProcessingJobs();

        assertEquals(1, successfulJobRunner.getNrExecutions());
    }

    @Test
    public void start_runTenJobs_stop_succeeds() throws Exception {
        int nrJobs = 10;
        SuccessfulJobRunner successfulJobRunner = new SuccessfulJobRunner();
        jobSchedulerService.startProcessingJobs();
        for (int i=0; i<10; i++) {
            jobSchedulerService.schedule(successfulJobRunner);
        }
        waitFor(() -> successfulJobRunner.getNrExecutions() >= nrJobs);
        jobSchedulerService.stopProcessingJobs();

        assertEquals(nrJobs, successfulJobRunner.getNrExecutions());
    }

    private void waitFor(Supplier<Boolean> condition) throws InterruptedException {
        long endTime = System.currentTimeMillis() + 30_000;
        while (!condition.get() && System.currentTimeMillis() < endTime) {
            Thread.sleep(10);
        }
    }

    private static class SuccessfulJobRunner implements Runnable {

        @Override
        public void run() {
            nrExecutions++;
        }

        public int getNrExecutions() {
            return nrExecutions;
        }
    }
}