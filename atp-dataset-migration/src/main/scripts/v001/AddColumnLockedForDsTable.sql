--liquibase formatted sql

--changeset dataset:Add_column_locked_for_dataset_table
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dataset' AND column_name = 'locked') THEN 1 ELSE 0 END
ALTER TABLE dataset ADD COLUMN locked boolean DEFAULT false NOT NULL;