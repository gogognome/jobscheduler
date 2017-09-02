package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

public interface ReadonlyJob {
    String getId();

    String getType();

    byte[] getData();

    Instant getScheduledAtInstant();
}
