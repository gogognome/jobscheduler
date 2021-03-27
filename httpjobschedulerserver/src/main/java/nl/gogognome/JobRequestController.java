package nl.gogognome;

import nl.gogognome.jobscheduler.jobingester.database.JobIngesterRunner;
import nl.gogognome.jobscheduler.scheduler.Job;
import nl.gogognome.jobscheduler.scheduler.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.Charset;
import java.util.Base64;

@RestController
@DependsOn("dataSourceInit")
public class JobRequestController {

    private final Logger logger = LoggerFactory.getLogger(JobRequestController.class);

    private final JobScheduler jobScheduler;
    private final Properties properties;
    private final JobIngesterRunner jobIngesterRunner;
    private Charset charset;

    public JobRequestController(JobScheduler jobScheduler, Properties properties, JobIngesterRunner jobIngesterRunner) {
        this.jobScheduler = jobScheduler;
        this.properties = properties;
        this.jobIngesterRunner = jobIngesterRunner;
    }

    @PostConstruct
    public void init() {
        logger.trace("init called");
        logger.info("Using database connection URL: " + properties.getDatabaseConnectionUrl());
        logger.info("Request timeout when waiting for runnable job: " + properties.getRequestTimeoutMilliseconds() + " ms");
        jobScheduler.loadPersistedJobs();
        jobIngesterRunner.start();
    }

    @PreDestroy
    public void close() {
        logger.trace("close called");
        jobIngesterRunner.stop();
    }

    @RequestMapping("/nextjob")
    public JobResponse nextJob(@RequestParam(value="requesterId") String requesterId) {
        logger.trace("nextJob called for requester " + requesterId);

        try {
            Job job = jobScheduler.startNextRunnableJob(requesterId, properties.getRequestTimeoutMilliseconds());
            if (job != null) {
                logger.debug("found job " + job.getId());
                JobResponse response = new JobResponse();
                response.setJobAvailable(true);
                response.setJobId(job.getId());
                response.setJobData(job.getData());
                return response;
            } else {
                logger.debug("timed out - no job found");
                return new JobResponse();
            }
        } catch (Exception e) {
            jobScheduler.loadPersistedJobs();
            throw e;
        }
    }

}
