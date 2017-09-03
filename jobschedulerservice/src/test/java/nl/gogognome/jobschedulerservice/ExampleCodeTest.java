package nl.gogognome.jobschedulerservice;

import com.zaxxer.hikari.HikariDataSource;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.jobingester.database.JobIngesterProperties;
import nl.gogognome.jobscheduler.jobpersister.database.DatabaseJobPersisterProperties;
import nl.gogognome.jobscheduler.runnablejobfinder.FifoRunnableJobFinder;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.Instant;

public class ExampleCodeTest {

    @Test
    public void exampleCode() throws Exception {
        // Initialize properties. Modify default values if needed.
        JobIngesterProperties jobIngesterProperties = new JobIngesterProperties();
        DatabaseJobPersisterProperties databaseJobPersisterProperties = new DatabaseJobPersisterProperties();

        // Register data source so that it can be used by the job ingester and database job persister
        DataSource dataSource = buildDataSource(); // DON'T SHOW buildDataSource() IN EXAMPLE CODE
        CompositeDatasourceTransaction.registerDataSource(jobIngesterProperties.getConnectionName(), dataSource);
        CompositeDatasourceTransaction.registerDataSource(databaseJobPersisterProperties.getConnectionName(), dataSource);
        initDatabase(jobIngesterProperties); // DON'T SHOW IN EXAMPLE CODE

        // Creste job scheduler service with maximum 4 threads for exeuting jobs
        JobSchedulerService jobSchedulerService = new JobSchedulerService(new FifoRunnableJobFinder(), jobIngesterProperties, databaseJobPersisterProperties, 4);

        // Start the processing of jobs
        jobSchedulerService.startProcessingJobs();

        System.out.println("Current time: " + Instant.now());
        // Schedule job to execute immediately
        jobSchedulerService.schedule(new HelloWorldJob());
        // Schedule job to execute after 5 seconds
        jobSchedulerService.schedule(new HelloWorldJob(), Instant.now().plusSeconds(5));

        // Wait till both jobs have been executed
        Thread.sleep(6_000);

        // Stop the processing of jobs
        jobSchedulerService.stopProcessingJobs();
    }

    private static class HelloWorldJob implements Runnable {
        @Override
        public void run() {
            System.out.println("Hello World! The time is: " + Instant.now());
        }
    }

    private DataSource buildDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:" + Thread.currentThread().getName() + "-" + System.nanoTime());
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void initDatabase(JobIngesterProperties jobIngesterProperties) {
        NewTransaction.runs(() -> new DatabaseMigratorDAO(jobIngesterProperties.getConnectionName()).applyMigrationsFromResource("/database/_migrations.txt"));
    }

}
