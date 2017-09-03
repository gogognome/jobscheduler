package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

/**
 * A job to be scheduled.
 */
public class Job {

    private final String id;
    private final String type;
    private final byte[] data;
    private final Instant scheduledAtInstant;

    public Job(String id, String type, byte[] data, Instant scheduledAtInstant) {
        this.id = id;
        this.type = type;
        this.data = data != null ? data.clone() : null;
        this.scheduledAtInstant = scheduledAtInstant;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    public Instant getScheduledAtInstant() {
        return scheduledAtInstant;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Job) {
            Job that = (Job) obj;
            return this.getId().equals(that.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "Job-" + getId();
    }

}
