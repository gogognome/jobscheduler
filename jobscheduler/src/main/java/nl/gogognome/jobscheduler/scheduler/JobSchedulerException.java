package nl.gogognome.jobscheduler.scheduler;

public class JobSchedulerException extends RuntimeException {

    public JobSchedulerException() {
    }

    public JobSchedulerException(String message) {
        super(message);
    }

    public JobSchedulerException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobSchedulerException(Throwable cause) {
        super(cause);
    }

    public JobSchedulerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
