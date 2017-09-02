package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.jobscheduler.scheduler.Job;

import java.sql.SQLException;
import java.util.List;

public class JobCommandDAO extends AbstractDomainClassDAO<JobCommand>{

    private final JobIngesterProperties properties;

    public JobCommandDAO(JobIngesterProperties properties) {
        super(properties.getTableName(), properties.getCommandIdSequence(), properties.getConnectionName());
        this.properties = properties;
    }

    public List<JobCommand> findJobCommands() throws SQLException {
        if (properties.getSelectJobCommandsQuery() == null) {
            return findAll(properties.getCommandIdColumn());
        }
        return execute(properties.getSelectJobCommandsQuery()).toList(this::getObjectFromResultSet);
    }

    public void deleteJobCommands(List<JobCommand> jobCommands) throws SQLException {
        if (!jobCommands.isEmpty()) {
            StringBuilder query = new StringBuilder();
            query.append("DELETE FROM ").append(tableName).append(" WHERE ").append(properties.getCommandIdColumn()).append(" IN (");
            for (int i = 0; i< jobCommands.size(); i ++) {
                if (i != 0) {
                    query.append(',');
                }
                query.append('?');
            }
            query.append(')');

            int nrDeletedRows = execute(query.toString(), jobCommands.stream().map(j -> j.getCommandId()).toArray(Object[]::new)).getNumberModifiedRows();
            if (nrDeletedRows != jobCommands.size()) {
                throw new SQLException("Deleted " + nrDeletedRows + " from the " + jobCommands.size() + " job commands!");
            }
        }
    }

    @Override
    protected JobCommand getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        String commandId = result.getString(properties.getCommandIdColumn());

        Job job = new Job(result.getString(properties.getIdColumn()));
        job.setScheduledAtInstant(result.getInstant(properties.getScheduledAtInstantColumn()));
        job.setType(result.getString(properties.getTypeColumn()));
        job.setData(result.getBytes(properties.getDataColumn()));

        Command command = result.getEnum(Command.class, properties.getCommandColumn());

        return new JobCommand(commandId, command, job);
    }

    @Override
    protected NameValuePairs getNameValuePairs(JobCommand jobCommand) throws SQLException {
        Job job = jobCommand.getJob();
        return new NameValuePairs()
                .add(properties.getCommandIdColumn(), jobCommand.getCommandId())
                .add(properties.getCommandColumn(), jobCommand.getCommand())
                .add(properties.getIdColumn(), job.getId())
                .add(properties.getScheduledAtInstantColumn(), job.getScheduledAtInstant())
                .add(properties.getTypeColumn(), job.getType())
                .add(properties.getDataColumn(), job.getData());
    }

    @Override
    protected String getPkColumn() {
        return properties.getCommandIdColumn();
    }
}
