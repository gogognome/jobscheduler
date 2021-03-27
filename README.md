# Job Scheduler - a small and pluggable job scheduler

## Introduction

Imagine you are building an enterprise application. Part of the work
done by the application will be responding to requests made by a user
or another external system. Most of the time the requested work can 
be done really quickly, so it is done as part of the request handling.

However, some work that is requested can take a long time. Processing
video files can take a long time because video files can be huge.
But even sending an email can take long, for example if the network is slow or
if the mail server is not responding at all. This kind of work is typically
handled by putting a message in a queue to do a specific job. 
Another part of the system takes these messages from the queue 
one by one and executes the jobs.

Job Scheduler implements a queue for jobs. It allows you to add jobs
to the queue and to take them from the queue to execute them.

This library is **small**, about 15 kilobytes, because it
has no dependencies at all. 

The library is **pluggable**. Job Scheduler stores the queued jobs in memory,
but it offers a hook to persist the queued jobs. An example of plugin
to persist jobs in a database can be found in the module `jobscheduler.databasepersister`.

The library can be included in your own application. You can add jobs
to the queue and get jobs from the queue by calling methods.
It is also possible to create a standalone application, a microservice, 
that keeps track of the queued jobs. If you include in this microservice
the jar file from the `jobscheduler.databaseingester` module, then commands to
create, update or delete jobs are 'sent' to the Job Scheduler by writing
rows in a database. It is quite easy to add an HTTP server to this microservice,
so that other microservices can request jobs to be executed via an HTTP request.
An example of such a microservice can be found in the `httpjobschedulerserver`
module.

Job Scheduler gives you full control over the order in which jobs are executed.
A FIFO implementation is supplied by this library. By implementing
the interface `RunnableJobFinder` yourself, you get full control over which
job is executed and when. This allows you to temporarily block certain types
of jobs, or to limit the number of jobs of certain type to be executed 
at any moment. It allows you to change the order of jobs, or even combine
jobs before they got executed. *This feature was the main reason to build
the Job Scheduler.*

## Code samples

Here is sample code to create a job scheduler, add a job and finally run jobs. 
    
    // Create a job scheduler
    JobScheduler jobScheduler = new JobScheduler(new FifoRunnableJobFinder(), new NoOperationPersister());

    // Create a job
    String jobId = "857394";
    String jobType = "send email";
    byte[] data = "{address: 'foo@bar.com', subject: 'welcome', contents: 'bla bla'}".getBytes(charset);
    Instant scheduledAtInstant = Instant.now();
    Job jobToSchedule = new Job(jobId, jobType, data, scheduledAtInstant);

    // Add the job
    jobScheduler.schedule(jobToSchedule);

    // Handle jobs
    while (true) {
        // Get the next job to be executed
        Job job = jobScheduler.startNextRunnableJob("pc83-pid654", 30000);
        if (job == null) {
            // No job present after 30000 millisecond timeout. Wait for a job to be added or to become runnable
            continue;
        }
        if ("send email".equals(job.getType())) {
            // It is a job to send an email. Handle it here.
            try {
                sendEmail(new String(job.getData(), charset));
                // Notify job scheduler that the job has finished.
                jobScheduler.jobFinished(job.getId());
            } catch (Exception e) {
                // Notify job scheduler that the job has failed.
                jobScheduler.jobFailed(job.getId());
            }
        } else {
            // Handle other type of job here
        }
    }
        
You notice that the constructor of `JobScheduler` has two parameters. The first is
a `RunnableJobFinder`, the second is a `JobPersister`. 

The job scheduler forwards all calls to `schedule`, `reschedule`, `jobFinished` 
and `jobFailed` to the `RunnableJobFinder`. The job finder will keep 
the jobs in memory in a data structure that is convenient for quickly determining 
the next runnable job.
 
The method `JobScheduler.tryStartNextRunnableJobUnsynchronized` asks 
the job finder to determine the next runnable job. If such a job is found, 
then its status is updated from `IDLE` to `RUNNING`.

The method `JobScheduler.startNextRunnableJob` is similar to 
`JobScheduler.tryStartNextRunnableJobUnsynchronized`, but if no job is runnable
at the moment of calling this method, it will wait until a job becomes runnable
within a specified amount of time.

