CREATE TABLE NlGogognomeJobsToIngest (
  id VARCHAR(1000),
  command VARCHAR(20) NOT NULL,
  scheduledAtInstant TIMESTAMP NULL,
  type VARCHAR(1000) NULL,
  data VARBINARY(100000) NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_NLGogognomeJobsToIngest_id
  ON NlGogognomeJobsToIngest (id);

CREATE TABLE NlGogognomeJobs (
  id VARCHAR(1000),
  scheduledAtInstant TIMESTAMP NULL,
  type VARCHAR(1000) NOT NULL,
  data VARBINARY(100000) NULL,
  state VARCHAR(100) NOT NULL,
  requesterId VARCHAR(1000) NULL,
  timeoutAtInstant TIMESTAMP NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_NlGogognomeJobs_id
  ON NlGogognomeJobs (id);
