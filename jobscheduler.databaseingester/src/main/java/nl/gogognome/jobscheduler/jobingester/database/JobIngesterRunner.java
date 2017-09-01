package nl.gogognome.jobscheduler.jobingester.database;

public class JobIngesterRunner {

    private final JobIngesterProperties properties;
    private final JobIngester jobIngester;

    private Thread thread;
    private boolean threadRunning;
    private final Object lock = new Object();

    public JobIngesterRunner(JobIngesterProperties properties, JobIngester jobIngester) {
        this.properties = properties;
        this.jobIngester = jobIngester;
    }

    public void start() {
        synchronized (lock) {
            if (threadRunning) {
                throw new IllegalStateException("The job ingester is still running");
            }
            threadRunning = true;
            thread = new Thread(this::timerThread);
            thread.start();
        }
    }

    public void stop() {
        synchronized (lock) {
            if (!threadRunning) {
                throw new IllegalStateException("The job ingester is not running");
            }
            threadRunning = false;
            lock.notify();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            // ignore this exception
        }
        thread = null;
    }

    private void timerThread() {
        long delayInMilliseconds = 1;
        while (true) {
            synchronized (lock) {
                if (!threadRunning) {
                    return;
                }
            }

            int nrCommandsHandled;
            try {
                nrCommandsHandled = jobIngester.ingestJobs();
            } catch (Exception e) {
                nrCommandsHandled = 0;
            }

            delayInMilliseconds = nrCommandsHandled > 0 ? 1 : Math.min(2 * delayInMilliseconds, properties.getDelayBetweenPolls());
            delayThread(delayInMilliseconds);
        }
    }

    protected void delayThread(long delayInMilliseconds) {
        synchronized (lock) {
            try {
                lock.wait(delayInMilliseconds);
            } catch (InterruptedException e) {
                // ignore this exception
            }
        }
    }
}
