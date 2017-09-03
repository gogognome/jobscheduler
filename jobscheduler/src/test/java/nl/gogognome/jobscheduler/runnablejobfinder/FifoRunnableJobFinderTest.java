package nl.gogognome.jobscheduler.runnablejobfinder;

import nl.gogognome.jobscheduler.ScheduledJobFakes;
import nl.gogognome.jobscheduler.scheduler.*;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.ZERO;
import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;
import static org.junit.Assert.*;

public class FifoRunnableJobFinderTest {

    private FifoRunnableJobFinder fifoRunnableJobFinder = new FifoRunnableJobFinder();

    @Test
    public void noJobsPresent_findById_returnsNull() {
        assertNull(fifoRunnableJobFinder.findById("1"));
    }

    @Test
    public void jobsPresent_findById_returnsJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();

        fifoRunnableJobFinder.addJob(scheduledJob);

        assertSame(scheduledJob, fifoRunnableJobFinder.findById(scheduledJob.getJob().getId()));
    }

    @Test
    public void multipleJobsPresent_findById_returnsJob() {
        ScheduledJob scheduledJob1 = ScheduledJobFakes.defaultIdleJob();
        ScheduledJob scheduledJob2 = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob1);
        fifoRunnableJobFinder.addJob(scheduledJob2);

        assertSame(scheduledJob1, fifoRunnableJobFinder.findById(scheduledJob1.getJob().getId()));
        assertSame(scheduledJob2, fifoRunnableJobFinder.findById(scheduledJob2.getJob().getId()));
    }

    @Test
    public void otherJobIsPresent_findById_returnsNull() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob);

        assertNull(fifoRunnableJobFinder.findById("non-existing"));
    }

    @Test
    public void getNextRunnableScheduledJob_noJobsPresent_returnsNull() {
        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(/* no jobs */);

        assertNull(nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_oneJobWithoutStartTime_returnsJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_threeJobsWithoutStartTime_returnsFirstScheduledJob() {
        ScheduledJob scheduledJob0 = ScheduledJobFakes.defaultIdleJob();
        ScheduledJob scheduledJob1 = ScheduledJobFakes.defaultIdleJob();
        ScheduledJob scheduledJob2 = ScheduledJobFakes.defaultIdleJob();

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob0, scheduledJob1, scheduledJob2);

        assertSame(scheduledJob0, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_oneJobWitStartTimeInPast_returnsJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJobStartingAfter(Duration.ofSeconds(-1));

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_oneJobWitStartTimeInFuture_returnsNull() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJobStartingAfter(Duration.ofMinutes(1));

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertNull(nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_threeJobs_oldestHasScheduledAtTimeInFuture_returnsSecondOldestJob() {
        ScheduledJob scheduledJob0 = ScheduledJobFakes.defaultIdleJobStartingAfter(Duration.ofMinutes(1));
        ScheduledJob scheduledJob1 = ScheduledJobFakes.defaultIdleJobStartingAfter(ZERO);
        ScheduledJob scheduledJob2 = ScheduledJobFakes.defaultIdleJobStartingAfter(ZERO);

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob1, scheduledJob0, scheduledJob2);

        assertSame(scheduledJob1, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_noJobWithStateNotIdle_returnsNull() {
        for (JobState state : JobState.values()) {
            fifoRunnableJobFinder = new FifoRunnableJobFinder();
            if (state != IDLE) {
                ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJobStartingAfter(Duration.ofMinutes(0), state);

                ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

                assertNull("Should not return runnable job for state " + state, nextRunnableScheduledJob);
            }
        }
    }

    @Test
    public void addJob_findNextRunnableScheduledJob_returnsJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob);

        ScheduledJob nextRunnableScheduledJob = fifoRunnableJobFinder.findNextRunnableJob();

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void addJob_addSameJobTwice_shouldFail() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob);
        try {
            fifoRunnableJobFinder.addJob(scheduledJob);
            fail("Expected exception was not thrown");
        } catch (DuplicateJobException e) {
            assertEquals("A job with id " + scheduledJob.getJob().getId() + " already exists. Jobs must have a unique id!", e.getMessage());
        }
    }

    @Test
    public void updateJob_nonExistingJob_shouldFail() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        try {
            fifoRunnableJobFinder.updateJob(scheduledJob);
            fail("Expected exception was not thrown");
        } catch (UnknownJobException e) {
            assertEquals("A job with id " + scheduledJob.getJob().getId() + " does not exist. Only existing jobs can be updated!", e.getMessage());
        }
    }

    @Test
    public void updateJob_updateExistingJob_findNextRunnableScheduledJobReturnsUpdatedJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob);

        ScheduledJob updateScheduledJob = ScheduledJobFakes.with(new Job(scheduledJob.getJob().getId(), "Updated job", null, Instant.now()));
        fifoRunnableJobFinder.updateJob(updateScheduledJob);

        ScheduledJob runnableJob = fifoRunnableJobFinder.findNextRunnableJob();
        assertSame(updateScheduledJob, runnableJob);
    }

    @Test
    public void removeJob_noJobPresent_shouldFail() {
        try {
            fifoRunnableJobFinder.removeJob("two");
            fail("Expected exception was not thrown");
        } catch (UnknownJobException e) {
            assertEquals("Cannot remove job with id two because it does not exist!", e.getMessage());
        }
    }

    @Test
    public void removeJob_removeOnlyPresentJob_findNextRunnableScheduledJobReturnsNull() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob);

        fifoRunnableJobFinder.removeJob(scheduledJob.getJob().getId());

        assertNull(fifoRunnableJobFinder.findNextRunnableJob());
    }

    @Test
    public void multipleJobsPresent_removeAllScheduledJobs_noJobsPresent() {
        ScheduledJob scheduledJob1 = ScheduledJobFakes.defaultIdleJob();
        ScheduledJob scheduledJob2 = ScheduledJobFakes.defaultIdleJob();
        fifoRunnableJobFinder.addJob(scheduledJob1);
        fifoRunnableJobFinder.addJob(scheduledJob2);

        fifoRunnableJobFinder.removeAllScheduledJobs();

        assertNull(fifoRunnableJobFinder.findById(scheduledJob1.getJob().getId()));
        assertNull(fifoRunnableJobFinder.findById(scheduledJob2.getJob().getId()));
    }


    private ScheduledJob getNextRunnableScheduledJob(ScheduledJob... scheduledJobs) {
        for (ScheduledJob scheduledJob : scheduledJobs) {
            fifoRunnableJobFinder.addJob(scheduledJob);
        }
        return fifoRunnableJobFinder.findNextRunnableJob();
    }
}