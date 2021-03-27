package nl.gogognome.jobscheduler.jobpersister.database;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobState;
import nl.gogognome.jobscheduler.scheduler.ScheduledJob;

import java.sql.SQLException;
import java.time.Instant;

public class ScheduledJobDAO extends AbstractDomainClassDAO<ScheduledJob>{

    private final DatabaseJobPersisterProperties properties;

    public ScheduledJobDAO(DatabaseJobPersisterProperties properties) {
        super(properties.getTableName(), null, properties.getConnectionName());
        this.properties = properties;
    }

    @Override
    protected ScheduledJob getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        String id = result.getString(properties.getIdColumn());
        String type = result.getString(properties.getTypeColumn());
        byte[] data = result.getBytes(properties.getDataColumn());
        Instant scheduledAtInstant = result.getInstant(properties.getScheduledAtInstantColumn());
        Job job = new Job(id, type, data, scheduledAtInstant);

        JobState state = result.getEnum(JobState.class, properties.getJobStateColumn());
        String requesterId = result.getString(properties.getRequesterIdColumn());
        Instant timeoutAtInstant = result.getInstant(properties.getTimeoutAtInstantColumn());
        return new ScheduledJob(job, state, requesterId, timeoutAtInstant);
    }

    @Override
    protected NameValuePairs getNameValuePairs(ScheduledJob scheduledJob) {
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
