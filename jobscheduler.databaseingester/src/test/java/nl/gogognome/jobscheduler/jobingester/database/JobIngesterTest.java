package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobScheduler;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class JobIngesterTest {

    private JobScheduler jobScheduler = mock(JobScheduler.class);
    private JobCommandDAO jobCommandDAO = mock(JobCommandDAO.class);
    private JobIngester jobIngester = new JobIngester(jobScheduler, jobCommandDAO);

    private List<JobCommand> jobCommandsInDatabase = new ArrayList<>();

    @Before
    public void initMocks() throws SQLException {
        when(jobCommandDAO.findJobCommands()).thenReturn(jobCommandsInDatabase);

        doAnswer(invocationOnMock -> { ((Runnable)invocationOnMock.getArguments()[0]).run(); return null; })
                .when(jobScheduler).runBatch(any(Runnable.class));
    }

    @Test
    public void ingestJobs_zeroJobCommands_noCommandsForwardedToScheduler() throws SQLException {
        jobCommandsInDatabase.clear();

        jobIngester.ingestJobs();

        verify(jobScheduler, never()).schedule(any(Job.class));
        verify(jobScheduler, never()).reschedule(any(Job.class));
        verify(jobScheduler, never()).jobFinished(any(String.class));
        verify(jobScheduler, never()).jobFailed(any(String.class));
        verify(jobCommandDAO).deleteJobCommands(jobCommandsInDatabase);
    }

    @Test
    public void ingestJobs_oneScheduleCommand_schedulesJobAndNoOtherCommandsForwardedToSchedulerAndJobDeletedFromDatabase() throws SQLException {
        Job job = buildJob();
        jobCommandsInDatabase.add(new JobCommand(Command.SCHEDULE, job));

        jobIngester.ingestJobs();

        verify(jobScheduler).schedule(eq(job));
        verify(jobScheduler, never()).reschedule(any(Job.class));
        verify(jobScheduler, never()).jobFinished(any(String.class));
        verify(jobScheduler, never()).jobFailed(any(String.class));
        verify(jobCommandDAO).deleteJobCommands(jobCommandsInDatabase);
    }

    @Test
    public void ingestJobs_fiveJobCommandsWithDifferentCommands_eachOfTheCommandsAreHandledAndJobCommandsArDeletedFromDatabase() throws SQLException {
        JobCommand jobCommand1 = JobCommandBuilder.buildJob("1", Command.SCHEDULE);
        jobCommandsInDatabase.add(jobCommand1);
        JobCommand jobCommand2 = JobCommandBuilder.buildJob("2", Command.RESCHEDULE);
        jobCommandsInDatabase.add(jobCommand2);
        JobCommand jobCommand3 = JobCommandBuilder.buildJob("3", Command.JOB_FINISHED);
        jobCommandsInDatabase.add(jobCommand3);
        JobCommand jobCommand4 = JobCommandBuilder.buildJob("4", Command.JOB_FAILED);
        jobCommandsInDatabase.add(jobCommand4);
        JobCommand jobCommand5 = JobCommandBuilder.buildJob("5", Command.REMOVE);
        jobCommandsInDatabase.add(jobCommand5);

        jobIngester.ingestJobs();

        verify(jobScheduler).schedule(eq(jobCommand1.getJob()));
        verify(jobScheduler).reschedule(eq(jobCommand2.getJob()));
        verify(jobScheduler).jobFinished(eq(jobCommand3.getJob().getId()));
        verify(jobScheduler).jobFailed(eq(jobCommand4.getJob().getId()));
        verify(jobScheduler).remove(eq(jobCommand5.getJob().getId()));
        verify(jobCommandDAO).deleteJobCommands(jobCommandsInDatabase);
    }

    @Test
    public void ingestJobs_ingestingThrowsException_jobSchedulerReloadsPersistedJobs() {
        Job job = buildJob();
        jobCommandsInDatabase.add(new JobCommand(Command.SCHEDULE, job));
        String message = "Failed to add job";
        doThrow(new RuntimeException(message)).when(jobScheduler).schedule(job);

        try {
            jobIngester.ingestJobs();
            fail("Expected exception was not thrown");
        } catch (RuntimeException e) {
            assertEquals(message, e.getMessage());
        }

        verify(jobScheduler).loadPersistedJobs();
    }

    private Job buildJob() {
        return new Job("1", "someType", null, Instant.now());
    }
}
