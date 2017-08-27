package nl.gogognome.jobscheduler.runnablejobfinder;

import nl.gogognome.jobscheduler.scheduler.*;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.ZERO;
import static org.junit.Assert.*;

public class FifoRunnableJobFinderTest {

    private FifoRunnableJobFinder fifoRunnableJobFinder = new FifoRunnableJobFinder();
    private int nextJobId = 1;

    @Test
    public void noJobsPresent_findById_returnsNull() {
        assertNull(fifoRunnableJobFinder.findById("1"));
    }

    @Test
    public void jobsPresent_findById_returnsJob() {
        Job job = new Job("1");
        ScheduledJob scheduledJob = new ScheduledJob(job);
        fifoRunnableJobFinder.addJob(scheduledJob);

        assertSame(scheduledJob, fifoRunnableJobFinder.findById(job.getId()));
    }

    @Test
    public void multipleJobsPresent_findById_returnsJob() {
        Job job1 = new Job("1");
        Job job2 = new Job("2");
        ScheduledJob scheduledJob1 = new ScheduledJob(job1);
        ScheduledJob scheduledJob2 = new ScheduledJob(job2);
        fifoRunnableJobFinder.addJob(scheduledJob1);
        fifoRunnableJobFinder.addJob(scheduledJob2);

        assertSame(scheduledJob1, fifoRunnableJobFinder.findById(job1.getId()));
        assertSame(scheduledJob2, fifoRunnableJobFinder.findById(job2.getId()));
    }

    @Test
    public void otherJobIsPresent_findById_returnsNull() {
        Job job = new Job("1");
        ScheduledJob scheduledJob = new ScheduledJob(job);
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
        ScheduledJob scheduledJob = buildScheduledJob(ZERO);

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_threeJobsWithoutStartTime_returnsFirstScheduledJob() {
        ScheduledJob scheduledJob0 = buildScheduledJob(ZERO);
        ScheduledJob scheduledJob1 = buildScheduledJob(ZERO);
        ScheduledJob scheduledJob2 = buildScheduledJob(ZERO);

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob0, scheduledJob1, scheduledJob2);

        assertSame(scheduledJob0, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_oneJobWitStartTimeInPast_returnsJob() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofSeconds(-1));

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_oneJobWitStartTimeInFuture_returnsNull() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(1));

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

        assertNull(nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_threeJobs_oldestHasScheduledAtTimeInFuture_returnsSecondOldestJob() {
        ScheduledJob scheduledJob0 = buildScheduledJob(Duration.ofMinutes(1));
        ScheduledJob scheduledJob1 = buildScheduledJob(ZERO);
        ScheduledJob scheduledJob2 = buildScheduledJob(ZERO);

        ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob1, scheduledJob0, scheduledJob2);

        assertSame(scheduledJob1, nextRunnableScheduledJob);
    }

    @Test
    public void getNextRunnableScheduledJob_noJobWithStateNotIdle_returnsNull() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(0));

        for (JobState state : JobState.values()) {
            fifoRunnableJobFinder = new FifoRunnableJobFinder();
            if (state != JobState.IDLE) {
                scheduledJob.setState(state);

                ScheduledJob nextRunnableScheduledJob = getNextRunnableScheduledJob(scheduledJob);

                assertNull("Should not return runnable job for state " + state, nextRunnableScheduledJob);
            }
        }
    }

    @Test
    public void addJob_findNextRunnableScheduledJob_returnsJob() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(0));
        fifoRunnableJobFinder.addJob(scheduledJob);

        ScheduledJob nextRunnableScheduledJob = fifoRunnableJobFinder.findNextRunnableJob();

        assertSame(scheduledJob, nextRunnableScheduledJob);
    }

    @Test
    public void addJob_addSameJobTwice_shouldFail() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(0));
        fifoRunnableJobFinder.addJob(scheduledJob);
        try {
            fifoRunnableJobFinder.addJob(scheduledJob);
            fail("Expected exception was not thrown");
        } catch (DuplicateJobException e) {
            assertEquals("A job with id 1 already exists. Jobs must have a unique id!", e.getMessage());
        }
    }

    @Test
    public void updateJob_nonExistingJob_shouldFail() {
        try {
            ScheduledJob job = new ScheduledJob(new Job(Integer.toString(nextJobId++)));
            fifoRunnableJobFinder.updateJob(job);
            fail("Expected exception was not thrown");
        } catch (UnknownJobException e) {
            assertEquals("A job with id 1 does not exist. Only existing jobs can be updated!", e.getMessage());
        }
    }

    @Test
    public void updateJob_updateExistingJob_findNextRunnableScheduledJobReturnsUpdatedJob() {
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(0));
        fifoRunnableJobFinder.addJob(scheduledJob);

        Job updateJob = new Job(scheduledJob.getJob().getId());
        updateJob.setType("Updated job");
        ScheduledJob updateScheduledJob = new ScheduledJob(updateJob);
        updateScheduledJob.setState(JobState.IDLE);
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
        ScheduledJob scheduledJob = buildScheduledJob(Duration.ofMinutes(0));
        fifoRunnableJobFinder.addJob(scheduledJob);

        fifoRunnableJobFinder.removeJob(scheduledJob.getJob().getId());

        assertNull(fifoRunnableJobFinder.findNextRunnableJob());
    }

    @Test
    public void multipleJobsPresent_removeAllScheduledJobs_noJobsPresent() {
        Job job1 = new Job("1");
        Job job2 = new Job("2");
        ScheduledJob scheduledJob1 = new ScheduledJob(job1);
        ScheduledJob scheduledJob2 = new ScheduledJob(job2);
        fifoRunnableJobFinder.addJob(scheduledJob1);
        fifoRunnableJobFinder.addJob(scheduledJob2);

        fifoRunnableJobFinder.removeAllScheduledJobs();

        assertNull(fifoRunnableJobFinder.findById(job1.getId()));
        assertNull(fifoRunnableJobFinder.findById(job2.getId()));
    }


    private ScheduledJob buildScheduledJob(Duration startInstantOffset) {
        Job job = new Job(Integer.toString(nextJobId++));
        Instant now = Instant.now();
        job.setScheduledAtInstant(now.plus(startInstantOffset));
        ScheduledJob scheduledJob = new ScheduledJob(job);
        scheduledJob.setState(JobState.IDLE);
        return scheduledJob;
    }

    private ScheduledJob getNextRunnableScheduledJob(ScheduledJob... scheduledJobs) {
        for (ScheduledJob scheduledJob : scheduledJobs) {
            fifoRunnableJobFinder.addJob(scheduledJob);
        }
        return fifoRunnableJobFinder.findNextRunnableJob();
    }
}