--liquibase formatted sql

--changeset dataset:delete_lost_overlaps
delete from attribute_key ak where not exists (select id from parameter p where p.attribute_id = ak.id)