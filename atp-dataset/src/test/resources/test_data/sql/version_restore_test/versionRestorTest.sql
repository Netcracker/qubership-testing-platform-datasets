delete from "parameter" where id in ('5f321e9d-fff4-4de9-b981-aaa4bcf1112f', '01da9b97-080d-4dbd-bbb7-a40fc1563dca', 'db738b17-4d4e-4737-9e9c-9b8f297189e4', '9a01dcd3-453b-4c6a-9478-3bce000ea20f', 'f6d23b9a-259b-49d0-8285-c5789c35a6ca', '6dc5f60b-e237-40b7-9143-d2ee70e1c3a1', '01da9b97-080d-4dbd-bbb7-a40fc1563dca', 'db738b17-4d4e-4737-9e9c-9b8f297189e4');

delete from dataset where id in ('8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c', 'e4c108f5-f1dc-4c78-af24-c14941972e5a', 'f381c812-782a-45bb-81d4-dde9cf0adcbc', '94872bbc-97c7-4b0f-8bff-f307d98667b3', 'a0a96684-b917-4c36-88ca-0ece3c360185');

delete from "list_values" where id in ('50dda1c1-8005-4c90-a2e4-31d272dc6156', '9acc4006-3683-4f5c-bd52-af7c876932fd');

delete from "attribute" where id in ('f3dd96b9-c1a9-47af-af61-06c5fcb1eb6e', '904d97bf-f817-4383-ac08-120d25420b93', '74dca9e3-5afd-4997-a183-26b919e04bf8', '94872bbc-97c7-4b0f-8bff-f307d98667b3', '5a5a858e-78d3-42ce-aa70-647af8d8e425');

delete from datasetlist where id in ('f546d8c7-7486-4813-adab-8271a9df76f7', '94872bbc-97c7-4b0f-8bff-f307d98667b3');

delete from visibility_area where id = '04852246-541c-4bd9-83f7-16050342252c';

INSERT INTO visibility_area
(id, "name")
VALUES('04852246-541c-4bd9-83f7-16050342252c'::uuid, 'Versioning Restore');

INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('f546d8c7-7486-4813-adab-8271a9df76f7'::uuid, '04852246-541c-4bd9-83f7-16050342252c'::uuid, 'Second Level DSL', NULL, NULL, '2024-08-17 13:03:44.153', NULL, '2024-08-17 13:03:44.153', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, '04852246-541c-4bd9-83f7-16050342252c'::uuid, 'Root Data Set List', NULL, NULL, '2024-08-17 13:03:43.526', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2024-08-17 13:03:44.272', NULL);

INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('f3dd96b9-c1a9-47af-af61-06c5fcb1eb6e'::uuid, 'f546d8c7-7486-4813-adab-8271a9df76f7'::uuid, 'Second Text attribute', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('904d97bf-f817-4383-ac08-120d25420b93'::uuid, '94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, 'Text attribute', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('74dca9e3-5afd-4997-a183-26b919e04bf8'::uuid, '94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, 'List attribute', 3, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5a5a858e-78d3-42ce-aa70-647af8d8e425'::uuid, '94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, 'Reference to second level', 4, 4, 'f546d8c7-7486-4813-adab-8271a9df76f7'::uuid, NULL);

INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('50dda1c1-8005-4c90-a2e4-31d272dc6156'::uuid, '74dca9e3-5afd-4997-a183-26b919e04bf8'::uuid, 'list Value 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('9acc4006-3683-4f5c-bd52-af7c876932fd'::uuid, '74dca9e3-5afd-4997-a183-26b919e04bf8'::uuid, 'list Value 2', NULL);

INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c'::uuid, 'Data Set 1', 'f546d8c7-7486-4813-adab-8271a9df76f7'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('e4c108f5-f1dc-4c78-af24-c14941972e5a'::uuid, 'Data Set 2', 'f546d8c7-7486-4813-adab-8271a9df76f7'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('f381c812-782a-45bb-81d4-dde9cf0adcbc'::uuid, 'Data Set 1', '94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('a0a96684-b917-4c36-88ca-0ece3c360185'::uuid, 'Data Set 2', '94872bbc-97c7-4b0f-8bff-f307d98667b3'::uuid, 2, NULL, false);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('db738b17-4d4e-4737-9e9c-9b8f297189e4'::uuid, 'f381c812-782a-45bb-81d4-dde9cf0adcbc'::uuid, '904d97bf-f817-4383-ac08-120d25420b93'::uuid, 'Data Set 1 text value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('01da9b97-080d-4dbd-bbb7-a40fc1563dca'::uuid, 'f381c812-782a-45bb-81d4-dde9cf0adcbc'::uuid, '74dca9e3-5afd-4997-a183-26b919e04bf8'::uuid, NULL, NULL, '50dda1c1-8005-4c90-a2e4-31d272dc6156'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('5f321e9d-fff4-4de9-b981-aaa4bcf1112f'::uuid, 'f381c812-782a-45bb-81d4-dde9cf0adcbc'::uuid, '5a5a858e-78d3-42ce-aa70-647af8d8e425'::uuid, NULL, NULL, NULL, '8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('6dc5f60b-e237-40b7-9143-d2ee70e1c3a1'::uuid, '8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c'::uuid, 'f3dd96b9-c1a9-47af-af61-06c5fcb1eb6e'::uuid, 'Second Data Set 1 text value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('f6d23b9a-259b-49d0-8285-c5789c35a6ca'::uuid, 'a0a96684-b917-4c36-88ca-0ece3c360185'::uuid, '904d97bf-f817-4383-ac08-120d25420b93'::uuid, 'Data Set 2 text value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('9a01dcd3-453b-4c6a-9478-3bce000ea20f'::uuid, 'e4c108f5-f1dc-4c78-af24-c14941972e5a'::uuid, 'f3dd96b9-c1a9-47af-af61-06c5fcb1eb6e'::uuid, 'Second Data Set 2 text value', NULL, NULL, NULL, NULL);
