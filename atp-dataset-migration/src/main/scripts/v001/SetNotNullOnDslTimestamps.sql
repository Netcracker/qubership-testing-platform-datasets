--liquibase formatted sql

--changeset dataset:set_not_null_on_datasetlist_timestamps
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'datasetlist' AND column_name = 'created_when' AND is_nullable = 'YES') OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'datasetlist' AND column_name = 'modified_when' AND is_nullable = 'YES') THEN 1 ELSE 0 END
ALTER TABLE datasetlist
ALTER COLUMN created_when SET NOT NULL,
ALTER COLUMN modified_when SET NOT NULL;
