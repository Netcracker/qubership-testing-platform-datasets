--liquibase formatted sql

--changeset dataset:EncryptedTypeChangeID
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM ATTRIBUTE_TYPE WHERE NAME = 'ENCRYPTED' AND ID != 6) THEN 1 ELSE 0 END
UPDATE ATTRIBUTE_TYPE SET ID = 6 WHERE NAME = 'ENCRYPTED' AND ID != 6;
