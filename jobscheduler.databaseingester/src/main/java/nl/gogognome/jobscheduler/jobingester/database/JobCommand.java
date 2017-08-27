package nl.gogognome.jobscheduler.jobingester.database;

import nl.gogognome.jobscheduler.scheduler.Job;

class JobCommand {

    private final Command command;
    private final Job job;

    public JobCommand(Command command, Job job) {
        this.command = command;
        this.job = job;
    }

    public Command getCommand() {
        return command;
    }

    public Job getJob() {
        return job;
    }
}
