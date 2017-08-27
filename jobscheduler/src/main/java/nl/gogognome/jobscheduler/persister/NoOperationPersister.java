package nl.gogognome.jobscheduler.persister;

import nl.gogognome.jobscheduler.scheduler.JobPersister;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

import static java.util.Collections.emptyList;

/**
 * This class implements a job persister that does nothing at all.
 * Use this job persister if you do not want to persist any jobs at all.
 */
public class NoOperationPersister implements JobPersister {

    @Override
    public void create(ScheduledJob job) {
    }

    @Override
    public void remove(String jobId) {
    }

    @Override
    public void update(ScheduledJob job) {
    }

    @Override
    public Iterable<ScheduledJob> findAllJobs() {
        return emptyList();
    }
}
