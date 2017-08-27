package nl.gogognome;

public class JobResponse {

    private boolean jobAvailable;
    private String jobId;
    private String jobData;

    public boolean isJobAvailable() {
        return jobAvailable;
    }

    public void setJobAvailable(boolean jobAvailable) {
        this.jobAvailable = jobAvailable;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }
}
