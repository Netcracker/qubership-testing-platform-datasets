delete from "parameter" p where dataset_id in (select d.id  from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd'));

delete from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd');

delete from list_values lv where attribute_id in (select a.id  from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd'));

delete from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd');

delete from datasetlist d where visibility_area_id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd';

delete from visibility_area v where id = 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd';


INSERT INTO visibility_area
(id, "name")
VALUES('c904edfc-310d-4b01-8af2-5cc4f1d86fcd'::uuid, 'Test JpaParameterServiceImplTestMon Aug 19 17:56:49 SAMT 20240.3398895130881372');

INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 'c904edfc-310d-4b01-8af2-5cc4f1d86fcd'::uuid, 'Test DataSetList 1', NULL, NULL, '2024-08-19 17:56:49.508', NULL, '2024-08-19 17:56:49.508', NULL);

INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('beb2484b-fb54-44dd-84fd-d759329c163b'::uuid, 'b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 'Attr 1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('08582328-90b8-49c7-9ab8-e676e8b896f8'::uuid, 'b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 'Attr 2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('faadc4c6-0089-403d-a0ca-8968d4ec1769'::uuid, 'b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 'Attr list 3', 4, 3, NULL, NULL);

INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('76d1b8fd-f30e-4326-a188-2c0bc4ff60f0'::uuid, 'faadc4c6-0089-403d-a0ca-8968d4ec1769'::uuid, 'list Value 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('329dea6f-a93e-46e1-8825-c5791783c1ec'::uuid, 'faadc4c6-0089-403d-a0ca-8968d4ec1769'::uuid, 'list Value 2', NULL);

INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('2f27efc6-bfa0-4a00-9064-f20cb2afb3b8'::uuid, 'DS 1', 'b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('18a8b8db-4c8b-41a8-af5e-ca366dc99f91'::uuid, 'DS 2', 'b8444ae9-bb06-47b6-a895-eedd5f2f4f60'::uuid, 2, NULL, false);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('4cfd2426-ad08-4d02-8501-dbcb83ce6825'::uuid, '2f27efc6-bfa0-4a00-9064-f20cb2afb3b8'::uuid, 'beb2484b-fb54-44dd-84fd-d759329c163b'::uuid, 'Value 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('57a9e15e-0cdb-4b85-ba17-d70812b0b1df'::uuid, '2f27efc6-bfa0-4a00-9064-f20cb2afb3b8'::uuid, '08582328-90b8-49c7-9ab8-e676e8b896f8'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('210fb6ac-08dc-4e47-bfd7-2ae980bcf586'::uuid, '2f27efc6-bfa0-4a00-9064-f20cb2afb3b8'::uuid, 'faadc4c6-0089-403d-a0ca-8968d4ec1769'::uuid, NULL, NULL, '76d1b8fd-f30e-4326-a188-2c0bc4ff60f0'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ccedbecc-deb6-456e-9b30-ae77ddfde157'::uuid, '18a8b8db-4c8b-41a8-af5e-ca366dc99f91'::uuid, 'beb2484b-fb54-44dd-84fd-d759329c163b'::uuid, 'Value 2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('fe2737d8-0a20-4a43-85ac-bea033a0396c'::uuid, '18a8b8db-4c8b-41a8-af5e-ca366dc99f91'::uuid, 'faadc4c6-0089-403d-a0ca-8968d4ec1769'::uuid, NULL, NULL, '329dea6f-a93e-46e1-8825-c5791783c1ec'::uuid, NULL, NULL);
