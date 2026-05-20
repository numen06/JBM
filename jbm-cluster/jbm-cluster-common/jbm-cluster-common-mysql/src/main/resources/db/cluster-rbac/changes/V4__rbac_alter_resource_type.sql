--liquibase formatted sql
--changeset jbm:rbac-v4-resource-type splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_authority' AND COLUMN_NAME = 'resource_type'

ALTER TABLE base_authority ADD COLUMN resource_type VARCHAR(20);
UPDATE base_authority SET resource_type = 'menu' WHERE menu_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'action' WHERE action_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'api' WHERE api_id IS NOT NULL AND resource_type IS NULL;
