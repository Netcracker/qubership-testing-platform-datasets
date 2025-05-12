delete from "parameter" p where dataset_id in (select d.id  from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c'));

delete from attribute_key where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c');

delete from dataset d where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c');

delete from list_values lv where attribute_id in (select a.id  from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c'));

delete from "attribute" a where datasetlist_id in (select d.id from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c');

delete from datasetlist d where visibility_area_id = 'f261ed65-a491-44b0-af38-f0d97a46008c';

delete from visibility_area v where id = 'f261ed65-a491-44b0-af38-f0d97a46008c';


INSERT INTO visibility_area
(id, "name")
VALUES('f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Test VA Mon Aug 19 17:09:22 SAMT 20240.7282443931002298');

INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Test DataSetList 1', NULL, NULL, '2024-08-19 17:09:23.359', NULL, '2024-08-19 17:09:23.359', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Test DataSetList 2', NULL, NULL, '2024-08-19 17:09:23.726', NULL, '2024-08-19 17:09:23.726', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('d7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Test DataSetList 3', NULL, NULL, '2024-08-19 17:09:23.920', NULL, '2024-08-19 17:09:23.920', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Root DSL', NULL, NULL, '2024-08-19 17:09:24.076', NULL, '2024-08-19 17:09:24.076', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('573c5fd3-d006-4b5f-94de-9a6314a7635a'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'Child DSL', NULL, NULL, '2024-08-19 17:09:24.124', NULL, '2024-08-19 17:09:24.124', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'SourceDsl', NULL, NULL, '2024-08-19 17:09:24.245', NULL, '2024-08-19 17:09:24.245', NULL);
INSERT INTO datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('8850a2a8-5409-4fe5-93f2-a5107c5e12b0'::uuid, 'f261ed65-a491-44b0-af38-f0d97a46008c'::uuid, 'sourceDslParameter', NULL, NULL, '2024-08-19 17:09:24.334', NULL, '2024-08-19 17:09:24.334', NULL);

INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('0d801cb9-acc4-4d29-a485-ba85ba88d919'::uuid, '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'Attr 1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('a5e2911d-4609-42d4-b8cb-5e5ed628bdd4'::uuid, '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'Attr 2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('d1963b33-9787-4b25-8d92-7c63d3716f79'::uuid, '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'Attr list 1', 4, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9832373e-3289-40d8-8783-c7ee48d2e7ea'::uuid, '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'DSL reference', 5, 4, 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('db885865-dfcb-477d-a042-65379b05978a'::uuid, 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 'Attr2  1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('d66b9552-ee34-49cb-98ba-54eaf1183bbf'::uuid, 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 'Attr2  2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('fa076731-b32b-4cdc-8685-781be5ddbca5'::uuid, 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 'Attr2  list 1', 4, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('4f10e979-4106-486f-9641-23a71ee17413'::uuid, 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 'DSL reference 2', 5, 4, 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5dee0429-a443-4ce7-9ff2-bccbe4852a92'::uuid, 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 'Attr3  1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('caec293c-2612-4380-b4bf-f176e76f34ff'::uuid, 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 'Attr3  2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('7874e9fa-ea75-46d8-ab77-7207fbd8807b'::uuid, 'b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 'Some text 1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9773bb74-a877-4719-833f-7c6aa883906e'::uuid, 'b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 'Some text 2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('85f6ad9f-001d-4dad-bcbc-66593e87f11b'::uuid, 'b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 'DSL reference', 4, 4, '573c5fd3-d006-4b5f-94de-9a6314a7635a'::uuid, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9aa6b098-2006-4185-bb28-7240f81977bd'::uuid, '573c5fd3-d006-4b5f-94de-9a6314a7635a'::uuid, 'Some var 1', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('489d4e30-e327-41b5-8328-0454804b5ac9'::uuid, '573c5fd3-d006-4b5f-94de-9a6314a7635a'::uuid, 'Some var 2', 3, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('2e70a5cf-815a-40ee-b258-90a1b05f709b'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'text_attr', 2, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('36f8ff3d-3291-4274-bbee-5a468db5f7d2'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'list_attr', 3, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9923d9ec-81b8-4dca-b762-f663ed24595b'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'dsl_attr', 4, 4, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('4079aab2-3d5e-4d86-9217-f66524808ec3'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'change_attr', 5, 5, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('81a84637-62a4-4d8b-8159-a6aeb20e52f5'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'enc_attr', 6, 6, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('6871e447-2f61-4fd7-ae4c-dc26aebdc607'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'text_attr_without_parameter', 7, 1, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('575e88a0-f83d-48d6-bf3a-adfda05d00f4'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'list_attr_without_parameter', 8, 3, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('132eadd7-4f7e-4fe3-91b8-e36a5e36b672'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'dsl_attr_without_parameter', 9, 4, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('062276b4-29a6-48a5-8348-af3a1165fcd8'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'change_attr_without_parameter', 10, 5, NULL, NULL);
INSERT INTO "attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('89520827-8daf-4f3a-ba05-d1369b63e636'::uuid, '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 'enc_attr_without_parameter', 11, 6, NULL, NULL);

INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('a55255f8-a0b0-48bb-84af-091a960db6cf'::uuid, 'd1963b33-9787-4b25-8d92-7c63d3716f79'::uuid, 'Value 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('d90aaacf-d7ea-4c4c-8a4c-73658507ef17'::uuid, 'd1963b33-9787-4b25-8d92-7c63d3716f79'::uuid, 'Value 2', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('f24f6ff1-e96e-44ba-b1eb-205068daa4c5'::uuid, 'fa076731-b32b-4cdc-8685-781be5ddbca5'::uuid, 'Value2 1', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('980b3537-50b3-44f2-a2b2-c3daad1ed918'::uuid, 'fa076731-b32b-4cdc-8685-781be5ddbca5'::uuid, 'Value2 2', NULL);
INSERT INTO list_values
(id, attribute_id, "text", source_id)
VALUES('b3bfa2d0-214a-485b-8ff6-2a37f0e8f17e'::uuid, '36f8ff3d-3291-4274-bbee-5a468db5f7d2'::uuid, '1', NULL);

INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, 'Test DataSet 1', '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'Test DataSet 2', '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('b7f95224-7c38-4d99-8a3b-ca1289dff61a'::uuid, 'Test DataSet2  1', 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, 'Test DataSet2  2', 'a29a1488-a8b9-4c47-9761-ea6121e6d27d'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('6e8adeb2-ff27-4ea7-bb81-35e90d1880b1'::uuid, 'Test DataSet3  1', 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('196885c2-6a4f-407e-a506-301e23894763'::uuid, 'Test DataSet3  2', 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 2, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('b893b683-0b17-4f76-a96f-c997af88f7b2'::uuid, 'Test DataSet3  3', 'd7edcb2c-17ec-4c28-8654-e751e8adb819'::uuid, 3, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('fe703972-1b4b-41d5-837b-ab3824de5269'::uuid, 'DataSet', 'b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('879993d2-1733-4f0b-87d7-c7ef6e909e0e'::uuid, 'DataSet', '573c5fd3-d006-4b5f-94de-9a6314a7635a'::uuid, 1, NULL, false);
INSERT INTO dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, 'SourceDS', '3bfdeabc-c6a7-450e-a104-98897df89dd9'::uuid, 1, NULL, false);

INSERT INTO attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('fca17877-c884-49b6-9b59-115f45c302ac'::uuid, '9832373e-3289-40d8-8783-c7ee48d2e7ea', '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'd66b9552-ee34-49cb-98ba-54eaf1183bbf'::uuid, NULL);
INSERT INTO attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('b022ced2-b5c0-4d0b-9662-322609bb9bc8'::uuid, '9832373e-3289-40d8-8783-c7ee48d2e7ea', '49119346-cec2-4801-a004-7820f379ca0e'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, '4f10e979-4106-486f-9641-23a71ee17413'::uuid, NULL);
INSERT INTO attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('0428319e-5c74-4b2c-808f-55777c247796'::uuid, '9832373e-3289-40d8-8783-c7ee48d2e7ea_4f10e979-4106-486f-9641-23a71ee17413', '49119346-cec2-4801-a004-7820f379ca0e'::uuid, '10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, 'caec293c-2612-4380-b4bf-f176e76f34ff'::uuid, NULL);
INSERT INTO attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('f50775e1-01f9-419f-9b6d-ab4c8a732ef6'::uuid, '85f6ad9f-001d-4dad-bcbc-66593e87f11b', 'b9452a06-264c-4f82-9c6d-bfeb7bf622a6'::uuid, 'fe703972-1b4b-41d5-837b-ab3824de5269'::uuid, '489d4e30-e327-41b5-8328-0454804b5ac9'::uuid, NULL);

INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('f3b397a0-f12e-483d-9b32-7b42738290f9'::uuid, 'b7f95224-7c38-4d99-8a3b-ca1289dff61a'::uuid, 'db885865-dfcb-477d-a042-65379b05978a'::uuid, 'Some value2 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c9e82573-5390-48f4-859d-894acb954a3e'::uuid, 'b7f95224-7c38-4d99-8a3b-ca1289dff61a'::uuid, 'fa076731-b32b-4cdc-8685-781be5ddbca5'::uuid, NULL, NULL, 'f24f6ff1-e96e-44ba-b1eb-205068daa4c5'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('62940d81-7316-4929-a349-c69e08ebbadb'::uuid, 'b7f95224-7c38-4d99-8a3b-ca1289dff61a'::uuid, '4f10e979-4106-486f-9641-23a71ee17413'::uuid, NULL, NULL, NULL, '6e8adeb2-ff27-4ea7-bb81-35e90d1880b1'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c5707b31-e259-46d7-9a3d-9fdac1354e55'::uuid, '196885c2-6a4f-407e-a506-301e23894763'::uuid, '5dee0429-a443-4ce7-9ff2-bccbe4852a92'::uuid, 'Some value3 3', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('bfbda8bc-35e3-4974-8cb9-1208dee82a05'::uuid, '10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, '0d801cb9-acc4-4d29-a485-ba85ba88d919'::uuid, 'Some value 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d0307e7e-c0d4-470e-842e-57de5c377748'::uuid, '10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, 'd1963b33-9787-4b25-8d92-7c63d3716f79'::uuid, NULL, NULL, 'a55255f8-a0b0-48bb-84af-091a960db6cf'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('4b03a18b-6c33-4623-8853-ed184e0f7e67'::uuid, '10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, '9832373e-3289-40d8-8783-c7ee48d2e7ea'::uuid, NULL, NULL, NULL, 'b7f95224-7c38-4d99-8a3b-ca1289dff61a'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('48296793-364c-4525-b658-0a8292c9e203'::uuid, '10c9ebb2-dc04-4486-ba30-df814ec6be72'::uuid, '0428319e-5c74-4b2c-808f-55777c247796'::uuid, 'Some overlap value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ee56c8d7-3fe3-431d-8d82-b2bae2043b7b'::uuid, '6e8adeb2-ff27-4ea7-bb81-35e90d1880b1'::uuid, '5dee0429-a443-4ce7-9ff2-bccbe4852a92'::uuid, 'Some value3 1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('6a0a12e0-c5cb-43ed-906c-7422bb037e59'::uuid, '6e8adeb2-ff27-4ea7-bb81-35e90d1880b1'::uuid, 'caec293c-2612-4380-b4bf-f176e76f34ff'::uuid, 'Some value3 2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('318d84b5-40f2-4c77-8a21-fddd191054ae'::uuid, '96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, 'db885865-dfcb-477d-a042-65379b05978a'::uuid, '#REF_THIS(Attr 2)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('7047237a-34de-48cb-af73-8e769f1cccc9'::uuid, '96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, 'd66b9552-ee34-49cb-98ba-54eaf1183bbf'::uuid, 'Some value2 2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('e61f2948-a913-43fe-91bd-55a4588263d4'::uuid, '96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, 'fa076731-b32b-4cdc-8685-781be5ddbca5'::uuid, NULL, NULL, '980b3537-50b3-44f2-a2b2-c3daad1ed918'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('5ab72bec-c7eb-474c-81da-13e1e397961e'::uuid, '96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, '4f10e979-4106-486f-9641-23a71ee17413'::uuid, NULL, NULL, NULL, '196885c2-6a4f-407e-a506-301e23894763'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d67adb7a-f54e-418b-b5b8-7a9c97c0a449'::uuid, 'fe703972-1b4b-41d5-837b-ab3824de5269'::uuid, '7874e9fa-ea75-46d8-ab77-7207fbd8807b'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('dbebca17-7a05-4bf7-a6fd-e357ccc4e7bd'::uuid, 'fe703972-1b4b-41d5-837b-ab3824de5269'::uuid, '9773bb74-a877-4719-833f-7c6aa883906e'::uuid, 'Val2', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d2288f10-2549-4876-84f6-5049a6279c37'::uuid, 'fe703972-1b4b-41d5-837b-ab3824de5269'::uuid, 'f50775e1-01f9-419f-9b6d-ab4c8a732ef6'::uuid, 'Some overlap value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('16059792-dc06-4ab5-a148-250a89dfbb9d'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'a5e2911d-4609-42d4-b8cb-5e5ed628bdd4'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('81d807b5-a5c6-40f6-9f04-03fdb4aac514'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'd1963b33-9787-4b25-8d92-7c63d3716f79'::uuid, NULL, NULL, 'd90aaacf-d7ea-4c4c-8a4c-73658507ef17'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('cb37082b-a380-46fd-a1bf-90f0d5e5a08d'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, '9832373e-3289-40d8-8783-c7ee48d2e7ea'::uuid, NULL, NULL, NULL, '96ba075a-1436-488a-90a0-30b3d58584f9'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('de981f64-20bc-4e81-9017-9a3d16de1521'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'fca17877-c884-49b6-9b59-115f45c302ac'::uuid, 'Some overlap value', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c1d8465a-c028-41c8-a155-8d15b3d39938'::uuid, 'f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c'::uuid, 'b022ced2-b5c0-4d0b-9662-322609bb9bc8'::uuid, NULL, NULL, NULL, 'b893b683-0b17-4f76-a96f-c997af88f7b2'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('38ae690c-bfa7-49da-add2-dd1b5e7edd6b'::uuid, '336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, '2e70a5cf-815a-40ee-b258-90a1b05f709b'::uuid, 'text1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('beb67634-ef8a-4073-95c5-9aa24296345a'::uuid, '336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, '36f8ff3d-3291-4274-bbee-5a468db5f7d2'::uuid, NULL, NULL, 'b3bfa2d0-214a-485b-8ff6-2a37f0e8f17e'::uuid, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('47a8e6a9-0b8d-4843-a393-6d0f47bb3ff5'::uuid, '336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, '9923d9ec-81b8-4dca-b762-f663ed24595b'::uuid, NULL, NULL, NULL, '8850a2a8-5409-4fe5-93f2-a5107c5e12b0'::uuid, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('acd91774-2676-42e6-9782-cda541410bd6'::uuid, '336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, '4079aab2-3d5e-4d86-9217-f66524808ec3'::uuid, 'changeType1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('21a2aaca-57af-4cb4-ac78-a9e310e3a91f'::uuid, '336e0cc9-dee2-458c-887e-bcdc406ae4be'::uuid, '81a84637-62a4-4d8b-8159-a6aeb20e52f5'::uuid, 'encType1', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c0027c68-a4af-4048-be1d-74f590f85ace'::uuid, 'b893b683-0b17-4f76-a96f-c997af88f7b2'::uuid, 'caec293c-2612-4380-b4bf-f176e76f34ff'::uuid, 'Some value3 4', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('522dc335-0d4d-40fa-91dc-8f6a6802d849'::uuid, '879993d2-1733-4f0b-87d7-c7ef6e909e0e'::uuid, '9aa6b098-2006-4185-bb28-7240f81977bd'::uuid, '32#RANDOMBETWEEN(48000000,48999999)', NULL, NULL, NULL, NULL);
INSERT INTO "parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('dbd14e98-d4eb-4ef8-924e-5b21fb8d0b57'::uuid, '879993d2-1733-4f0b-87d7-c7ef6e909e0e'::uuid, '489d4e30-e327-41b5-8328-0454804b5ac9'::uuid, 'Val2', NULL, NULL, NULL, NULL);
