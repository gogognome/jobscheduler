# Job Scheduler Database Ingester
a job ingester that reads commands to manipulate jobs from a database and forwards 
the commands to the job scheduler

## Introduction

This library allows your application (which could be a microservice or a big monolith enterprise application) 
to schedule jobs and notify that
jobs have finished using a `JobScheduler` (see `jobscheduler` module) by writing
job commands in a database table. This library creates a worker thread that polls
for job commands in the database table and forwards them to the job scheduler and then
removes the job commands from the database.

This library is ideal if you need multiple applications to work with
a shared job scheduler instance and if these applications already share
a database. By using the database for 'sending' job commands you have the advantage
that job commands are 'sent' as part of the database transaction. You don't need
distributed transactions as is needed if you are using a message bus like MSMQ.

## Usage

You are responsible for creating a table and a sequence. The name of the table, columns and sequence 
are configurable. The default table and sequence can be created with this SQL command:

    CREATE TABLE NlGogognomeJobsToIngest (
      command_id VARCHAR(1000) NOT NULL,
      command VARCHAR(20) NOT NULL,
      id VARCHAR(1000) NOT NULL,
      scheduledAtInstant TIMESTAMP NULL,
      type VARCHAR(1000) NULL,
      data VARBINARY(100000) NULL,
      PRIMARY KEY (command_id)
    );
    
    CREATE INDEX idx_NLGogognomeJobsToIngest_id
    ON NlGogognomeJobsToIngest (id);
    
    CREATE SEQUENCE command_id_sequence;

If you do not supply a sequence, then your application is responsible for generating a unique
id for the job commands. A simple scheme to follow might be `<server-name>-<process-id>-<sequence-number>`.
Jobs will be ingested by ascending command id, so using a GUID as command id changes the order in which commands
are inserted.

The value of `command` must be `SCHEDULE`, `RESCHEDULE`, `JOB_FINISHED`, `JOB_FAILED` or `REMOVE`. 

Creating a `JobIngesterRunner` requires the following steps:

    JobScheduler jobScheduler = ... // see job scheduler project
    JobIngesterProperties properties = new JobIngesterProperties();
    JobCommandDAO jobCommandDao = new JobCommandDAO(properties);
    JobIngester jobIngester = new JobIngester(jobScheduler, jobCommandDao);
    JobIngesterRunner jobIngesterRunner = new JobIngesterRunner(properties, jobIngester);

This library uses [Gogo Data Access](https://github.com/gogognome/gogodataaccess) to access
the database. All you have to do is register a `DataSource` at initialization time:

    DataSource dataSource = ...; // probably wise to use a DataSource backed up by a connection pool
    CompositeDatasourceTransaction.registerDataSource(properties.getConnectionName(), dataSource);

Once you have an instance of `JobIngesterRunner` you can start a separate thread that will
poll for job commands in a database table and forward all commands to the `JobScheduler` as follows:

    jobIngesterRunner.start();
    
And when you want to stop the `JobIngesterRunner` simply do

    jobIngesterRunner.stop();

For an example of how to use this application, you can check out the `httpjobschedulerserver` module.