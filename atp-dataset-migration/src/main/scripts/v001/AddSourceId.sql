--liquibase formatted sql
--changeset dataset:add_source_id
ALTER TABLE dataset
    ADD COLUMN IF NOT EXISTS source_id UUID;
ALTER TABLE datasetlist
    ADD COLUMN IF NOT EXISTS source_id UUID;
ALTER TABLE attribute
    ADD COLUMN IF NOT EXISTS source_id UUID;
ALTER TABLE attribute_key
    ADD COLUMN IF NOT EXISTS source_id UUID;
ALTER TABLE parameter
    ADD COLUMN IF NOT EXISTS source_id UUID;
ALTER TABLE list_values
    ADD COLUMN IF NOT EXISTS source_id UUID;
