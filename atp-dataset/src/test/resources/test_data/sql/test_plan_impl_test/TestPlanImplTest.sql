delete from test_plan where visibility_area_id = 'fcc17c65-6c8c-4ee4-878c-8b7d70664b06';
delete from visibility_area where id = 'fcc17c65-6c8c-4ee4-878c-8b7d70664b06';

INSERT INTO visibility_area
(id, "name")
VALUES('fcc17c65-6c8c-4ee4-878c-8b7d70664b06'::uuid, 'ATPII-5119');

