package nl.gogognome.jobscheduler.scheduler;

public class UnknownJobException extends JobSchedulerException {

    public UnknownJobException(String message) {
        super(message);
    }
}
