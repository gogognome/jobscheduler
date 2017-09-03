package nl.gogognome.jobscheduler;

import nl.gogognome.jobscheduler.scheduler.Job;

import java.time.Instant;

public class JobFakes {

    private static int nextId;

    public static Job defaultJob() {
        return defaultWithId(nextId());
    }

    public static Job defaultWithId(String jobId) {
        return new Job(jobId, "someType", null, Instant.now());
    }

    public static Job withStartInstant(Instant startAtInstant) {
        return new Job(nextId(), "someType", null, startAtInstant);
    }

    private static String nextId() {
        return Integer.toString(nextId++);
    }
}
