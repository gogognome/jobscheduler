package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobState;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
class ScheduledJobDAO extends AbstractDomainClassDAO<ScheduledJob>{

    private final DatabaseJobPersisterProperties properties;

    public ScheduledJobDAO(DatabaseJobPersisterProperties properties) {
        super(properties.getTableName(), null, properties.getConnectionName());
        this.properties = properties;
    }

    @Override
    protected ScheduledJob getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Job job = new Job(result.getString(properties.getIdColumn()));
        job.setType(result.getString(properties.getTypeColumn()));
        job.setData(result.getBytes(properties.getDataColumn()));
        job.setScheduledAtInstant(result.getInstant(properties.getScheduledAtInstantColumn()));
        ScheduledJob scheduledJob = new ScheduledJob(job);
        scheduledJob.setState(result.getEnum(JobState.class, properties.getJobStateColumn()));
        scheduledJob.setRequesterId(result.getString(properties.getRequesterIdColumn()));
        scheduledJob.setTimeoutAtInstant(result.getInstant(properties.getTimeoutAtInstantColumn()));
        return scheduledJob;
    }

    @Override
    protected NameValuePairs getNameValuePairs(ScheduledJob scheduledJob) throws SQLException {
        return new NameValuePairs()
                .add(properties.getIdColumn(), scheduledJob.getJob().getId())
                .add(properties.getScheduledAtInstantColumn(), scheduledJob.getJob().getScheduledAtInstant())
                .add(properties.getTypeColumn(), scheduledJob.getJob().getType())
                .add(properties.getDataColumn(), scheduledJob.getJob().getData())
                .add(properties.getJobStateColumn(), scheduledJob.getState())
                .add(properties.getRequesterIdColumn(), scheduledJob.getRequesterId())
                .add(properties.getTimeoutAtInstantColumn(), scheduledJob.getTimeoutAtInstant());
    }
}