A job is considered runnable if it is allowed to be run. An example of a job
that is not runnable would be a job that is scheduled to be executed in one hour.
After the hour has passed the job becomes runnable.

The class `FifoRunnableJobFinder` stores all jobs in an `ArrayList`. When it has to
determine the next runnable job it scans through this `ArrayList` until it finds
a job that has state `IDLE` and with 'scheduled at instant' that is not in the future.

The code above shows how the `JobScheduler` works. However, it still leaves a lot
of boilerplate code to be written. The module `jobschedulerservice` combines
the database ingester and database persister modules to offer a service that
can be used to schedule jobs and let them be executed on a specified number of
threads. The jobs are classes that implement the `Runnable` interface.
The attributes of the classes are persisted in the database in JSON format.
When the job is executed, the instance is created again from this JSON format
and then the `run` method is called. The class name of this object is used
as type of the job. That is the reason why a job must be implemented by a class
instead of using a lambda expression.

Here is sample code for the job scheduler service:

        // Initialize properties. Modify default values if needed. 
        JobIngesterProperties jobIngesterProperties = new JobIngesterProperties();
        DatabaseJobPersisterProperties databaseJobPersisterProperties = new DatabaseJobPersisterProperties();

        // Register data source so that it can be used by the job ingester and database job persister
        DataSource dataSource = ...;
        CompositeDatasourceTransaction.registerDataSource(jobIngesterProperties.getConnectionName(), dataSource);
        CompositeDatasourceTransaction.registerDataSource(databaseJobPersisterProperties.getConnectionName(), dataSource);

        // Creste job scheduler service with maximum 4 threads for executing jobs
        JobSchedulerService jobSchedulerService = new JobSchedulerService(new FifoRunnableJobFinder(), jobIngesterProperties, databaseJobPersisterProperties, 4);

        // Start the processing of jobs
        jobSchedulerService.startProcessingJobs();

        System.out.println("Current time: " + Instant.now());
        // Schedule job to execute immediately
        jobSchedulerService.schedule(new HelloWorldJob());
        // Schedule job to execute after 5 seconds
        jobSchedulerService.schedule(new HelloWorldJob(), Instant.now().plusSeconds(5));

        // Wait till both jobs have been executed
        Thread.sleep(6_000);

        // Stop the processing of jobs
        jobSchedulerService.stopProcessingJobs();
    
## Why building yet another job scheduler and not use an existing one?
 
The reason for writing Job Scheduler is my experience with NServiceBus in a .NET project.
NServiceBus is service bus for .NET, which supports different methods for sending messages
from one enpoint to another and to deal with long running tasks (sagas).
NServiceBus was used with MSMQ as transport, NHibernate for persistence for saga data
and distributed transactions.

Although NServiceBus is a fine product, it did not fulfill our requirements well. Let me
explain what my experiences with NServiceBus were.

### Why NServiceBus did not fulfill our requirements

#### Searching messages

When a client reports a problem, then sometimes this problem is caused because some
message has not been executed or has ended up in an error queue. Therefore, 
an operations engineer must have the ability to search messages.

Messages in queues and error queues are hard to search. 
For our operations engineer we have built a control page showing the number of messages 
per queue. It can also show raw XML for messages in one specific queue. It is very hard
to find a specific message in this screen. Occasionally a developer would use
PowerShell scripts to find a particalur message in a queue, which was an elaborate task.

#### Message priorities

Our system has a lot of long-running jobs to perform. Some jobs are near-realtime
and others are background jobs. The near-realtime jobs should be executed with
a higher priority than the background jobs. Jobs are typically scheduled by sending
a message to and endpoint.

Messages in a queue are handled in a first-in-first-out way. Thus, once messages end up 
in a queue, we cannot change the priorities in which they are handled anymore.

The only way NServiceBus supports priorities is by creating one queue per priority level.
We solved this by creating two instances of the same endpoint, each with their own queue. 
One instance was for the near-realtime jobs and the other one for the background jobs.

One requirement that was mentioned by product management was to change priorities
of sagas per customer. It should be possible that an endpoint first focusses on
the saga of one customer before it would continue working on other sagas. I don't
see an obvious way to implement this with NServiceBus.

