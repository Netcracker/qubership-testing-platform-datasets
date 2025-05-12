--liquibase formatted sql

--changeset dataset:clearduplicateofnameofdsl_upd_3
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:1 SELECT CASE WHEN EXISTS (SELECT 1 FROM (SELECT name, visibility_area_id FROM datasetlist GROUP BY name, visibility_area_id HAVING COUNT(*) > 1) t) THEN 1 ELSE 0 END
UPDATE datasetlist DSL SET name = T.name
  FROM (
    SELECT a.visibility_area_id, a.id, a.name || '_' || a.num as name
    FROM (
        SELECT b.visibility_area_id, b.id, b.name,
               row_number() OVER (PARTITION BY b.visibility_area_id, b.name) AS num
        FROM (
            SELECT visibility_area_id, name, count(id) cnt
            FROM datasetlist
            GROUP BY name, visibility_area_id
        ) a,
        datasetlist b
        WHERE a.cnt > 1
          AND a.name = b.name
          AND a.visibility_area_id = b.visibility_area_id
    ) a
    WHERE a.num > 1
  ) as T
WHERE T.id = DSL.id;

--changeset dataset:dropIfExistsDatasetlistConstraint_upd_3
--preconditions onFail:MARK_RAN onError:WARN
ALTER TABLE DATASETLIST DROP CONSTRAINT IF EXISTS "U_DATASETLIST(NAME,VISIBILITY_AREA_ID)";

--changeset dataset:createDatasetlistConstraint_upd_3
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name = 'datasetlist' AND constraint_type = 'UNIQUE' AND constraint_name = 'U_DATASETLIST(NAME,VISIBILITY_AREA_ID)') THEN 1 ELSE 0 END
ALTER TABLE DATASETLIST
  ADD CONSTRAINT "U_DATASETLIST(NAME,VISIBILITY_AREA_ID)" UNIQUE (name, visibility_area_id);
