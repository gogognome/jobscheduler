package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.jobscheduler.scheduler.Job;

public class JobCommandBuilder {

    public static JobCommand buildJob(String id, Command command) {
        Job job = new Job(id);
        job.setType("Test");
        job.setData(new byte[] { 1, 2, 3, 4 });
        return new JobCommand(command, job);
    }

}
