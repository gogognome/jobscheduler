package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

/**
 * A ScheduledJob represents a Job that is managed by the scheduler.
 */
public class ScheduledJob implements ReadonlyScheduledJob {

    private final Job job;

    private JobState state;
    private String requesterId;
    private Instant timeoutAtInstant;

    public ScheduledJob(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    @Override
    public ReadonlyJob getReadonlyJob() {
        return job;
    }

    @Override
    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    @Override
    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    @Override
    public Instant getTimeoutAtInstant() {
        return timeoutAtInstant;
    }

    public void setTimeoutAtInstant(Instant timeoutAtInstant) {
        this.timeoutAtInstant = timeoutAtInstant;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScheduledJob) {
            ScheduledJob that = (ScheduledJob) obj;
            return this.getJob().equals(that.getJob());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 23 * getJob().hashCode();
    }

    @Override
    public String toString() {
        return "ScheduledJob-" + getJob().getId();
    }

}
