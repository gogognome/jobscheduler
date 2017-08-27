package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.jobscheduler.scheduler.Job;

public class JobBuilder {

    public static Job build(String id) {
        Job job = new Job(id);
        job.setType("Test");
        job.setData(new byte[] { 1, 2, 3, 4 });
        return job;
    }

}
