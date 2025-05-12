delete  from "parameter" p where dataset_id in (
    select id from dataset d where datasetlist_id in (
        select id from datasetlist d where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'
    ));

delete from attribute_key ak where datasetlist_id in (
    select id from datasetlist d where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'
);

delete from list_values where attribute_id in
 (select id from "attribute" a where datasetlist_id
 in (select id from datasetlist d where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'));

delete from "attribute" a where datasetlist_id in (
select id from datasetlist d where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'
);

delete from dataset d where datasetlist_id in (
    select id from datasetlist d where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'
);

delete from datasetlist where visibility_area_id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb';

delete from visibility_area where id ='46094b3a-dc76-46f6-9a38-e3af2a8cc9fb';
