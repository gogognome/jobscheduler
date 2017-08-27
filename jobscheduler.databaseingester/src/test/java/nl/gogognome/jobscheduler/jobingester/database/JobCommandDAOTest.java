package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.dataaccess.dao.QueryBuilder;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.dataaccess.transaction.RequireTransaction;
import nl.gogognome.jobscheduler.scheduler.Job;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JobCommandDAOTest {

    private JobIngesterProperties properties = new JobIngesterProperties();
    @SuppressWarnings("unused")
    private Connection connectionToKeepInMemoryDatabaseAlive;
    private JobCommandDAO jobCommandDAO;

    @Before
    public void setupInMemoryDatabase() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID();
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(jdbcUrl);
        connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();
        CompositeDatasourceTransaction.registerDataSource(properties.getConnectionName(), dataSource);

        NewTransaction.runs(() -> new DatabaseMigratorDAO(properties.getConnectionName()).applyMigrationsFromResource("/database/_migrations.txt"));

        jobCommandDAO = new JobCommandDAO(properties);
    }

    @After
    public void removeInMemoryDatabase() {
        connectionToKeepInMemoryDatabaseAlive = null;
    }

    @Test
    public void findAll_zeroJobsInDatabase_returnsEmptyList() throws SQLException {
        NewTransaction.runs(() -> {
            List<JobCommand> jobCommands = jobCommandDAO.findJobCommands();

            assertEquals(emptyList(), jobCommands);
        });
    }

    @Test
    public void findAll_oneJobsInDatabase_returnsOneJob() throws SQLException {
        JobCommand jobCommand = JobCommandBuilder.buildJob("1", Command.SCHEDULE);

        NewTransaction.runs(() -> {
            jobCommandDAO.create(jobCommand);
            List<JobCommand> jobCommands = jobCommandDAO.findJobCommands();

            assertEquals(1, jobCommands.size());
            assertEquals(jobCommand.getCommand(), jobCommands.get(0).getCommand());
            Job job = jobCommand.getJob();
            Job retrievedJob1 = jobCommands.get(0).getJob();
            assertEquals(job.getId(), retrievedJob1.getId());
            assertEquals(job.getType(), retrievedJob1.getType());
            assertArrayEquals(job.getData(), retrievedJob1.getData());
        });
    }

    @Test
    public void findAll_twoJobCommandsPresentAndSelectJobCommandsQueryOnlyGetsFirstCommand_getsFirstRow() throws SQLException {
        properties.setSelectJobCommandsQuery("SELECT * FROM " + properties.getTableName() + " LIMIT 1");
        NewTransaction.runs(() -> {
            JobCommand jobCommand1 = JobCommandBuilder.buildJob("1", Command.SCHEDULE);
            jobCommandDAO.create(jobCommand1);
            JobCommand jobCommand2 = JobCommandBuilder.buildJob("2", Command.RESCHEDULE);
            jobCommandDAO.create(jobCommand2);

            List<JobCommand> jobCommands = jobCommandDAO.findJobCommands();

            assertEquals(1, jobCommands.size());
            assertEquals(jobCommand1.getCommand(), jobCommands.get(0).getCommand());
            assertEquals(jobCommand1.getJob(), jobCommands.get(0).getJob());
        });
    }

    @Test
    public void delete_zeroJobCommandsToDelete_deletesNothing() {
        NewTransaction.runs(() -> {
            jobCommandDAO = new JobCommandDAO(properties) {
                protected QueryBuilder execute(String sqlStatement, Object... parameters) {
                    fail("This method should not have been called");
                    return null;
                }
            };

            jobCommandDAO.deleteJobCommands(emptyList());
        });
    }

    @Test
    public void delete_oneJobCommandToDelete_deletesJobCommand() throws SQLException {
        NewTransaction.runs(() -> {
            JobCommand jobCommand = JobCommandBuilder.buildJob("1", Command.SCHEDULE);
            jobCommandDAO.create(jobCommand);

            jobCommandDAO.deleteJobCommands(singletonList(jobCommand));

            assertNoJobCommandsPresent();
        });
    }


    @Test
    public void delete_twoJobCommandsToDelete_deletesJobCommands() throws SQLException {
        NewTransaction.runs(() -> {
            JobCommand jobCommand1 = JobCommandBuilder.buildJob("1", Command.SCHEDULE);
            jobCommandDAO.create(jobCommand1);
            JobCommand jobCommand2 = JobCommandBuilder.buildJob("2", Command.RESCHEDULE);
            jobCommandDAO.create(jobCommand2);

            jobCommandDAO.deleteJobCommands(asList(jobCommand1, jobCommand2));

            assertNoJobCommandsPresent();
        });
    }

    @Test
    public void delete_nonExistingJobCommand_shouldFail() {
        NewTransaction.runs(() -> {
            JobCommand jobCommand = JobCommandBuilder.buildJob("1", Command.SCHEDULE);

            try {
                jobCommandDAO.deleteJobCommands(singletonList(jobCommand));
                fail("Expected exception not thrown");
            } catch (SQLException e) {
                assertEquals("Deleted 0 from the 1 job commands!", e.getMessage());
            }
        });
    }

    @Test
    public void allCommandsShouldFitInCommandColumn() throws SQLException {
        NewTransaction.runs(() -> {
            int nextId = 1;
            for (Command command : Command.values()) {
                jobCommandDAO.create(JobCommandBuilder.buildJob(Integer.toString(nextId++), command));
            }
        });
    }

    private void assertNoJobCommandsPresent() {
        RequireTransaction.runs(() -> assertEquals(0, jobCommandDAO.count(null)));
    }
}
