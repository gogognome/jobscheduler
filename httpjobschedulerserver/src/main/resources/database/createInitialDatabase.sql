CREATE TABLE NlGogognomeJobsToIngest (
  command_id VARCHAR(1000) NOT NULL,
  command VARCHAR(20) NOT NULL,
  id VARCHAR(1000) NOT NULL,
  scheduledAtInstant TIMESTAMP NULL,
  type VARCHAR(1000) NULL,
  data VARCHAR(100000) NULL,
  PRIMARY KEY (command_id)
);

CREATE INDEX idx_NLGogognomeJobsToIngest_id
  ON NlGogognomeJobsToIngest (id);

CREATE SEQUENCE command_id_sequence;

CREATE TABLE NlGogognomeJobs (
  id VARCHAR(1000),
  scheduledAtInstant TIMESTAMP NULL,
  type VARCHAR(1000) NOT NULL,
  data VARCHAR(100000) NULL,
  state VARCHAR(100) NOT NULL,
  requesterId VARCHAR(1000) NULL,
  timeoutAtInstant TIMESTAMP NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_NlGogognomeJobs_id
ON NlGogognomeJobs (id);
