package nl.gogognome.jobscheduler;

import nl.gogognome.jobscheduler.persister.NoOperationPersister;
import nl.gogognome.jobscheduler.runnablejobfinder.FifoRunnableJobFinder;
import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobScheduler;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;

public class ExampleCodeTest {

    @SuppressWarnings({"InfiniteLoopStatement", "StatementWithEmptyBody"})
    @Ignore("This test does not end. It contains the example code of README.md.")
    @Test
    public void exampleCode() {
        // Create a job scheduler
        JobScheduler jobScheduler = new JobScheduler(new FifoRunnableJobFinder(), new NoOperationPersister());

        // Create a job
        String jobId = "857394";
        String jobType = "send email";
        String data = "{address: 'foo@bar.com', subject: 'welcome', contents: 'bla bla'}";
        Instant scheduledAtInstant = Instant.now();
        Job jobToSchedule = new Job(jobId, jobType, data, scheduledAtInstant);

        // Add the job
        jobScheduler.schedule(jobToSchedule);

        // Handle jobs
        while (true) {
            // Get the next job to be executed
            Job job = jobScheduler.startNextRunnableJob("pc83-pid654", 30000);
            if (job == null) {
                // No job present after 30000 millisecond timeout. Wait for a job to be added or to become runnable
                continue;
            }
            if ("send email".equals(job.getType())) {
                // It is a job to send an email. Handle it here.
                try {
                    sendEmail(job.getData());
                    // Notify job scheduler that the job has finished.
                    jobScheduler.jobFinished(job.getId());
                } catch (Exception e) {
                    // Notify job scheduler that the job has failed.
                    jobScheduler.jobFailed(job.getId());
                }
            } else {
                // Handle other type of job here
            }
        }
    }

    private void sendEmail(String data) {
        System.out.println("Sending email. Data: " + data);
    }
}
