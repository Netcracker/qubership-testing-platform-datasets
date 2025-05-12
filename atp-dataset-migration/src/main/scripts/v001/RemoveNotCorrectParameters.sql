--liquibase formatted sql

--changeset dataset:remove_not_correct_parameters
delete from "parameter" where id in (
select p.id from
"parameter" p,
"dataset" d,
"attribute" a
where p.dataset_id = d.id and p.attribute_id = a.id and d.datasetlist_id != a.datasetlist_id);