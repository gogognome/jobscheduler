package nl.gogognome.jobscheduler.scheduler;

import java.time.Instant;

/**
 * A job to be scheduled.
 */
public class Job implements ReadonlyJob {

    private final String id;
    private String type;
    private byte[] data;
    private Instant scheduledAtInstant;

    public Job(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    public void setData(byte[] data) {
        this.data = data != null ? data.clone() : null;
    }

    @Override
    public Instant getScheduledAtInstant() {
        return scheduledAtInstant;
    }

    public void setScheduledAtInstant(Instant scheduledAtInstant) {
        this.scheduledAtInstant = scheduledAtInstant;
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
