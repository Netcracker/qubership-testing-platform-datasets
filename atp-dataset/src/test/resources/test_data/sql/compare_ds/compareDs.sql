-- va
INSERT INTO public.visibility_area
(id, "name")
VALUES('a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'compareDs');

-- ref dsl
INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('c3ebb1c6-85af-4118-84e5-5f89d9c5c202'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'ref_dsl1', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:21.671', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:21.833', NULL);

-- ds
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('b2a10672-e694-4eaf-aa64-194880e30257'::uuid, 'ref_ds', 'c3ebb1c6-85af-4118-84e5-5f89d9c5c202'::uuid, 7824, NULL, false);

-- dsl
INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('2d3baddd-42ec-4c6d-a8d1-37425b1a6b47'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl1', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:21.990', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:25.773', NULL);

-- attrs
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9bb2acb4-90c4-41cd-b857-6774a8a17efc'::uuid, '2d3baddd-42ec-4c6d-a8d1-37425b1a6b47'::uuid, 'attr_text', 0, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('f8d068e7-c3cd-4908-b730-596b39b04e10'::uuid, '2d3baddd-42ec-4c6d-a8d1-37425b1a6b47'::uuid, 'attr_dsl', 1, 4, 'c3ebb1c6-85af-4118-84e5-5f89d9c5c202'::uuid, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('9c06e1c3-4c14-433c-beca-05fae929f706'::uuid, '2d3baddd-42ec-4c6d-a8d1-37425b1a6b47'::uuid, 'attr_list', 2, 3, NULL, NULL);

-- list values
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('6ba60ab1-7c4c-4593-ad59-36791e55837e'::uuid, '9c06e1c3-4c14-433c-beca-05fae929f706'::uuid, 'listValue', NULL);

-- ds
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('527b9023-b6a8-4a64-99c1-af80b94449d2'::uuid, 'ds1_dsl1', '2d3baddd-42ec-4c6d-a8d1-37425b1a6b47'::uuid, 7825, NULL, false);

-- parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('885cadd7-5e0c-4127-8588-cd3a0bc5f531'::uuid, '527b9023-b6a8-4a64-99c1-af80b94449d2'::uuid, '9bb2acb4-90c4-41cd-b857-6774a8a17efc'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('4194f6ec-5c67-4fb3-85b0-af407c18ce66'::uuid, '527b9023-b6a8-4a64-99c1-af80b94449d2'::uuid, 'f8d068e7-c3cd-4908-b730-596b39b04e10'::uuid, NULL, NULL, NULL, 'b2a10672-e694-4eaf-aa64-194880e30257'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('a4339a9b-dd3b-4383-b83a-e711b2ce11cb'::uuid, '527b9023-b6a8-4a64-99c1-af80b94449d2'::uuid, '9c06e1c3-4c14-433c-beca-05fae929f706'::uuid, NULL, NULL, '6ba60ab1-7c4c-4593-ad59-36791e55837e'::uuid, NULL, NULL);


INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl2', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:26.425', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:42.088', NULL);

-- attrs
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 'attr_text', 0, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5bccc7e7-b292-44af-b279-09ec9e7e71b3'::uuid, 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 'attr_dsl', 1, 4, 'c3ebb1c6-85af-4118-84e5-5f89d9c5c202'::uuid, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 'attr_list', 2, 3, NULL, NULL);

-- list values
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('46b14bb1-0638-474d-bac5-186fb3a79e60'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, 'listValue', NULL);
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('46c22d11-5ab4-48f9-82fa-3811938128ce'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, 'listValue2', NULL);

-- ds
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('1919170b-5733-4a9a-b67d-80a6ca027d7d'::uuid, 'identical_ds', 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 7826, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('d450e596-1910-4ad4-8c94-61c576296e4d'::uuid, 'ds_one_param', 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 7827, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('fa11fd87-4461-4908-9069-f0c2f4a2d058'::uuid, 'diff_text_ds', 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 7828, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('1ff48d2c-1819-4682-afc1-a1092a198929'::uuid, 'diff_dsl_ds', 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 7829, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('6ab9a777-3b0d-478c-8340-4b12843342e7'::uuid, 'diff_list_ds', 'fc87c373-51b7-4694-b7bb-820164dcfde2'::uuid, 7830, NULL, false);

-- parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('72ef7ba0-d391-40a4-9ae3-bf73799045a7'::uuid, '1919170b-5733-4a9a-b67d-80a6ca027d7d'::uuid, '24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('57445b5c-3ac7-43ca-96d4-3ed549b6e004'::uuid, '1919170b-5733-4a9a-b67d-80a6ca027d7d'::uuid, '5bccc7e7-b292-44af-b279-09ec9e7e71b3'::uuid, NULL, NULL, NULL, 'b2a10672-e694-4eaf-aa64-194880e30257'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('1c0f442a-46b9-49b9-9a73-e1cf26737fbc'::uuid, '1919170b-5733-4a9a-b67d-80a6ca027d7d'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, NULL, NULL, '46b14bb1-0638-474d-bac5-186fb3a79e60'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ddc6cd0e-8752-4312-ae57-0e3a488ed3dd'::uuid, 'd450e596-1910-4ad4-8c94-61c576296e4d'::uuid, '24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('6a966182-92c2-4b7c-9715-ecd94bb9d5e4'::uuid, 'fa11fd87-4461-4908-9069-f0c2f4a2d058'::uuid, '24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'diff_text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c1b6370d-d1d1-454e-9ba9-64268791d446'::uuid, 'fa11fd87-4461-4908-9069-f0c2f4a2d058'::uuid, '5bccc7e7-b292-44af-b279-09ec9e7e71b3'::uuid, NULL, NULL, NULL, 'b2a10672-e694-4eaf-aa64-194880e30257'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('5d332c83-ac2b-4add-85d5-4309fb9f77ce'::uuid, 'fa11fd87-4461-4908-9069-f0c2f4a2d058'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, NULL, NULL, '46b14bb1-0638-474d-bac5-186fb3a79e60'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('a3aa5cbb-0cc9-48d8-8a4e-ee078c52648f'::uuid, '1ff48d2c-1819-4682-afc1-a1092a198929'::uuid, '24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'diff_text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('39abb691-36b5-4687-8b9c-7ae219de1497'::uuid, '1ff48d2c-1819-4682-afc1-a1092a198929'::uuid, '5bccc7e7-b292-44af-b279-09ec9e7e71b3'::uuid, NULL, NULL, NULL, '067df355-ab02-4af8-bf46-14765c52fcaf'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('49d12ded-4f35-444c-a85e-bc899507947c'::uuid, '1ff48d2c-1819-4682-afc1-a1092a198929'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, NULL, NULL, '46b14bb1-0638-474d-bac5-186fb3a79e60'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('fbdbdd2c-59a8-4783-88c9-f9ab16c6c45b'::uuid, '6ab9a777-3b0d-478c-8340-4b12843342e7'::uuid, '24fbda94-eee2-4afc-a68e-44eab46cb156'::uuid, 'diff_text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('94e04402-f68b-4255-8cb3-642e296528e1'::uuid, '6ab9a777-3b0d-478c-8340-4b12843342e7'::uuid, '5bccc7e7-b292-44af-b279-09ec9e7e71b3'::uuid, NULL, NULL, NULL, 'b2a10672-e694-4eaf-aa64-194880e30257'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('3c426c2d-6d5b-4bbc-b84c-e7b9bfea84eb'::uuid, '6ab9a777-3b0d-478c-8340-4b12843342e7'::uuid, '38c408be-4c1b-41e4-86d1-4319ac85e10d'::uuid, NULL, NULL, '46c22d11-5ab4-48f9-82fa-3811938128ce'::uuid, NULL, NULL);



INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('6ac1915d-23f4-493f-a072-a0e47e84234f'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl3', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:43.223', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:47.686', NULL);

-- attrs
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('532180d9-b392-4815-971d-06eba86f73d6'::uuid, '6ac1915d-23f4-493f-a072-a0e47e84234f'::uuid, 'attr_text', 0, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('4de7ce95-33e0-4edb-b80e-025575f81c0d'::uuid, '6ac1915d-23f4-493f-a072-a0e47e84234f'::uuid, 'attr_dsl', 1, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('3c97dc22-5d86-4841-8af0-50dbab5aaa7d'::uuid, '6ac1915d-23f4-493f-a072-a0e47e84234f'::uuid, 'attr_list', 2, 3, NULL, NULL);

-- ds
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('23be800d-dd13-4ead-ae82-883a4a228a92'::uuid, 'ds1_dsl3', '6ac1915d-23f4-493f-a072-a0e47e84234f'::uuid, 7831, NULL, false);

-- parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('9783cbda-aa86-4e7d-a1b0-4aa1102a2e26'::uuid, '23be800d-dd13-4ead-ae82-883a4a228a92'::uuid, '532180d9-b392-4815-971d-06eba86f73d6'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('b8608f8c-131a-4d57-a04f-00296661c7b1'::uuid, '23be800d-dd13-4ead-ae82-883a4a228a92'::uuid, '4de7ce95-33e0-4edb-b80e-025575f81c0d'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('dcf2b8c4-77c5-484a-953a-a92e7c6d0e53'::uuid, '23be800d-dd13-4ead-ae82-883a4a228a92'::uuid, '3c97dc22-5d86-4841-8af0-50dbab5aaa7d'::uuid, NULL, NULL, '46b14bb1-0638-474d-bac5-186fb3a79e60'::uuid, NULL, NULL);



INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('22a217b7-248f-4955-ad29-3ecb8c17fe52'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl4', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:48.527', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:52.787', NULL);

-- attrs
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('e73f15c8-16eb-41ee-972b-9afacc800d13'::uuid, '22a217b7-248f-4955-ad29-3ecb8c17fe52'::uuid, 'attr1_dsl3', 0, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('b8581e1d-8d55-49bb-ae25-d806db916423'::uuid, '22a217b7-248f-4955-ad29-3ecb8c17fe52'::uuid, 'attr2_notCorrect', 1, 4, 'c3ebb1c6-85af-4118-84e5-5f89d9c5c202'::uuid, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('84208ee9-43a2-48fd-8dfa-38e296194cb1'::uuid, '22a217b7-248f-4955-ad29-3ecb8c17fe52'::uuid, 'attr_list', 2, 3, NULL, NULL);

-- ds
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('e8fe4a2b-815c-46ba-b32f-13a63204eb8a'::uuid, 'ds1_dsl4', '22a217b7-248f-4955-ad29-3ecb8c17fe52'::uuid, 7832, NULL, false);

-- parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('77ffce08-d41a-4817-8646-b94b8eeb5316'::uuid, 'e8fe4a2b-815c-46ba-b32f-13a63204eb8a'::uuid, 'e73f15c8-16eb-41ee-972b-9afacc800d13'::uuid, 'text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('8fe50654-a4e6-47b8-ba54-ab5885d68573'::uuid, 'e8fe4a2b-815c-46ba-b32f-13a63204eb8a'::uuid, 'b8581e1d-8d55-49bb-ae25-d806db916423'::uuid, NULL, NULL, NULL, 'b2a10672-e694-4eaf-aa64-194880e30257'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('45178b68-ef91-4744-83a1-ff368b612c2d'::uuid, 'e8fe4a2b-815c-46ba-b32f-13a63204eb8a'::uuid, '84208ee9-43a2-48fd-8dfa-38e296194cb1'::uuid, NULL, NULL, '46c22d11-5ab4-48f9-82fa-3811938128ce'::uuid, NULL, NULL);

-- compare ds with not used attributes
-- dsl
INSERT INTO public.datasetlist (id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES
    ('109b85de-3bca-4a2a-ab52-27df714b0d68'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl322_322', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:26.425', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:42.088', NULL),
    ('48f389d6-ed89-4042-9848-4e74b90393b5'::uuid, 'a2edd07b-1250-4c79-8392-a35ad62f09b4'::uuid, 'dsl322_323', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:26.425', '1c0167c9-1bea-4587-8f32-d637ff341d31'::uuid, '2023-11-08 17:00:42.088', NULL);

-- attrs
INSERT INTO public."attribute" (id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES
    ('120da011-26b7-4620-92f6-f0653d5bd3ca'::uuid, '109b85de-3bca-4a2a-ab52-27df714b0d68'::uuid, 'attr_equal', 0, 1, NULL, NULL),
    ('f912e2be-5d8c-4ae5-8aa1-e77e13c63457'::uuid, '109b85de-3bca-4a2a-ab52-27df714b0d68'::uuid, 'attr_incompatible', 1, 1, NULL, NULL),
    ('83d780ae-114a-4b54-a4be-6ee78f42946d'::uuid, '48f389d6-ed89-4042-9848-4e74b90393b5'::uuid, 'attr_equal', 0, 1, NULL, NULL),
    ('e17d34c3-fdf6-467e-88f2-244ecf95a133'::uuid, '48f389d6-ed89-4042-9848-4e74b90393b5'::uuid, 'attr_incompatible', 1, 2, NULL, NULL);

-- ds
INSERT INTO public.dataset (id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES
    ('3e2d9226-453a-452d-999e-f060627f8c6c'::uuid, 'ds1_dsl1', '109b85de-3bca-4a2a-ab52-27df714b0d68'::uuid, 7833, NULL, false),
    ('9b3d09e7-2ddf-4381-a711-187174d7ad73'::uuid, 'ds1_dsl1', '48f389d6-ed89-4042-9848-4e74b90393b5'::uuid, 7834, NULL, false);