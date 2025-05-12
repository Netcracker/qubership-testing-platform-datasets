--liquibase formatted sql
--changeset dataset:modified_when_and_created_when_set_timestamp_rev2
UPDATE datasetlist
SET modified_when = make_timestamptz(1970, 01, 01, 0,  0, 1)
WHERE modified_when IS null;

UPDATE datasetlist
SET created_when = make_timestamptz(1970, 01, 01, 0,  0, 1)
WHERE created_when IS null;