package nl.gogognome.jobscheduler.scheduler;

public class IllegalJobStateException extends JobSchedulerException {

    public IllegalJobStateException(String message) {
        super(message);
    }
}
