package nl.gogognome.jobscheduler.scheduler;

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

    private Job job1 = new Job("one");
    private ScheduledJob scheduledJob1 = new ScheduledJob(job1);
    private Job job2 = new Job("two");
    private ScheduledJob scheduledJob2 = new ScheduledJob(job2);

    @Test
    public void schedule_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.schedule(null));
    }

    @Test
    public void schedule_addsJobToJobFinderAndPersistsJob() {
        jobScheduler.schedule(job1);

        ArgumentCaptor<ScheduledJob> argumentCaptor = ArgumentCaptor.forClass(ScheduledJob.class);
        verify(runnableJobFinder).addJob(argumentCaptor.capture());
        ScheduledJob createdScheduledJob = argumentCaptor.getValue();
        assertSame(job1, createdScheduledJob.getJob());
        assertEquals(IDLE, createdScheduledJob.getState());
        assertNull(createdScheduledJob.getRequesterId());

        verify(jobPersister).create(createdScheduledJob);
    }

    @Test
    public void jobFinished_existingRunningJob_jobIsRemoved() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(RUNNING);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        jobScheduler.jobFinished(job1.getId());

        verify(runnableJobFinder).removeJob(job1.getId());
        verify(jobPersister).remove(job1.getId());
    }

    @Test
    public void stopSuccessfully_null_thorwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.jobFinished(null));
    }

    @Test
    public void stopSuccessfull_existingNotRunningJob_throwsException() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(IDLE);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.jobFinished(job1.getId()));

        verify(runnableJobFinder, never()).removeJob(job1.getId());
        verify(jobPersister, never()).remove(job1.getId());
    }

    @Test
    public void stopSuccessfully_notExistingJob_throwsException() {
        assertThrows(UnknownJobException.class, () -> jobScheduler.jobFinished(job1.getId()));

        verify(runnableJobFinder, never()).removeJob(job1.getId());
        verify(jobPersister, never()).remove(job1.getId());
    }

    @Test
    public void jobFailed_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.jobFailed(null));
    }

    @Test
    public void jobFailed_existingRunningJob_jobIsUpdated() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(RUNNING);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        jobScheduler.jobFailed(job1.getId());

        verify(jobPersister).update(same(scheduledJob1));
        assertEquals(ERROR, scheduledJob1.getState());
        assertNull(scheduledJob1.getRequesterId());
    }

    @Test
    public void jobFailed_existingNotRunningJob_throwsException() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(IDLE);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.jobFailed(job1.getId()));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }


    @Test
    public void jobFailed_notExistingJob_throwsException() {
        assertThrows(UnknownJobException.class, () -> jobScheduler.jobFailed(job1.getId()));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }

    @Test
    public void reschedule_null_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.reschedule(null));
    }

    @Test
    public void reschedule_existingNotRunningJob_jobIsRescheduled() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(RUNNING);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        jobScheduler.reschedule(job1);

        verify(jobPersister).update(same(scheduledJob1));
        assertEquals(IDLE, scheduledJob1.getState());
        assertNull(scheduledJob1.getRequesterId());
    }

    @Test
    public void reschedule_notExistingJob_throwsException() {
        assertThrows(UnknownJobException.class, () -> jobScheduler.reschedule(job1));

        verify(runnableJobFinder, never()).updateJob(any(ScheduledJob.class));
        verify(jobPersister, never()).update(any(ScheduledJob.class));
    }

    @Test
    public void reschedule_existingNotRunningJob_throwsException() {
        scheduledJob1.setRequesterId("tester");
        scheduledJob1.setState(IDLE);
        when(runnableJobFinder.findById(job1.getId())).thenReturn(scheduledJob1);

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.reschedule(job1));

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

        assertNull(startedJob);
    }

    @Test
    public void tryStartNextRunnableJob_idleJobIsChosen_ReturnsJobWithStateUpdated() {
        scheduledJob1.setState(IDLE);
        when(runnableJobFinder.findNextRunnableJob()).thenReturn(scheduledJob1);

        Job startedJob = jobScheduler.tryStartNextRunnableJob("tester");

        assertJobIsStarted(job1, startedJob);
    }

    @Test
    public void tryStartNextRunnableJob_noneIdleJobChosen_Fails() {
        scheduledJob1.setState(RUNNING);
        when(runnableJobFinder.findNextRunnableJob()).thenReturn(scheduledJob1);

        assertThrows(IllegalJobStateException.class, () -> jobScheduler.tryStartNextRunnableJob("tester"));
        verify(runnableJobFinder, never()).updateJob(scheduledJob1);
        verify(jobPersister, never()).update(scheduledJob1);
    }

    @Test
    public void startNextRunnableJob_idleJobPresent_ReturnsJobWithStateUpdated() throws InterruptedException {
        scheduledJob1.setState(IDLE);
        when(runnableJobFinder.findNextRunnableJob()).thenReturn(scheduledJob1);

        Job startedJob = jobScheduler.startNextRunnableJob("tester", 1000L);

        assertJobIsStarted(job1, startedJob);
    }

    @Test
    public void startNextRunnableJob_noJobsAdded_TimesOut() throws InterruptedException {
        long timeout = 1000L;
        long expectedEndTime = System.currentTimeMillis() + timeout;
        Job job = jobScheduler.startNextRunnableJob("tester", timeout);

        assertNull(job);
        verify(runnableJobFinder, never()).updateJob(scheduledJob1);
        verify(jobPersister, never()).update(scheduledJob1);
        assertTrue(System.currentTimeMillis() >= expectedEndTime);
    }

    @Test
    public void startNextRunnableJob_invalidParameter_throwsException() {
        assertThrows(NullPointerException.class, () -> jobScheduler.startNextRunnableJob(null, 123));
        assertThrows(IllegalArgumentException.class, () -> jobScheduler.startNextRunnableJob("tester", -1));
    }

    @Test
    public void startNextRunnableJob_firstJobAddedAfterOneSecond_ReturnsJobWithStateUpdateAfterOneSecond() throws InterruptedException {
        long start = System.currentTimeMillis();
        long jobAddDelay = 1000;
        long timeout = jobAddDelay + 3000;
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(() -> {
            try {
                Thread.sleep(jobAddDelay);
                scheduledJob1.setState(IDLE);
                when(runnableJobFinder.findNextRunnableJob()).thenReturn(scheduledJob1);
                jobScheduler.schedule(job1);
            } catch (InterruptedException e) {
                // ignore exception
            }
        });

        Job startedJob = jobScheduler.startNextRunnableJob("tester", timeout);

        assertJobIsStarted(job1, startedJob);
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
        when(jobPersister.findAllJobs()).thenReturn(Arrays.asList(scheduledJob1, scheduledJob2));

        jobScheduler.loadPersistedJobs();

        verify(runnableJobFinder).removeAllScheduledJobs();
        verify(runnableJobFinder).addJob(scheduledJob1);
        verify(runnableJobFinder).addJob(scheduledJob2);
    }

    private void assertJobIsStarted(Job expectedScheduledJob, Job startedJob) {
        assertEquals(expectedScheduledJob, startedJob);
        ScheduledJob startedScheduledJob = startedJob == job1 ? scheduledJob1 : scheduledJob2;
        assertEquals(RUNNING, startedScheduledJob.getState());
        assertEquals("tester", startedScheduledJob.getRequesterId());
        verify(jobPersister).update(startedScheduledJob);
    }
}
