--liquibase formatted sql
--changeset dataset:add_idx_for_source_id

CREATE INDEX IF NOT EXISTS idx_dataset_source_id_and_datasetlist_id ON dataset (source_id, datasetlist_id);

CREATE INDEX IF NOT EXISTS idx_datasetlist_source_id_and_visibility_area_id ON datasetlist (source_id, visibility_area_id);

CREATE INDEX IF NOT EXISTS idx_attribute_source_id_and_datasetlist_id ON attribute (source_id, datasetlist_id);

CREATE INDEX IF NOT EXISTS idx_attribute_key_source_id_and_datasetlist_id ON attribute_key (source_id, datasetlist_id);

CREATE INDEX IF NOT EXISTS idx_parameter_key_source_id_and_dataset_id ON parameter (source_id, dataset_id);

CREATE INDEX IF NOT EXISTS idx_list_values_key_source_id_and_attribute_id ON list_values (source_id, attribute_id);

