package nl.gogognome.jobscheduler.scheduler;

import nl.gogognome.jobscheduler.JobFakes;
import nl.gogognome.jobscheduler.ScheduledJobFakes;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static nl.gogognome.jobscheduler.scheduler.JobState.*;
import static nl.gogognome.test.AssertExtensions.assertThrows;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JobSchedulerTest {

    private RunnableJobFinder runnableJobFinder = mock(RunnableJobFinder.class);
    private JobPersister jobPersister = mock(JobPersister.class);
    private JobScheduler jobScheduler = new JobScheduler(runnableJobFinder, jobPersister);

    @Test
    public void schedule_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.schedule(null));
    }

    @Test
    public void schedule_addsJobToJobFinderAndPersistsJob() {
        Job job = JobFakes.defaultJob();
        jobScheduler.schedule(job);

        ArgumentCaptor<ScheduledJob> argumentCaptor = ArgumentCaptor.forClass(ScheduledJob.class);
        verify(runnableJobFinder).addJob(argumentCaptor.capture());
        ScheduledJob createdScheduledJob = argumentCaptor.getValue();
        assertSame(job, createdScheduledJob.getJob());
        assertEquals(IDLE, createdScheduledJob.getState());
        assertNull(createdScheduledJob.getRequesterId());

        verify(jobPersister).create(createdScheduledJob);
    }

    @Test
    public void jobFinished_existingRunningJob_jobIsRemoved() {
        ScheduledJob scheduledJob = scheduleRunningJob();
        String jobId = scheduledJob.getJob().getId();

        jobScheduler.jobFinished(jobId);

        verify(runnableJobFinder).removeJob(jobId);
        verify(jobPersister).remove(jobId);
    }

    @Test
    public void stopSuccessfully_null_thorwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.jobFinished(null));
    }

    @Test
    public void stopSuccessfully_existingNotRunningJob_throwsException() {
        ScheduledJob scheduledJob = scheduleIdleJob();
        String jobId = scheduledJob.getJob().getId();

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.jobFinished(jobId));

        verify(runnableJobFinder, never()).removeJob(jobId);
        verify(jobPersister, never()).remove(jobId);
    }

    @Test
    public void stopSuccessfully_notExistingJob_throwsException() {
        String nonExistentJobId = "bla";
        assertThrows(UnknownJobException.class, () -> jobScheduler.jobFinished(nonExistentJobId));

        verify(runnableJobFinder, never()).removeJob(nonExistentJobId);
        verify(jobPersister, never()).remove(nonExistentJobId);
    }

    @Test
    public void jobFailed_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.jobFailed(null));
    }

    @Test
    public void jobFailed_existingRunningJob_jobIsUpdated() {
        ScheduledJob scheduledJob = scheduleRunningJob();

        jobScheduler.jobFailed(scheduledJob.getJob().getId());

        ScheduledJob updatedScheduledJob = getUpdatedScheduledJob();
        assertEquals(ERROR, updatedScheduledJob.getState());
        assertNull(updatedScheduledJob.getRequesterId());
    }

    @Test
    public void jobFailed_existingNotRunningJob_throwsException() {
        ScheduledJob scheduledJob = scheduleIdleJob();
        String jobId = scheduledJob.getJob().getId();

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.jobFailed(jobId));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }


    @Test
    public void jobFailed_notExistingJob_throwsException() {
        assertThrows(UnknownJobException.class, () -> jobScheduler.jobFailed("bla"));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }

    @Test
    public void reschedule_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.reschedule(null));
    }

    @Test
    public void reschedule_existingRunningJob_jobIsRescheduled() {
        ScheduledJob scheduledJob = scheduleRunningJob();

        jobScheduler.reschedule(scheduledJob.getJob());

        ScheduledJob actualScheduledJob = getUpdatedScheduledJob();
        assertEquals(IDLE, actualScheduledJob.getState());
        assertNull(actualScheduledJob.getRequesterId());
    }

    @Test
    public void reschedule_notExistingJob_throwsException() {
        assertThrows(UnknownJobException.class, () -> jobScheduler.reschedule(JobFakes.defaultJob()));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }

    @Test
    public void reschedule_existingNotRunningJob_throwsException() {
        ScheduledJob scheduledJob = scheduleIdleJob();

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.reschedule(scheduledJob.getJob()));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }

    @Test
    public void tryStartNextRunnableJob_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.tryStartNextRunnableJob(null));
    }

    @Test
    public void tryStartNextRunnableJob_noJobsAdded_ReturnsNull() {
        Job startedJob = jobScheduler.tryStartNextRunnableJob("tester");
        setupNextRunnableJob(null);

        assertNull(startedJob);
    }

    @Test
    public void tryStartNextRunnableJob_idleJobIsChosen_ReturnsJobWithStateUpdated() {
        ScheduledJob scheduledJob = scheduleIdleJob();
        setupNextRunnableJob(scheduledJob);

        Job startedJob = jobScheduler.tryStartNextRunnableJob("tester");

        assertJobIsStarted(startedJob);
    }

    @Test
    public void tryStartNextRunnableJob_noneIdleJobChosen_Fails() {
        ScheduledJob scheduledJob = scheduleRunningJob();
        setupNextRunnableJob(scheduledJob);

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.tryStartNextRunnableJob("tester"));
        verify(runnableJobFinder, never()).updateJob(scheduledJob);
        verify(jobPersister, never()).update(scheduledJob);
    }

    @Test
    public void startNextRunnableJob_idleJobPresent_ReturnsJobWithStateUpdated() throws InterruptedException {
        ScheduledJob scheduledJob = scheduleIdleJob();
        setupNextRunnableJob(scheduledJob);

        Job startedJob = jobScheduler.startNextRunnableJob("tester", 1000L);

        assertJobIsStarted(startedJob);
    }

    @Test
    public void startNextRunnableJob_noJobsAdded_TimesOut() throws InterruptedException {
        long timeout = 1000L;
        long expectedEndTime = System.currentTimeMillis() + timeout;
        Job job = jobScheduler.startNextRunnableJob("tester", timeout);

        assertNull(job);
        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
        assertTrue(System.currentTimeMillis() >= expectedEndTime);
    }

    @Test
    public void startNextRunnableJob_invalidParameter_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.startNextRunnableJob(null, 123));
        assertThrows(IllegalArgumentException.class, () -> jobScheduler.startNextRunnableJob("tester", -1));
    }

    @Test
    public void startNextRunnableJob_firstJobAddedAfterOneSecond_ReturnsJobWithStateUpdateAfterOneSecond() throws InterruptedException {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        long start = System.currentTimeMillis();
        long jobAddDelay = 1000;
        long timeout = jobAddDelay + 3000;
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(() -> {
            try {
                Thread.sleep(jobAddDelay);
                scheduleJob(scheduledJob);
                setupNextRunnableJob(scheduledJob);
                jobScheduler.schedule(scheduledJob.getJob());
            } catch (InterruptedException e) {
                // ignore exception
            }
        });

        Job startedJob = jobScheduler.startNextRunnableJob("tester", timeout);

        assertJobIsStarted(startedJob);
        assertTrue(System.currentTimeMillis() >= start + jobAddDelay);
        assertTrue(System.currentTimeMillis() < start + timeout);
    }

    @Test
    public void runBatch_runsAction() {
        Runnable action = mock(Runnable.class);

        jobScheduler.runBatch(action);

        verify(action).run();
    }

    @Test
    public void loadPersistedJobs_replacesJobsInRunnableJobFinderByPersistedJobs() {
        ScheduledJob scheduledJob1 = ScheduledJobFakes.defaultIdleJob();
        ScheduledJob scheduledJob2 = ScheduledJobFakes.defaultIdleJob();
        when(jobPersister.findAllJobs()).thenReturn(Arrays.asList(scheduledJob1, scheduledJob2));

        jobScheduler.loadPersistedJobs();

        verify(runnableJobFinder).removeAllScheduledJobs();
        verify(runnableJobFinder).addJob(scheduledJob1);
        verify(runnableJobFinder).addJob(scheduledJob2);
    }

    private void assertJobIsStarted(Job startedJob) {
        ScheduledJob actualScheduledJob = getUpdatedScheduledJob();
        assertEquals(actualScheduledJob.getJob(), startedJob);
        assertEquals(RUNNING, actualScheduledJob.getState());
        assertEquals("tester", actualScheduledJob.getRequesterId());
    }

    private ScheduledJob scheduleIdleJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.defaultIdleJob();
        scheduleJob(scheduledJob);
        return scheduledJob;
    }

    private ScheduledJob scheduleRunningJob() {
        ScheduledJob scheduledJob = ScheduledJobFakes.runningJob();
        scheduleJob(scheduledJob);
        return scheduledJob;
    }

    private void scheduleJob(ScheduledJob scheduledJob) {
        String jobId = scheduledJob.getJob().getId();
        when(runnableJobFinder.findById(jobId)).thenReturn(scheduledJob);
    }

    private void setupNextRunnableJob(ScheduledJob scheduledJob) {
        when(runnableJobFinder.findNextRunnableJob()).thenReturn(scheduledJob);
    }

    private ScheduledJob getUpdatedScheduledJob() {
        ArgumentCaptor<ScheduledJob> argumentCaptor = ArgumentCaptor.forClass(ScheduledJob.class);
        verify(jobPersister).update(argumentCaptor.capture());
        return argumentCaptor.getValue();
    }

}
