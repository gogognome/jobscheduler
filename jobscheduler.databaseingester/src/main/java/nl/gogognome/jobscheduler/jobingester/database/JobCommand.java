package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.jobscheduler.scheduler.Job;

public class JobCommand {

    private final String commandId;
    private final Command command;
    private final Job job;

    public JobCommand(Command command, Job job) {
        this(null, command, job);
    }

    public JobCommand(String commandId, Command command, Job job) {
        this.commandId = commandId;
        this.command = command;
        this.job = job;
    }

    public String getCommandId() {
        return commandId;
    }

    public Command getCommand() {
        return command;
    }

    public Job getJob() {
        return job;
    }

}
