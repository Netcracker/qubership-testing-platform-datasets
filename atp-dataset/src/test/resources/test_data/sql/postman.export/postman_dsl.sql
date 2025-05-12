INSERT INTO public.visibility_area
(id, "name")
VALUES('46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'::uuid, '[Qubership] ATP2');

-- postman_ref2_dsl
INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, '46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'::uuid, 'postman_ref2_dsl',
       NULL, '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 11:14:30.589',
       '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 11:22:37.555', NULL);

-- postman_ref2_dsl attributes
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('8d6d032f-e270-4231-8fec-7191b6c76f5c'::uuid, '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, 'postman_ref2_list_attr', 2, 3, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('78dd4715-2e88-4c62-acde-777557c5a8c0'::uuid, '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, 'postman_ref2_encrypt_attr', 3, 6, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('42bfb273-712d-41d3-a25e-68a49d51a98f'::uuid, '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, 'postman_ref2_text_attr', 4, 1, NULL, NULL);

-- postman_ref2_dsl datasets
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('e8847d79-fb21-48c6-ab70-6d3ba7815cf7'::uuid, 'postman_ref2_ds', '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, 1, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('923ee6fc-332b-49ac-a780-bb7e063d8684'::uuid, 'postman_ref2_ds2', '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, 2, NULL, false);

-- postman_ref2_dsl list value
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('41146df2-03b3-4b03-b153-aaad80d85ed2'::uuid, '8d6d032f-e270-4231-8fec-7191b6c76f5c'::uuid,
       'ref2_list_value', '1651cf12-76a4-4cc0-b65f-b0d06a57de8f'::uuid);
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('f98066de-29d6-4926-b0e5-4fd198564417'::uuid, '8d6d032f-e270-4231-8fec-7191b6c76f5c'::uuid,
       'ref_list_value', '1651cf12-76a4-4cc0-b65f-b0d06a57de8f'::uuid);

-- postman_ref2_dsl parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('028224c5-5efe-4559-bbd5-e68f431406cd'::uuid, 'e8847d79-fb21-48c6-ab70-6d3ba7815cf7'::uuid, '8d6d032f-e270-4231-8fec-7191b6c76f5c'::uuid, NULL, NULL, '41146df2-03b3-4b03-b153-aaad80d85ed2'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('405fcc49-7513-4439-88ef-37be1633bd34'::uuid, 'e8847d79-fb21-48c6-ab70-6d3ba7815cf7'::uuid, '42bfb273-712d-41d3-a25e-68a49d51a98f'::uuid, 'ref2_ds_macros_random_#RANDOMBETWEEN(0, 20)', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('2123e6e4-56d4-4bfa-80c3-8f551f2e0811'::uuid, 'e8847d79-fb21-48c6-ab70-6d3ba7815cf7'::uuid, '78dd4715-2e88-4c62-acde-777557c5a8c0'::uuid, '{ENC}{bFKD0Ayj5r2xpAXe6E/Ydw==}{1CclqkoS8pvvXBU42lBhuw==}', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ed4f53fa-f449-4784-bc4a-5dde17d07544'::uuid, '923ee6fc-332b-49ac-a780-bb7e063d8684'::uuid, '8d6d032f-e270-4231-8fec-7191b6c76f5c'::uuid, NULL, NULL, 'f98066de-29d6-4926-b0e5-4fd198564417'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('12d250ab-2aa5-4fd6-9584-5c2487d837f6'::uuid, '923ee6fc-332b-49ac-a780-bb7e063d8684'::uuid, '42bfb273-712d-41d3-a25e-68a49d51a98f'::uuid, 'ref2_ds2_text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('2bb3c373-888a-4e67-b4fa-5a8dea457351'::uuid, '923ee6fc-332b-49ac-a780-bb7e063d8684'::uuid, '78dd4715-2e88-4c62-acde-777557c5a8c0'::uuid, '{ENC}{oSJp83LBzxIEnWxmY9nUJw==}{j4fchpD8b/+TpgrBMERTVQ==}', NULL, NULL, NULL, NULL);


-- postman_ref1_dsl
INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, '46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'::uuid, 'postman_ref1_dsl', NULL, '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 11:14:21.130', '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 11:25:19.733', NULL);

-- postman_ref1_dsl attributes
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('ab691ee7-8632-4e80-9f22-9d386a69420f'::uuid, 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 'postman_ref_text_attr', 2, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('c02644ba-f56e-4d07-bbc0-b5cb7467e4e3'::uuid, 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 'postman_ref_text_attr2', 3, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5137c88c-c51a-4a06-910e-c7f185717421'::uuid, 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 'postman_ref_file_attr', 4, 2, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5aba27f9-c6b1-4f5b-9ad9-2d1c2742cb78'::uuid, 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 'postman_ref_link_attr', 5, 4, '4949f48b-8edb-419b-a4d1-5fc8739771b5'::uuid, NULL);

-- postman_ref2_dsl datasets
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('e02bff70-51f6-435c-a16f-e8e986a4bf5b'::uuid, 'postman_ref_ds1', 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 1, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('9ec7b59f-f1cf-4098-9706-1d59913c5a70'::uuid, 'postman_ref_ds2', 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, 2, NULL, false);

-- postman_ref2_dsl parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('523ac73f-f91a-4f68-9781-23b9320f1417'::uuid, 'e02bff70-51f6-435c-a16f-e8e986a4bf5b'::uuid, 'ab691ee7-8632-4e80-9f22-9d386a69420f'::uuid, 'ref_ds_text', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('e67a5252-7ba9-4f69-96ad-ee288a3bee75'::uuid, 'e02bff70-51f6-435c-a16f-e8e986a4bf5b'::uuid, 'c02644ba-f56e-4d07-bbc0-b5cb7467e4e3'::uuid, 'ref_ds_text2_macros_random_#RANDOMBETWEEN(10, 15)', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('451b2b3f-b194-4adb-b192-70ee63451e87'::uuid, 'e02bff70-51f6-435c-a16f-e8e986a4bf5b'::uuid, '5aba27f9-c6b1-4f5b-9ad9-2d1c2742cb78'::uuid, NULL, NULL, NULL, '923ee6fc-332b-49ac-a780-bb7e063d8684'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('ed8a510c-ca53-4934-b965-d9e21bc014d6'::uuid, '9ec7b59f-f1cf-4098-9706-1d59913c5a70'::uuid, 'ab691ee7-8632-4e80-9f22-9d386a69420f'::uuid, 'ref_ds2_macros_random_#RANDOMBETWEEN(1, 10)', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('53d0f4ea-9679-4193-9b64-7a5e3b81d85a'::uuid, '9ec7b59f-f1cf-4098-9706-1d59913c5a70'::uuid, 'c02644ba-f56e-4d07-bbc0-b5cb7467e4e3'::uuid, 'ref_ds2_text2', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('2db34844-e24c-4d52-8053-2360c895c38c'::uuid, '9ec7b59f-f1cf-4098-9706-1d59913c5a70'::uuid, '5aba27f9-c6b1-4f5b-9ad9-2d1c2742cb78'::uuid, NULL, NULL, NULL, 'e8847d79-fb21-48c6-ab70-6d3ba7815cf7'::uuid, NULL);


-- postman_test_dsl
INSERT INTO public.datasetlist
(id, visibility_area_id, "name", test_plan_id, created_by, created_when, modified_by, modified_when, source_id)
VALUES('1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, '46094b3a-dc76-46f6-9a38-e3af2a8cc9fb'::uuid,
       'postman_test_dsl', NULL, '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 11:11:22.344',
       '0ba2b3a5-3546-47cc-a4cf-2fffe6e58348'::uuid, '2024-02-28 13:19:35.235', NULL);

-- postman_test_dsl attributes
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('ab060480-9f4a-463c-8c15-4660e891dcf2'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_text_attr1', 2, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('b1c29d85-93b3-4315-b977-2b4cfbc1f283'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_text_attr2', 3, 1, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('d6d4f629-6b01-49bb-b9af-0a6f1277d9d5'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_ref_attr', 5, 4, 'ea7419ab-3b4b-4f08-9b96-771354b9e46f'::uuid, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('78bb38bd-cd2e-498c-b30d-15adb094928f'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_list_attr1', 6, 3, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('5c605ae4-e1df-4883-a538-d43ae34fc968'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_file_attr', 7, 2, NULL, NULL);
INSERT INTO public."attribute"
(id, datasetlist_id, "name", "ordering", attribute_type_id, type_datasetlist_id, source_id)
VALUES('3cf14a36-7d68-4543-b39c-02027fc9e8e3'::uuid, '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 'postman_encrypt_attr', 8, 6, NULL, NULL);

-- postman_ref2_dsl list value
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('127d1376-5520-4721-8538-d0bdc30bfb7b'::uuid, '78bb38bd-cd2e-498c-b30d-15adb094928f'::uuid, 'list_value1', '1651cf12-76a4-4cc0-b65f-b0d06a57de8f'::uuid);
INSERT INTO public.list_values
(id, attribute_id, "text", source_id)
VALUES('ad659a4f-0e81-4faf-884f-b20c43c6a1ad'::uuid, '78bb38bd-cd2e-498c-b30d-15adb094928f'::uuid, 'list_value2', '1651cf12-76a4-4cc0-b65f-b0d06a57de8f'::uuid);

-- postman_test_dsl datasets
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, 'postman_ds1', '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 1, NULL, false);
INSERT INTO public.dataset
(id, "name", datasetlist_id, "ordering", source_id, "locked")
VALUES('89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'postman_ds2', '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, 2, NULL, false);

-- postman_test_dsl attribute keys
INSERT INTO public.attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('ce6966c2-a938-4d96-a2d4-ff9e03ac004c'::uuid, 'd6d4f629-6b01-49bb-b9af-0a6f1277d9d5_5aba27f9-c6b1-4f5b-9ad9-2d1c2742cb78', '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, '42bfb273-712d-41d3-a25e-68a49d51a98f'::uuid, NULL);
INSERT INTO public.attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('bd5bb991-8fc4-4b43-adf7-9194df40c7dd'::uuid, 'd6d4f629-6b01-49bb-b9af-0a6f1277d9d5', '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'c02644ba-f56e-4d07-bbc0-b5cb7467e4e3'::uuid, NULL);
INSERT INTO public.attribute_key
(id, "key", datasetlist_id, dataset_id, attribute_id, source_id)
VALUES('8e7e0822-c304-4cf1-8497-45efb5c300c6'::uuid, 'd6d4f629-6b01-49bb-b9af-0a6f1277d9d5_5aba27f9-c6b1-4f5b-9ad9-2d1c2742cb78', '1ca198cf-6b55-4a59-899c-bd9808bd5ad6'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, '42bfb273-712d-41d3-a25e-68a49d51a98f'::uuid, NULL);

-- postman_test_dsl parameters
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('07894e1c-bf40-4896-86a0-a9760e7acc73'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, '78bb38bd-cd2e-498c-b30d-15adb094928f'::uuid, NULL, NULL, '127d1376-5520-4721-8538-d0bdc30bfb7b'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('4b47d3a2-de24-484d-b96e-d067234439fa'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, '3cf14a36-7d68-4543-b39c-02027fc9e8e3'::uuid, '{ENC}{oanR+SnpZWUn/iDSoACfVw==}{PqE4Ik45DFEhylibXHfZfg==}', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('0a24931b-444b-4732-96cc-c02547982518'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, 'd6d4f629-6b01-49bb-b9af-0a6f1277d9d5'::uuid, NULL, NULL, NULL, 'e02bff70-51f6-435c-a16f-e8e986a4bf5b'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('24e436d4-15b6-4438-a2ff-ad1ac296459d'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, 'ce6966c2-a938-4d96-a2d4-ff9e03ac004c'::uuid, 'ref2_ds2_text_overlap', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('1c6573ec-bd7c-4a97-96d9-26c50fa023a7'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, 'ab060480-9f4a-463c-8c15-4660e891dcf2'::uuid, 'ref_this_result_ds', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('706f49a7-4855-4b16-89fd-2e7d6c590d62'::uuid, '61f29785-39d5-4e90-81b0-e0b68d772648'::uuid, 'b1c29d85-93b3-4315-b977-2b4cfbc1f283'::uuid, '#REF_THIS(ab060480-9f4a-463c-8c15-4660e891dcf2)', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('289c53e6-27c7-45ff-89fa-022763ce9b78'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, '78bb38bd-cd2e-498c-b30d-15adb094928f'::uuid, NULL, NULL, 'ad659a4f-0e81-4faf-884f-b20c43c6a1ad'::uuid, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('bd04a6bb-6584-4fe2-bbe7-73477fcd8888'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, '3cf14a36-7d68-4543-b39c-02027fc9e8e3'::uuid, '{ENC}{4+/6bF3MAei3cdvKTX4rAg==}{hlt/4XSDojEiBbX8BO6TPA==}', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('a5b4d139-d414-4fe6-b0c2-3eb8e4f80583'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'd6d4f629-6b01-49bb-b9af-0a6f1277d9d5'::uuid, NULL, NULL, NULL, '9ec7b59f-f1cf-4098-9706-1d59913c5a70'::uuid, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('f63b3986-0a3b-4f04-8486-4d834c6e7b3b'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'bd5bb991-8fc4-4b43-adf7-9194df40c7dd'::uuid, 'ref_ds2_text2_overlap', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('d78b2f77-c396-4689-b887-af9b132fff50'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'ab060480-9f4a-463c-8c15-4660e891dcf2'::uuid, 'ref_this_result_ds2', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('223da2d2-5ac7-45ae-93ae-b8387a5fc555'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, 'b1c29d85-93b3-4315-b977-2b4cfbc1f283'::uuid, '#REF_THIS(ab060480-9f4a-463c-8c15-4660e891dcf2)', NULL, NULL, NULL, NULL);
INSERT INTO public."parameter"
(id, dataset_id, attribute_id, string, file, list, ds, source_id)
VALUES('c2ab635d-ece1-4026-8e44-9b1922ac9754'::uuid, '89e33c63-6961-4a2a-af38-0137a6752807'::uuid, '8e7e0822-c304-4cf1-8497-45efb5c300c6'::uuid, 'ref2_ds_macros_random_#RANDOMBETWEEN(11, 20)_macros_overlap', NULL, NULL, NULL, NULL);


