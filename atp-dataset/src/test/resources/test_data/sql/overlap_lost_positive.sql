INSERT INTO PUBLIC.visibility_area
VALUES ('7559770f-0c6f-4a4c-b2e0-a779637c57b4', 'Test project');

INSERT INTO PUBLIC.datasetlist
VALUES ('f26f9fc4-7cea-44da-b02f-6392520801d9', '8559770f-0c6f-4a4c-b2e0-a779637c57b4', 'dslOverlap1', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31', '2023-03-16 18:40:57.704', '1c0167c9-1bea-4587-8f32-d637ff341d31', '2023-03-16 19:10:53.506', NULL);

INSERT INTO PUBLIC.datasetlist
VALUES ('0f2d1382-6cf4-4d2f-97b9-a4c4ba92d752', '8559770f-0c6f-4a4c-b2e0-a779637c57b4', 'dslOverlap2', NULL, '1c0167c9-1bea-4587-8f32-d637ff341d31', '2023-03-16 18:40:57.704', '1c0167c9-1bea-4587-8f32-d637ff341d31', '2023-03-16 19:10:53.506', NULL);

INSERT INTO PUBLIC.dataset
VALUES ('46cbd694-dbdd-4d1e-a967-e6edee97f318', 'ds11', 'f26f9fc4-7cea-44da-b02f-6392520801d9', 1, NULL, false);

INSERT INTO PUBLIC.attribute
VALUES ('d5d0b2d7-782c-4ae2-a273-c6377f874d82', '0f2d1382-6cf4-4d2f-97b9-a4c4ba92d752', 'attr21', 0, 1, NULL, NULL);

INSERT INTO PUBLIC.attribute_key
VALUES ('f7d1609f-6f08-4742-b6d0-3a0c6ca1af6e', 'b37f7e80-1c04-4663-af6c-8f71f5f2dab9', 'f26f9fc4-7cea-44da-b02f-6392520801d9', '46cbd694-dbdd-4d1e-a967-e6edee97f318', 'd5d0b2d7-782c-4ae2-a273-c6377f874d82', NULL);
