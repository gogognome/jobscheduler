package nl.gogognome.jobscheduler.jobpersister.database;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("database")
public class DatabaseJobPersisterProperties {

    private String connectionName = "nl.gogognome.jobscheduler.jobingester";
    private String tableName = "NlGogognomeJobs";
    private String idColumn = "id";
    private String scheduledAtInstantColumn = "scheduledAtInstant";
    private String typeColumn = "type";
    private String dataColumn = "data";
    private String jobStateColumn = "state";
    private String requesterIdColumn = "requesterId";
    private String timeoutAtInstantColumn = "timeoutAtInstant";

    private long delayBetweenPolls = 1000L;

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    public String getScheduledAtInstantColumn() {
        return scheduledAtInstantColumn;
    }

    public void setScheduledAtInstantColumn(String scheduledAtInstantColumn) {
        this.scheduledAtInstantColumn = scheduledAtInstantColumn;
    }

    public String getTypeColumn() {
        return typeColumn;
    }

    public void setTypeColumn(String typeColumn) {
        this.typeColumn = typeColumn;
    }

    public String getDataColumn() {
        return dataColumn;
    }

    public void setDataColumn(String dataColumn) {
        this.dataColumn = dataColumn;
    }

    public String getJobStateColumn() {
        return jobStateColumn;
    }

    public void setJobStateColumn(String jobStateColumn) {
        this.jobStateColumn = jobStateColumn;
    }

    public String getRequesterIdColumn() {
        return requesterIdColumn;
    }

    public void setRequesterIdColumn(String requesterIdColumn) {
        this.requesterIdColumn = requesterIdColumn;
    }

    public long getDelayBetweenPolls() {
        return delayBetweenPolls;
    }

    public void setDelayBetweenPolls(long delayBetweenPolls) {
        this.delayBetweenPolls = delayBetweenPolls;
    }

    public String getTimeoutAtInstantColumn() {
        return timeoutAtInstantColumn;
    }

    public void setTimeoutAtInstantColumn(String timeoutAtInstantColumn) {
        this.timeoutAtInstantColumn = timeoutAtInstantColumn;
    }
}
