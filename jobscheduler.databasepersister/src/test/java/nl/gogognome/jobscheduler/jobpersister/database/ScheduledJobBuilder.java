package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MINUTES;
import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;

public class ScheduledJobBuilder {

    public static ScheduledJob build(String id) {
        ScheduledJob scheduledJob = new ScheduledJob(JobBuilder.build(id), IDLE, "Piet Puk", Instant.now().plus(1, MINUTES));
        return scheduledJob;
    }
}
