package nl.gogognome.jobscheduler.runnablejobfinder;

import nl.gogognome.jobscheduler.scheduler.*;

import java.time.Instant;
import java.util.ArrayList;

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
        boolean jobWasPresent = scheduledJobs.remove(new ScheduledJob(new Job(jobId)));
        if (!jobWasPresent) {
            throw new UnknownJobException("Cannot remove job with id " + jobId + " because it does not exist!");
        }
    }

    @Override
    public ScheduledJob findNextRunnableJob() {
        Instant now = Instant.now();
        ScheduledJob bestCandidate = null;
        for (ScheduledJob scheduledJob : scheduledJobs) {
            if (scheduledJob.getState() == JobState.IDLE) {
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
    public void removeAllScheduledJobs() {
        scheduledJobs.clear();
    }
}
