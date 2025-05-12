--liquibase formatted sql

--changeset dataset:add_new_column_for_saga_transaction
ALTER TABLE datasetlist
ADD COLUMN IF NOT EXISTS saga_session_id UUID;

CREATE INDEX IF NOT EXISTS idx_visibility_area_id_and_saga_session_id ON datasetlist (visibility_area_id, saga_session_id)
WHERE visibility_area_id IS NOT NULL AND saga_session_id IS NOT NULL;