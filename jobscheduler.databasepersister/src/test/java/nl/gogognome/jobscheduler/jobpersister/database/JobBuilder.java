package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.jobscheduler.scheduler.Job;

import java.time.Instant;

public class JobBuilder {

    public static Job build(String id) {
        return new Job(id, "Test", "Hello, World!", Instant.now());
    }

}
