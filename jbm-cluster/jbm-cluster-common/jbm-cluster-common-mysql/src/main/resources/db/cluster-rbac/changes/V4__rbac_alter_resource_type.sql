--liquibase formatted sql
--changeset jbm:rbac-v4-resource-type splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BASE_AUTHORITY' AND COLUMN_NAME = 'RESOURCE_TYPE'

ALTER TABLE base_authority ADD COLUMN IF NOT EXISTS resource_type VARCHAR(20);
UPDATE base_authority SET resource_type = 'menu' WHERE menu_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'action' WHERE action_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'api' WHERE api_id IS NOT NULL AND resource_type IS NULL;
