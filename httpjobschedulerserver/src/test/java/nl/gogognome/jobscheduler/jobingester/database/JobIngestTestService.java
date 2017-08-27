package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.scheduler.Job;
import org.springframework.stereotype.Component;

@Component
public class JobIngestTestService {

    private final JobIngesterProperties properties;

    public JobIngestTestService(JobIngesterProperties properties) {
        this.properties = properties;
    }

    public void createJobCommand(Command command, String jobId) {
        NewTransaction.runs(() -> new JobCommandDAO(properties).create(new JobCommand(command, new Job(jobId))));
    }

    public void createJobCommand(Command command, Job job) {
        NewTransaction.runs(() -> new JobCommandDAO(properties).create(new JobCommand(command, job)));
    }

    public void waitUntilJobsAreIngested() {
        while (true) {
            if (NewTransaction.returns(() -> new JobCommandDAO(properties).count(null)) == 0) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore this exception
            }
        }
    }
}
