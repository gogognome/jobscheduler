package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

import static nl.gogognome.jobscheduler.scheduler.JobState.ERROR;
import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;
import static nl.gogognome.jobscheduler.scheduler.JobState.RUNNING;

/**
 * A ScheduledJob represents a Job that is managed by the scheduler.
 */
public class ScheduledJob {

    private final Job job;

    private final JobState state;
    private final String requesterId;
    private final Instant timeoutAtInstant;

    public ScheduledJob(Job job, JobState state) {
        this(job, state, null, null);
    }

    public ScheduledJob(Job job, JobState state, String requesterId, Instant timeoutAtInstant) {
        this.job = job;
        this.state = state;
        this.requesterId = requesterId;
        this.timeoutAtInstant = timeoutAtInstant;
    }

    public Job getJob() {
        return job;
    }

    public JobState getState() {
        return state;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public Instant getTimeoutAtInstant() {
        return timeoutAtInstant;
    }

    public ScheduledJob onStart(String requesterId, Instant timeoutAtInstant) {
        return new ScheduledJob(job, RUNNING, requesterId, timeoutAtInstant);
    }

    public ScheduledJob onReschedule() {
        return new ScheduledJob(job, IDLE, null, null);
    }

    public ScheduledJob onError() {
        return new ScheduledJob(job, ERROR, null, null);
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
