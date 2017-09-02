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
