--liquibase formatted sql

--changeset dataset:deduplicate_parameter
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM (SELECT dataset_id, attribute_id FROM parameter GROUP BY dataset_id, attribute_id HAVING COUNT(*) > 1) t) THEN 1 ELSE 0 END
DO
$$
DECLARE
    temp_row RECORD;
BEGIN
    FOR temp_row IN (
        SELECT dataset_id, attribute_id, COUNT(*)
        FROM parameter
        GROUP BY dataset_id, attribute_id
        HAVING COUNT(*) > 1
    )
    LOOP
        DELETE FROM parameter
        WHERE id IN (
            SELECT duplicates.id
            FROM (
                SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS rowNumber
                FROM parameter
                WHERE dataset_id = temp_row.dataset_id
                  AND attribute_id = temp_row.attribute_id
            ) duplicates
            WHERE duplicates.rowNumber > 1
        );
    END LOOP;
END;
$$;

--changeset dataset:add_unique_param_constraint
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name = 'parameter' AND constraint_type = 'UNIQUE' AND constraint_name = 'u_param') THEN 1 ELSE 0 END
ALTER TABLE parameter
  ADD CONSTRAINT u_param UNIQUE (dataset_id, attribute_id);
