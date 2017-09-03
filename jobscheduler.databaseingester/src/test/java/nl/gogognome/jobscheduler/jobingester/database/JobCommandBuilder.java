package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.jobscheduler.scheduler.Job;

import java.time.Instant;

public class JobCommandBuilder {

    public static JobCommand buildJob(String id, Command command) {
        Job job = new Job(id, "someType", new byte[] { 1, 2, 3, 4 }, Instant.now());
        return new JobCommand(command, job);
    }

}
