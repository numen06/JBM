--liquibase formatted sql

--changeset jbm:openapi-docs-v22-document-master-cols-mysql dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'open_api_document' AND column_name = 'id'
ALTER TABLE open_api_document ADD COLUMN id BIGINT;
ALTER TABLE open_api_document ADD COLUMN code VARCHAR(64);
ALTER TABLE open_api_document ADD COLUMN app_id BIGINT;
ALTER TABLE open_api_document ADD COLUMN parent_id BIGINT;
ALTER TABLE open_api_document ADD COLUMN level INT;
ALTER TABLE open_api_document ADD COLUMN leaf_path VARCHAR(512);
ALTER TABLE open_api_document ADD COLUMN extend_data TEXT;

--changeset jbm:openapi-docs-v22-operation-master-cols-mysql dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'open_api_operation' AND column_name = 'id'
ALTER TABLE open_api_operation ADD COLUMN id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN code VARCHAR(64);
ALTER TABLE open_api_operation ADD COLUMN app_id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN parent_id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN level INT;
ALTER TABLE open_api_operation ADD COLUMN leaf_path VARCHAR(512);
ALTER TABLE open_api_operation ADD COLUMN extend_data TEXT;

--changeset jbm:openapi-docs-v22-published-master-cols-mysql dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'published_api_doc' AND column_name = 'id'
ALTER TABLE published_api_doc ADD COLUMN id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN code VARCHAR(64);
ALTER TABLE published_api_doc ADD COLUMN app_id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN parent_id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN level INT;
ALTER TABLE published_api_doc ADD COLUMN leaf_path VARCHAR(512);
ALTER TABLE published_api_doc ADD COLUMN extend_data TEXT;

--changeset jbm:openapi-docs-v22-master-cols-h2 dbms:h2 splitStatements:true
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS id BIGINT;
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS app_id BIGINT;
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS level INT;
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS leaf_path VARCHAR(512);
ALTER TABLE open_api_document ADD COLUMN IF NOT EXISTS extend_data TEXT;
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS app_id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS level INT;
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS leaf_path VARCHAR(512);
ALTER TABLE open_api_operation ADD COLUMN IF NOT EXISTS extend_data TEXT;
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS app_id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS level INT;
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS leaf_path VARCHAR(512);
ALTER TABLE published_api_doc ADD COLUMN IF NOT EXISTS extend_data TEXT;
