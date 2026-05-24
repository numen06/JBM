--liquibase formatted sql
--changeset jbm:org-v15-user-org-code dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'code'
ALTER TABLE base_user_org ADD COLUMN code VARCHAR(64);

--changeset jbm:org-v15-user-org-app-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'app_id'
ALTER TABLE base_user_org ADD COLUMN app_id BIGINT;

--changeset jbm:org-v15-user-org-parent-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'parent_id'
ALTER TABLE base_user_org ADD COLUMN parent_id BIGINT;

--changeset jbm:org-v15-user-org-level dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'level'
ALTER TABLE base_user_org ADD COLUMN level INT;

--changeset jbm:org-v15-user-org-leaf-path dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'leaf_path'
ALTER TABLE base_user_org ADD COLUMN leaf_path VARCHAR(512);

--changeset jbm:org-v15-user-org-extend-data dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_user_org' AND column_name = 'extend_data'
ALTER TABLE base_user_org ADD COLUMN extend_data TEXT;

--changeset jbm:org-v15-user-org-master-cols-h2 dbms:h2 splitStatements:true
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS code VARCHAR(64);
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS app_id BIGINT;
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS level INT;
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS leaf_path VARCHAR(512);
ALTER TABLE base_user_org ADD COLUMN IF NOT EXISTS extend_data TEXT;
