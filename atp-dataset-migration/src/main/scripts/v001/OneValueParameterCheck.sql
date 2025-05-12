--liquibase formatted sql

-- =========================================
-- Adding CHECK constraint to ensure only one value column is used
-- =========================================

--changeset dataset:add_one_value_parameter_constraint
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name = 'parameter' AND constraint_type = 'CHECK' AND constraint_name = 'onevalueparametercheck') THEN 1 ELSE 0 END
ALTER TABLE parameter ADD CONSTRAINT oneValueParameterCheck CHECK (
    (string IS NULL AND file IS NULL AND list IS NULL AND ds IS NOT NULL)
    OR
    (string IS NOT NULL AND file IS NULL AND list IS NULL AND ds IS NULL)
    OR
    (string IS NULL AND file IS NOT NULL AND list IS NULL AND ds IS NULL)
    OR
    (string IS NULL AND file IS NULL AND list IS NOT NULL AND ds IS NULL)
    OR
    (string IS NULL AND file IS NULL AND list IS NULL AND ds IS NULL)
);
