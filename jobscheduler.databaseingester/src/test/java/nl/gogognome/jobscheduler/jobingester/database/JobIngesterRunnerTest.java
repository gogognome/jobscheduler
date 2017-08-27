package nl.gogognome.jobscheduler.jobingester.database;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class JobIngesterRunnerTest {

    private JobIngesterProperties properties = new JobIngesterProperties();
    private JobIngester jobIngester = mock(JobIngester.class);
    private JobIngesterRunnerSpy jobIngesterRunner = new JobIngesterRunnerSpy(properties, jobIngester);

    static class JobIngesterRunnerSpy extends JobIngesterRunner {

        private final List<Long> delaysInMilliseconds = new ArrayList<>();

        public JobIngesterRunnerSpy(JobIngesterProperties properties, JobIngester jobIngester) {
            super(properties, jobIngester);
        }

        @Override
        protected void delayThread(long delayInMilliseconds) {
            synchronized (delaysInMilliseconds) {
                delaysInMilliseconds.add(delayInMilliseconds);
                delaysInMilliseconds.notify();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public List<Long> getDelaysInMilliseconds() {
            synchronized (delaysInMilliseconds) {
                return delaysInMilliseconds;
            }
        }

        public void waitForNrIterations(int nrIterations) throws InterruptedException {
            synchronized (delaysInMilliseconds) {
                while (nrIterations > delaysInMilliseconds.size()) {
                    delaysInMilliseconds.wait(10);
                }
            }
        }
    }

    @Test
    public void timerThread_oneIteration_shouldIngestJobsOnce() throws InterruptedException {
        jobIngesterRunner.start();
        jobIngesterRunner.waitForNrIterations(1);
        jobIngesterRunner.stop();

        verify(jobIngester, times(1)).ingestJobs();
    }

    @Test
    public void timerThread_twentyIterationsWithoutJobCommandsFound_timeoutShouldGrowExponentiallyAndBeLimitted() throws InterruptedException {
        jobIngesterRunner.start();
        jobIngesterRunner.waitForNrIterations(20);
        jobIngesterRunner.stop();

        List<Long> expectedDelays = asList(2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1000L,
                1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L, 1000L);
        assertEquals(expectedDelays, jobIngesterRunner.getDelaysInMilliseconds());
    }

    @Test
    public void timerThread_twentyIterationsWithJobCommandsFound_timeoutShoulBeReset() throws InterruptedException {
        when(jobIngester.ingestJobs()).thenReturn(1);

        jobIngesterRunner.start();
        jobIngesterRunner.waitForNrIterations(5);
        jobIngesterRunner.stop();

        List<Long> expectedDelays = asList(1L, 1L, 1L, 1L, 1L);
        assertEquals(expectedDelays, jobIngesterRunner.getDelaysInMilliseconds());
    }

    @Test
    public void startTwice_shouldFail() {
        jobIngesterRunner.start();

        try {
            jobIngesterRunner.start();
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            assertEquals("The job ingester is still running", e.getMessage());
        } finally {
            jobIngesterRunner.stop();
        }
    }

    @Test
    public void stoppingTwice_shouldFail() {
        jobIngesterRunner.start();
        jobIngesterRunner.stop();

        try {
            jobIngesterRunner.stop();
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            assertEquals("The job ingester is not running", e.getMessage());
        }
    }
}
