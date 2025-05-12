delete from "parameter" where dataset_id in(select id from dataset where datasetlist_id in
    (select id from datasetlist d where visibility_area_id = 'a2edd07b-1250-4c79-8392-a35ad62f09b4'));

delete from "attribute" where datasetlist_id in (select id from datasetlist d where visibility_area_id = 'a2edd07b-1250-4c79-8392-a35ad62f09b4');

delete from dataset where datasetlist_id in (select id from datasetlist d where visibility_area_id = 'a2edd07b-1250-4c79-8392-a35ad62f09b4');

delete from visibility_area where id = 'a2edd07b-1250-4c79-8392-a35ad62f09b4';

delete from datasetlist where visibility_area_id = 'a2edd07b-1250-4c79-8392-a35ad62f09b4';
