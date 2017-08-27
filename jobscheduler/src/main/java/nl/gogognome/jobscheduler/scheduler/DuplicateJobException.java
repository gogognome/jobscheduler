package nl.gogognome.jobscheduler.scheduler;

public class DuplicateJobException extends JobSchedulerException {

    public DuplicateJobException(String message) {
        super(message);
    }
}
