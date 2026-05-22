--liquibase formatted sql
-- V1 已含 resource_type；MySQL 老库补列，H2 仅做数据回填
--changeset jbm:rbac-v4-resource-type dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_authority' AND column_name = 'resource_type'

ALTER TABLE base_authority ADD COLUMN resource_type VARCHAR(20);
UPDATE base_authority SET resource_type = 'menu' WHERE menu_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'action' WHERE action_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'api' WHERE api_id IS NOT NULL AND resource_type IS NULL;

--changeset jbm:rbac-v4-resource-type-h2 dbms:h2 splitStatements:true
UPDATE base_authority SET resource_type = 'menu' WHERE menu_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'action' WHERE action_id IS NOT NULL AND resource_type IS NULL;
UPDATE base_authority SET resource_type = 'api' WHERE api_id IS NOT NULL AND resource_type IS NULL;
