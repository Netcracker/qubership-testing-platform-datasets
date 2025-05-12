--liquibase formatted sql
--changeset addStructureChangeAttrType
INSERT INTO ATTRIBUTE_TYPE (NAME) VALUES ('CHANGE');
