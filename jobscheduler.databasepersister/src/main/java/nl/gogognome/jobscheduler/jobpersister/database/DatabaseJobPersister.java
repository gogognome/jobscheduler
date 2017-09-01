package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.dataaccess.transaction.RequireTransaction;
import nl.gogognome.jobscheduler.scheduler.JobPersister;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

public class DatabaseJobPersister implements JobPersister {

    private final DatabaseJobPersisterProperties properties;
    private final ScheduledJobDAO scheduledJobDAO;

    public DatabaseJobPersister(DatabaseJobPersisterProperties properties, ScheduledJobDAO scheduledJobDAO) {
        this.properties = properties;
        this.scheduledJobDAO = scheduledJobDAO;
    }

    @Override
    public void create(ScheduledJob scheduledJob) {
        RequireTransaction.runs(() -> scheduledJobDAO.create(scheduledJob));
    }

    @Override
    public void remove(String jobId) {
        RequireTransaction.runs(() -> scheduledJobDAO.delete(jobId));
    }

    @Override
    public void update(ScheduledJob scheduledJob) {
        RequireTransaction.runs(() -> scheduledJobDAO.update(scheduledJob));
    }

    @Override
    public Iterable<ScheduledJob> findAllJobs() {
        return RequireTransaction.returns(scheduledJobDAO::findAll);
    }
}
