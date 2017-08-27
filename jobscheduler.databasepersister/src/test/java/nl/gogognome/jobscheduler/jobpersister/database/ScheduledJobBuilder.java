package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.jobscheduler.scheduler.JobState;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MINUTES;

public class ScheduledJobBuilder {

    public static ScheduledJob build(String id) {
        ScheduledJob scheduledJob = new ScheduledJob(JobBuilder.build(id));
        scheduledJob.setState(JobState.IDLE);
        scheduledJob.setRequesterId("Piet Puk");
        scheduledJob.setTimeoutAtInstant(Instant.now().plus(1, MINUTES));
        return scheduledJob;
    }
}
