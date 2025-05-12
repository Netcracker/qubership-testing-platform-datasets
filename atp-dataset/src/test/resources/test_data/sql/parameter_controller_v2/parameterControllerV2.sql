delete from "parameter" p where dataset_id in (select d.id  from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = '5bf7f388-8969-409f-98e0-fcfb3efe0817'));

delete from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = '5bf7f388-8969-409f-98e0-fcfb3efe0817');

delete from list_values lv where attribute_id in (select a.id  from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = '5bf7f388-8969-409f-98e0-fcfb3efe0817'));

delete from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = '5bf7f388-8969-409f-98e0-fcfb3efe0817');

delete from datasetlist d where visibility_area_id = '5bf7f388-8969-409f-98e0-fcfb3efe0817';

delete from visibility_area v where id = '5bf7f388-8969-409f-98e0-fcfb3efe0817';


INSERT INTO visibility_area
(id, name)
VALUES('5bf7f388-8969-409f-98e0-fcfb3efe0817'::uuid, 'ParameterControllerV2');


INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('646b1bbb-e392-4364-910d-e54e8609ea8a'::uuid, '5bf7f388-8969-409f-98e0-fcfb3efe0817'::uuid, 'Test DataSetList 1', NULL, NULL, '2024-08-19 17:56:49.508', NULL, '2024-08-19 17:56:49.508', NULL);


INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('2782bcd4-76e1-4093-88db-088e5d4eb829'::uuid, '646b1bbb-e392-4364-910d-e54e8609ea8a'::uuid, 'Attr 1', 0, 1, NULL, NULL);


INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('c360900b-d558-4907-b4c4-d596df40d07c'::uuid, 'DS 1', '646b1bbb-e392-4364-910d-e54e8609ea8a'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('fe3257df-6864-434a-a5ed-4b78a98f60a1'::uuid, 'DS 2', '646b1bbb-e392-4364-910d-e54e8609ea8a'::uuid, 2, NULL, false);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d0c2a5ce-2864-4f17-a3eb-d696f3b008ea'::uuid, 'c360900b-d558-4907-b4c4-d596df40d07c'::uuid, '2782bcd4-76e1-4093-88db-088e5d4eb829'::uuid, 'Value 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('3b02168f-c98b-4277-ad63-70a0dd3be309'::uuid, 'fe3257df-6864-434a-a5ed-4b78a98f60a1'::uuid, '2782bcd4-76e1-4093-88db-088e5d4eb829'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);



INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('ea7c10e7-7ab4-437e-b7f8-3961d3bf299d'::uuid, '5bf7f388-8969-409f-98e0-fcfb3efe0817'::uuid, 'Test DataSetList 2', NULL, NULL, '2024-08-19 17:56:49.508', NULL, '2024-08-19 17:56:49.508', NULL);


INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('c66e105a-701a-4e76-b7fa-e4fafc234bff'::uuid, 'ea7c10e7-7ab4-437e-b7f8-3961d3bf299d'::uuid, 'Attr 1', 0, 1, NULL, NULL);


INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('36554046-85ed-4606-82f7-e78040b63b71'::uuid, 'DS 1', 'ea7c10e7-7ab4-437e-b7f8-3961d3bf299d'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('a7ce3f23-5363-46e3-99ee-c1943c984747'::uuid, 'DS 2', 'ea7c10e7-7ab4-437e-b7f8-3961d3bf299d'::uuid, 2, NULL, false);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('5fe9e373-d709-4d96-b07d-c5a1d0b78ed3'::uuid, '36554046-85ed-4606-82f7-e78040b63b71'::uuid, 'c66e105a-701a-4e76-b7fa-e4fafc234bff'::uuid, 'Value 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('36b8f581-c378-47d7-adec-3ff80e9d8999'::uuid, 'a7ce3f23-5363-46e3-99ee-c1943c984747'::uuid, 'c66e105a-701a-4e76-b7fa-e4fafc234bff'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
