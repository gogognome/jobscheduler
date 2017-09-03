package nl.gogognome.jobscheduler.runnablejobfinder;

import nl.gogognome.jobscheduler.scheduler.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static nl.gogognome.jobscheduler.scheduler.JobState.IDLE;

public class FifoRunnableJobFinder implements RunnableJobFinder {

    private final ArrayList<ScheduledJob> scheduledJobs = new ArrayList<>();

    @Override
    public void addJob(ScheduledJob scheduledJob) {
        if (scheduledJobs.contains(scheduledJob)) {
            throw new DuplicateJobException("A job with id " + scheduledJob.getJob().getId() + " already exists. Jobs must have a unique id!");
        }

        scheduledJobs.add(scheduledJob);
    }

    @Override
    public ScheduledJob findById(String jobId) {
        for (ScheduledJob scheduledJob : scheduledJobs) {
            if (scheduledJob.getJob().getId().equals(jobId)) {
                return scheduledJob;
            }
        }
        return null;
    }

    @Override
    public void updateJob(ScheduledJob scheduledJob) {
        int index = scheduledJobs.indexOf(scheduledJob);
        if (index == -1) {
            throw new UnknownJobException("A job with id " + scheduledJob.getJob().getId() + " does not exist. Only existing jobs can be updated!");
        }

        scheduledJobs.set(index, scheduledJob);
    }

    @Override
    public void removeJob(String jobId) {
        ScheduledJob scheduledJob = findById(jobId);
        if (scheduledJob == null) {
            throw new UnknownJobException("Cannot remove job with id " + jobId + " because it does not exist!");
        }
        scheduledJobs.remove(scheduledJob);
    }

    @Override
    public ScheduledJob findNextRunnableJob() {
        Instant now = Instant.now();
        ScheduledJob bestCandidate = null;
        for (ScheduledJob scheduledJob : scheduledJobs) {
            if (scheduledJob.getState() == IDLE) {
                if (scheduledJob.getJob().getScheduledAtInstant() != null && scheduledJob.getJob().getScheduledAtInstant().isAfter(now)) {
                    continue;
                }
                if (bestCandidate == null || bestCandidate.getJob().getScheduledAtInstant().isAfter(scheduledJob.getJob().getScheduledAtInstant())) {
                    bestCandidate = scheduledJob;
                }
            }
        }
        return bestCandidate;
    }

    @Override
    public List<ScheduledJob> findAllJobs() {
        // Return a copy of the list. The schedule jobs are immutable, so it is safe to just return a copy of
        // the list of scheduled jobs. Returning Collections.unmodifiableList(scheduledJobs) has as drawback that
        // the returned list might change when jobs are scheduled, started or finished.
        return new ArrayList<>(scheduledJobs);
    }

    @Override
    public void removeAllScheduledJobs() {
        scheduledJobs.clear();
    }

    @Override
    public Instant getTimeoutInstant(Job jobToStart) {
        return Instant.now().plus(Duration.ofHours(1));
    }
}
