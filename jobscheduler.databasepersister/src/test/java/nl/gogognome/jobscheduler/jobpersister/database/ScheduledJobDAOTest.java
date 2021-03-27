package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class ScheduledJobDAOTest {

    private final DatabaseJobPersisterProperties properties = new DatabaseJobPersisterProperties();
    @SuppressWarnings("unused")
    private Connection connectionToKeepInMemoryDatabaseAlive;
    private ScheduledJobDAO scheduledJobDAO;

    @Before
    public void setupInMemoryDatabase() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID();
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(jdbcUrl);
        connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();
        CompositeDatasourceTransaction.registerDataSource(properties.getConnectionName(), dataSource);

        NewTransaction.runs(() -> new DatabaseMigratorDAO(properties.getConnectionName()).applyMigrationsFromResource("/database/_migrations.txt"));

        scheduledJobDAO = new ScheduledJobDAO(properties);
    }

    @After
    public void removeInMemoryDatabase() {
        connectionToKeepInMemoryDatabaseAlive = null;
    }

    @Test
    public void createJobAndReadItBack_shouldGetSameJobAgain() {
        ScheduledJob scheduledJob = ScheduledJobBuilder.build("1");

        NewTransaction.runs(() -> {
            scheduledJobDAO.create(scheduledJob);

            ScheduledJob readScheduledJob = scheduledJobDAO.get(scheduledJob.getJob().getId());
            assertNotNull(readScheduledJob);
            assertEquals(scheduledJob.getJob().getId(), readScheduledJob.getJob().getId());
            assertEquals(scheduledJob.getJob().getType(), readScheduledJob.getJob().getType());
            assertArrayEquals(scheduledJob.getJob().getData(), readScheduledJob.getJob().getData());
            assertEquals(scheduledJob.getJob().getScheduledAtInstant(), readScheduledJob.getJob().getScheduledAtInstant());
            assertEquals(scheduledJob.getState(), readScheduledJob.getState());
            assertEquals(scheduledJob.getRequesterId(), readScheduledJob.getRequesterId());
            assertEquals(scheduledJob.getTimeoutAtInstant(), readScheduledJob.getTimeoutAtInstant());
        });
    }

    @Test
    public void findAll_noJobsCreated_returnsEmptyList() {
        List<ScheduledJob> scheduledJobs = NewTransaction.returns(() -> scheduledJobDAO.findAll());

        assertTrue(scheduledJobs.isEmpty());
    }

    @Test
    public void findAll_oneJobCreated_returnsJob() {
        ScheduledJob scheduledJob = NewTransaction.returns(() -> scheduledJobDAO.create(ScheduledJobBuilder.build("1")));

        List<ScheduledJob> scheduledJobs = NewTransaction.returns(() -> scheduledJobDAO.findAll());

        assertEquals(singletonList(scheduledJob), scheduledJobs);
    }

    @Test
    public void findAll_multipleJobsCreated_returnsJobs() {
        ScheduledJob scheduledJob1 = NewTransaction.returns(() -> scheduledJobDAO.create(ScheduledJobBuilder.build("1")));
        ScheduledJob scheduledJob2 = NewTransaction.returns(() -> scheduledJobDAO.create(ScheduledJobBuilder.build("2")));
        ScheduledJob scheduledJob3 = NewTransaction.returns(() -> scheduledJobDAO.create(ScheduledJobBuilder.build("3")));

        List<ScheduledJob> scheduledJobs = NewTransaction.returns(() -> scheduledJobDAO.findAll());

        assertEquals(asList(scheduledJob1, scheduledJob2, scheduledJob3), scheduledJobs);
    }
}
