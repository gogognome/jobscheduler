package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

public interface ReadonlyScheduledJob {

    ReadonlyJob getReadonlyJob();
    JobState getState();
    String getRequesterId();
    Instant getTimeoutAtInstant();
}
