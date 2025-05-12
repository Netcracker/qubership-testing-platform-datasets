--liquibase formatted sql
--changeset dataset:fillAttrTypes
INSERT INTO ATTRIBUTE_TYPE (NAME) VALUES ('TEXT'), ('FILE'), ('LIST'), ('DSL');