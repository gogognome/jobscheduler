package nl.gogognome.jobscheduler.jobingester.database;

public class JobIngesterProperties {

    private String connectionName = "nl.gogognome.jobscheduler.jobingester";
    private String tableName = "NlGogognomeJobsToIngest";
    private String commandIdSequence = "command_id_sequence";
    private String commandIdColumn = "command_id";
    private String commandColumn = "command";
    private String idColumn = "id";
    private String scheduledAtInstantColumn = "scheduledAtInstant";
    private String typeColumn = "type";
    private String dataColumn = "data";
    private String selectJobCommandsQuery = null;

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

    public String getCommandIdSequence() {
        return commandIdSequence;
    }

    public void setCommandIdSequence(String commandIdSequence) {
        this.commandIdSequence = commandIdSequence;
    }

    public String getCommandIdColumn() {
        return commandIdColumn;
    }

    public void setCommandIdColumn(String commandIdColumn) {
        this.commandIdColumn = commandIdColumn;
    }

    public String getCommandColumn() {
        return commandColumn;
    }

    public void setCommandColumn(String commandColumn) {
        this.commandColumn = commandColumn;
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

    public long getDelayBetweenPolls() {
        return delayBetweenPolls;
    }

    public void setDelayBetweenPolls(long delayBetweenPolls) {
        this.delayBetweenPolls = delayBetweenPolls;
    }

    public String getSelectJobCommandsQuery() {
        return selectJobCommandsQuery;
    }

    public void setSelectJobCommandsQuery(String selectJobCommandsQuery) {
        this.selectJobCommandsQuery = selectJobCommandsQuery;
    }
}
