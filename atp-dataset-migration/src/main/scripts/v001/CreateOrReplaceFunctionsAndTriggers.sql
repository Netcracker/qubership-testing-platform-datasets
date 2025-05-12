--liquibase formatted sql

-- =============================
-- CREATE OR REPLACE FUNCTIONS
-- =============================

--changeset dataset:create_function_attribute_ref_check splitStatements:false
--preconditions onFail:MARK_RAN onError:WARN
CREATE OR REPLACE FUNCTION attribute_ref_check() RETURNS trigger AS $param_attr_id_check$
BEGIN
    IF
        (SELECT id FROM attribute WHERE id = NEW.attribute_id) IS NULL AND
        (SELECT id FROM attribute_key WHERE id = NEW.attribute_id) IS NULL
    THEN
        RAISE EXCEPTION 'Invalid attribute_id reference: attribute or attribute_key id does not exist';
    END IF;
    RETURN NEW;
END;
$param_attr_id_check$ LANGUAGE plpgsql;

--changeset dataset:create_function_attribute_backref_check splitStatements:false
--preconditions onFail:MARK_RAN onError:WARN
CREATE OR REPLACE FUNCTION attribute_backref_check() RETURNS trigger AS $attr_id_backref_check$
BEGIN
    IF
        (SELECT id FROM parameter WHERE attribute_id = OLD.id) IS NOT NULL
    THEN
        RAISE EXCEPTION 'Can not delete entry: there is parameter referenced to this entry id';
    END IF;
    RETURN OLD;
END;
$attr_id_backref_check$ LANGUAGE plpgsql;

-- =============================
-- DROP TRIGGERS IF EXIST
-- =============================

--changeset dataset:drop_triggers_if_exist
--preconditions onFail:MARK_RAN onError:WARN
DROP TRIGGER IF EXISTS param_attr_id_check ON parameter;
DROP TRIGGER IF EXISTS attr_id_backref_check ON attribute;
DROP TRIGGER IF EXISTS attr_id_backref_check ON attribute_key;

-- =============================
-- CREATE TRIGGERS (safe)
-- =============================

--changeset dataset:create_trigger_param_attr_id_check
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'param_attr_id_check') THEN 1 ELSE 0 END
CREATE TRIGGER param_attr_id_check
BEFORE INSERT OR UPDATE OF attribute_id ON parameter
FOR EACH ROW EXECUTE PROCEDURE attribute_ref_check();

--changeset dataset:create_trigger_attr_backref_check_attribute
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'attr_id_backref_check' AND tgrelid = 'attribute'::regclass) THEN 1 ELSE 0 END
CREATE TRIGGER attr_id_backref_check
BEFORE DELETE ON attribute
FOR EACH ROW EXECUTE PROCEDURE attribute_backref_check();

--changeset dataset:create_trigger_attr_backref_check_attribute_key
--preconditions onFail:MARK_RAN onError:WARN
--precondition-sql-check expectedResult:0 SELECT CASE WHEN EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'attr_id_backref_check' AND tgrelid = 'attribute_key'::regclass) THEN 1 ELSE 0 END
CREATE TRIGGER attr_id_backref_check
BEFORE DELETE ON attribute_key
FOR EACH ROW EXECUTE PROCEDURE attribute_backref_check();
