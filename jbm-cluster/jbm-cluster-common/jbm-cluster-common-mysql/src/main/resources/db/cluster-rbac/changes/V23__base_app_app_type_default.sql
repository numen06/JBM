--liquibase formatted sql
-- 兼容历史库 app_type 为 NOT NULL 且无默认值时，新增应用未传类型导致插入失败。

--changeset jbm:base-app-v23-app-type-default-mysql dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_app' AND column_name = 'app_type'
UPDATE base_app SET app_type = 'pc' WHERE app_type IS NULL OR app_type = '';
ALTER TABLE base_app ALTER COLUMN app_type SET DEFAULT 'pc';

--changeset jbm:base-app-v23-app-type-default-h2 dbms:h2 splitStatements:true
UPDATE base_app SET app_type = 'pc' WHERE app_type IS NULL OR app_type = '';
ALTER TABLE base_app ALTER COLUMN app_type SET DEFAULT 'pc';
