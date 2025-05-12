--liquibase formatted sql
--changeset dataset:addDateAuditorFields
ALTER TABLE datasetlist ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE datasetlist ADD COLUMN IF NOT EXISTS created_when TIMESTAMP WITH TIME ZONE;
ALTER TABLE datasetlist ADD COLUMN IF NOT EXISTS modified_by UUID;
ALTER TABLE datasetlist ADD COLUMN IF NOT EXISTS modified_when TIMESTAMP WITH TIME ZONE;
