--liquibase formatted sql

--changeset dataset:insert_encrypted_type_if_missing
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN NOT EXISTS (SELECT 1 FROM ATTRIBUTE_TYPE WHERE NAME = 'ENCRYPTED') THEN 1 ELSE 0 END
INSERT INTO ATTRIBUTE_TYPE (NAME)
SELECT 'ENCRYPTED'
WHERE NOT EXISTS (
    SELECT 1 FROM ATTRIBUTE_TYPE WHERE NAME = 'ENCRYPTED'
);
