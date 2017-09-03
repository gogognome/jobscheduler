package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.jobscheduler.scheduler.Job;

import java.time.Instant;

public class JobBuilder {

    public static Job build(String id) {
        return new Job(id, "Test", new byte[] { 1, 2, 3, 4 }, Instant.now());
    }

}
