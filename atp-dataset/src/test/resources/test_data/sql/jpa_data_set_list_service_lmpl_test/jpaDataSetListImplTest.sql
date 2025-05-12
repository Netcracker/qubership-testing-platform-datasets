delete from "parameter" p where dataset_id in (select d.id  from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'b703c594-e214-49cc-bda7-4eb51311ddb1'));

delete from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'b703c594-e214-49cc-bda7-4eb51311ddb1');

delete from list_values lv where attribute_id in (select a.id  from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'b703c594-e214-49cc-bda7-4eb51311ddb1'));

delete from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'b703c594-e214-49cc-bda7-4eb51311ddb1');

delete from datasetlist d where visibility_area_id = 'b703c594-e214-49cc-bda7-4eb51311ddb1';

delete from visibility_area v where id = 'b703c594-e214-49cc-bda7-4eb51311ddb1';





INSERT INTO visibility_area
(id, "name")
VALUES('b703c594-e214-49cc-bda7-4eb51311ddb1'::uuid, 'Test JpaDataSetListServiceImplTest Mon Aug 19 10:40:36 SAMT 20240.9274306016666821');

INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 'b703c594-e214-49cc-bda7-4eb51311ddb1'::uuid, 'Test DataSetList 1', NULL, NULL, '2024-08-19 10:40:37.063', NULL, '2024-08-19 10:40:37.063', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 'b703c594-e214-49cc-bda7-4eb51311ddb1'::uuid, 'Test DataSetList 2', NULL, NULL, '2024-08-19 10:40:37.564', NULL, '2024-08-19 10:40:37.564', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 'b703c594-e214-49cc-bda7-4eb51311ddb1'::uuid, 'Test DataSetList 3', NULL, NULL, '2024-08-19 10:40:37.874', NULL, '2024-08-19 10:40:37.874', NULL);

INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('b3118a9f-d9f8-4d20-bc18-4db230df8fd5'::uuid, '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 'Attr 1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5ef82f66-6c8d-4407-83b8-805d43f78870'::uuid, '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 'Attr 2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('55fd601c-cc65-474f-bcca-f2616735e10c'::uuid, '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 'Attr list 1', 4, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('686026e5-26d8-4feb-9b11-c56eb9f72e0d'::uuid, '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 'DSL reference', 5, 4, 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('b59f18b4-ffac-4559-8ba5-214c9075e28d'::uuid, 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 'Attr2  1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('ec86296d-904a-4365-8d45-d6264d51db3a'::uuid, 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 'Attr2  2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('6705e6ed-b14b-4141-a6e8-2f220f76733a'::uuid, 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 'Attr2  list 1', 4, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('6739a1d9-42c3-4a41-8ee5-28c10add3ec2'::uuid, 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 'DSL reference 2', 5, 4, 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('22099158-921a-4251-814e-e809f01878b7'::uuid, 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 'Attr3  1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('358a8644-538c-4248-96a2-ceae184d3b04'::uuid, 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 'Attr3  2', 3, 1, NULL, NULL);

INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('66df6b71-3027-4527-8006-03dfb0a07b9f'::uuid, '55fd601c-cc65-474f-bcca-f2616735e10c'::uuid, 'Value 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('b965aaa4-446f-472d-b843-5a91f8a9404e'::uuid, '55fd601c-cc65-474f-bcca-f2616735e10c'::uuid, 'Value 2', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('fa301dcb-1a3e-4693-a1b2-477338a347be'::uuid, '6705e6ed-b14b-4141-a6e8-2f220f76733a'::uuid, 'Value2 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('8b264da5-c0b6-4475-a01d-baac1ea28e0a'::uuid, '6705e6ed-b14b-4141-a6e8-2f220f76733a'::uuid, 'Value2 2', NULL);

INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('d0af9007-de2c-4089-bca4-63e7d293c5c0'::uuid, 'Test DataSet 1', '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('8f7dfce6-a557-438c-9ff2-eb265904e94c'::uuid, 'Test DataSet 2', '39d0e387-1150-4344-a061-4b81a6a571df'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('76273b56-02f3-4c2b-adf4-76cc17b77edd'::uuid, 'Test DataSet2  1', 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, 'Test DataSet2  2', 'da430230-6c37-42ee-8b6c-f855e6024eed'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('bdfc2261-9c31-4dca-8676-1deff799e7f6'::uuid, 'Test DataSet3  1', 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('e12f3d73-c020-4563-b665-63f7587687d4'::uuid, 'Test DataSet3  2', 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('0ea8ad97-367e-410f-8de5-71584cea46ce'::uuid, 'Test DataSet3  3', 'c5e341f7-8c6e-437b-9af0-850710dffd83'::uuid, 3, NULL, false);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('861c6495-8859-4bc6-8de9-0606f161231c'::uuid, 'd0af9007-de2c-4089-bca4-63e7d293c5c0'::uuid, 'b3118a9f-d9f8-4d20-bc18-4db230df8fd5'::uuid, 'Some value 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('cce01b7f-2cf8-4754-ae20-ed642ac44d12'::uuid, 'd0af9007-de2c-4089-bca4-63e7d293c5c0'::uuid, '55fd601c-cc65-474f-bcca-f2616735e10c'::uuid, NULL, NULL, '66df6b71-3027-4527-8006-03dfb0a07b9f'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ca22c369-d2df-4e68-85c5-3f143810e9fc'::uuid, 'd0af9007-de2c-4089-bca4-63e7d293c5c0'::uuid, '686026e5-26d8-4feb-9b11-c56eb9f72e0d'::uuid, NULL, NULL, NULL, '76273b56-02f3-4c2b-adf4-76cc17b77edd'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d0bfa206-ffea-44bd-ac78-7ef2fbdef3b0'::uuid, 'bdfc2261-9c31-4dca-8676-1deff799e7f6'::uuid, '22099158-921a-4251-814e-e809f01878b7'::uuid, 'Some value3 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('b3ae9c9d-1f7c-4de8-87ba-6711eba12432'::uuid, 'bdfc2261-9c31-4dca-8676-1deff799e7f6'::uuid, '358a8644-538c-4248-96a2-ceae184d3b04'::uuid, 'Some value3 2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('3d9a91b0-6fe1-4a3e-9901-1e000033a6c0'::uuid, '0ea8ad97-367e-410f-8de5-71584cea46ce'::uuid, '358a8644-538c-4248-96a2-ceae184d3b04'::uuid, 'Some value3 4', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('3a811c83-538e-4af5-a3a3-b338559d446c'::uuid, '76273b56-02f3-4c2b-adf4-76cc17b77edd'::uuid, 'b59f18b4-ffac-4559-8ba5-214c9075e28d'::uuid, 'Some value2 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('327407d7-636f-4a21-a749-88694490e1f6'::uuid, '76273b56-02f3-4c2b-adf4-76cc17b77edd'::uuid, '6705e6ed-b14b-4141-a6e8-2f220f76733a'::uuid, NULL, NULL, 'fa301dcb-1a3e-4693-a1b2-477338a347be'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('25a9bca2-565c-4252-95cf-4c7dd9be125d'::uuid, '76273b56-02f3-4c2b-adf4-76cc17b77edd'::uuid, '6739a1d9-42c3-4a41-8ee5-28c10add3ec2'::uuid, NULL, NULL, NULL, 'bdfc2261-9c31-4dca-8676-1deff799e7f6'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('3d4bb1b1-52ab-464e-b01a-fea586d1ff9c'::uuid, 'e12f3d73-c020-4563-b665-63f7587687d4'::uuid, '22099158-921a-4251-814e-e809f01878b7'::uuid, 'Some value3 3', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('4abd1c40-3ab4-474b-b202-f24ac05394e7'::uuid, 'f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, 'b59f18b4-ffac-4559-8ba5-214c9075e28d'::uuid, '#REF_THIS(Attr 2)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('b566f654-c3bf-4e1d-b8cd-b23a4504836f'::uuid, 'f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, 'ec86296d-904a-4365-8d45-d6264d51db3a'::uuid, 'Some value2 2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('71ddc345-9c1e-4b7a-85fa-c95340c6f916'::uuid, 'f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, '6705e6ed-b14b-4141-a6e8-2f220f76733a'::uuid, NULL, NULL, '8b264da5-c0b6-4475-a01d-baac1ea28e0a'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('11afd5fa-f442-431d-9deb-6b12313e700c'::uuid, 'f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, '6739a1d9-42c3-4a41-8ee5-28c10add3ec2'::uuid, NULL, NULL, NULL, 'e12f3d73-c020-4563-b665-63f7587687d4'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('7553e536-91a3-434e-b319-f1f443a17ae0'::uuid, '8f7dfce6-a557-438c-9ff2-eb265904e94c'::uuid, '5ef82f66-6c8d-4407-83b8-805d43f78870'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('fa979320-70ff-4167-8600-ccc7870af95e'::uuid, '8f7dfce6-a557-438c-9ff2-eb265904e94c'::uuid, '55fd601c-cc65-474f-bcca-f2616735e10c'::uuid, NULL, NULL, 'b965aaa4-446f-472d-b843-5a91f8a9404e'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('6028b618-3ad0-4125-8440-a455e2bd4cb7'::uuid, '8f7dfce6-a557-438c-9ff2-eb265904e94c'::uuid, '686026e5-26d8-4feb-9b11-c56eb9f72e0d'::uuid, NULL, NULL, NULL, 'f39d316f-7c97-4894-9179-a5ae78bee4be'::uuid, NULL);
