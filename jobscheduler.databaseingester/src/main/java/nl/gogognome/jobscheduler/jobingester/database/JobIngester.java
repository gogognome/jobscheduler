package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.jobscheduler.scheduler.JobScheduler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobIngester {

    private final JobScheduler jobScheduler;
    private final JobCommandDAO jobCommandDAO;

    public JobIngester(JobScheduler jobScheduler, JobCommandDAO jobCommandDAO) {
        this.jobScheduler = jobScheduler;
        this.jobCommandDAO = jobCommandDAO;
    }

    /**
     * Reads job commands from the database and forwards them to the job scheduler.
     * @return the number of job commands handled
     */
    public int ingestJobs() {
        try {
            List<JobCommand> jobCommands = NewTransaction.returns(jobCommandDAO::findJobCommands);
            jobScheduler.runBatch(() -> {
                NewTransaction.runs(() -> {
                    jobCommands.stream().forEach(j -> {
                        switch (j.getCommand()) {
                            case SCHEDULE:
                                jobScheduler.schedule(j.getJob());
                                break;
                            case RESCHEDULE:
                                jobScheduler.reschedule(j.getJob());
                                break;
                            case JOB_FINISHED:
                                jobScheduler.jobFinished(j.getJob().getId());
                                break;
                            case JOB_FAILED:
                                jobScheduler.jobFailed(j.getJob().getId());
                                break;
                        }
                    });
                    jobCommandDAO.deleteJobCommands(jobCommands);
                });
            });
            return jobCommands.size();
        } catch (Exception e) {
            jobScheduler.loadPersistedJobs(); // transaction is rolled back, thus refresh the jobs in the scheduler from the persisted jobs
            throw e;
        }
    }

}
