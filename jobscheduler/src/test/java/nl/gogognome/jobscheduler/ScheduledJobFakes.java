package nl.gogognome.jobscheduler;

import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobState;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

import java.time.Duration;
import java.time.Instant;

import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;
import static nl.gogognome.jobscheduler.scheduler.JobState.RUNNING;

public class ScheduledJobFakes {


    public static ScheduledJob defaultIdleJob() {
        return defaultIdleJobStartingAfter(Duration.ZERO);
    }

    public static ScheduledJob defaultIdleJobStartingAfter(Duration startInstantOffset) {
        return defaultIdleJobStartingAfter(startInstantOffset, IDLE);
    }

    public static ScheduledJob defaultIdleJobStartingAfter(Duration startInstantOffset, JobState state) {
        Job job = JobFakes.withStartInstant(Instant.now().plus(startInstantOffset));
        return new ScheduledJob(job, state);
    }

    public static ScheduledJob with(Job job) {
        return new ScheduledJob(job, IDLE);
    }

    public static ScheduledJob runningJob() {
        return new ScheduledJob(JobFakes.defaultJob(), RUNNING, "tester", Instant.now().plus(Duration.ofHours(1)));
    }
}