#### Load balancing

There are two ways to balance the load using NServiceBus:

1. deploy an endpoint on multiple machines 
2. change the number of worker threads per endpoint.
 
Both steps involve manually changing the configuration and then performing a deployment.
You don't want to make these kind of changes under the pressure of an urgent bug fix.
Even worse, these kind of changes must be made by a developer and cannot be 
made by an operations engineer. And it gets worse: deploying an endpoint
on multiple machines requires an operations engineer to modify the configuration
of their tool to monitor the queue of the new endpoints.

One major drawback of this way of load balancing is the fact the current load of
the application server is not taken into account. Some of our application servers
run endpoints for near-realtime jobs and for background jobs. As long as near-realtime
jobs are available, we want that the only the near-realtime jobs are executed. 
Once the near-realtime jobs have all been finished, then we want the background jobs 
to be executed. And we want that as long as there is a CPU not fully loaded on the application
server that more jobs are being executed. Using NServiceBus we cannot implemeent 
this strategy and never fully utilize the CPUs of the application server. 

#### Sagas

For long running tasks NSerciveBus came up with the notion of a saga. 
A saga is started by a message from a queue. The saga maintains state. 
After the saga has done some work its updated state is stored in the database. 
When a new message arrives for an existing saga, then the state is fetched again 
from the database and another chunk of work is performed. 
When the saga is complete, then its data is removed from the database.

Some of our sagas run for hours. They would run longer than a database transaction
could be open. Therefore, such sagas are executed by performing a little bit of
work that takes perhaps a couple of minutes and then send a message to continue
the work of the saga later. This technique also ensures that all sagas handled
by the endpoint will make progress. 

For some types of long running jobs this last property is actually not what we want. 
Some endpoint was downloading video content in one-minute chunks. Because all sagas
would make progress, video chunks were downloaded in a more-or-less random order,
while we actually wanted the videos to be downloaded one at a time per worker thread.

#### Version check

Some sagas perform version checks on objects to ensure the objects have been committed 
in the database. Even though we use NServiceBus and SQL Server in distributed transactions, 
the following behavior still occurs:

1. Saga 1 requests a commit.
2. Messages sent in saga 1 get committed.
3. Saga 2 starts handling a message that was committed by saga 1.
4. Saga 2 reads data from the database, but does not see the changes made by saga 1.
5. Changes made to the database by saga 1 are committed.

To prevent this problem some sagas check row version of objects to determine 
whether a message has arrived too early. If the message arrived too early,
then the message would be put back in the queue to be handled later.

#### Performance

Currently we use distributed transactions to ensure that commits or rollbacks 
to the database and to the message queues happen together. 
Distributed transactions come with a performance overhead when compared to
non-distributed transactions.

#### Backups and copying messages to acceptance and development environments

Every night the live database is copied to the database of the acceptance 
and development environments. Messages in message queues are not copied. 
So on the acceptance and development environments the messages in the queues
are out-of-sync with the sagas in the database.

### My proposed solution

My idea to solve the above mentioned issues with NServiceBus was to create
one queue containing jobs of different types. This queue must be held
in memory by a single endpoint, named the Job Scheduler Endpoint. 
Whenever another endpoints wants to exectue a job, it requests it
from the Job Scheduler Endpoint.

Adding jobs to, updating jobs in and removing jobs from the queue would be performed
by adding rows with add, update or remove commands to a table in a database.
The Job Scheduler Endpoint polls this table. Any commands found are processed
in the queue and then the commands are removed from the table. Using a shared database
for communication between endpoints removes the burden of distributed transactions.

The final part of the solution is a plugin design that allows you to write
your own strategy for determining which message can be handled. A FIFO implementation
is supplied as example, but it is very simple to implement your own strategey.
You can put limits on the number of jobs  being executed per job type,
give near-realtime jobs priority over background jobs 
and to give jobs of certain customers priority over other jobs. 

Endpoints should request messages to handle until the CPU load 
on the application server passes an upper limit. This ensures that as long
as there are runnable jobs and CPU capacity available, jobs will get executed.
