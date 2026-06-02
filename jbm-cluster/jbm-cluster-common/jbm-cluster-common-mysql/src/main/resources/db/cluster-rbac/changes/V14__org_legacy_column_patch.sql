--liquibase formatted sql
-- 兼容 V8 等历史库中已存在但列不全的 base_org 表

--changeset jbm:org-v14-col-leaf-path dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'leaf_path'
ALTER TABLE base_org ADD COLUMN leaf_path VARCHAR(512);

--changeset jbm:org-v14-col-app-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'app_id'
ALTER TABLE base_org ADD COLUMN app_id BIGINT;

--changeset jbm:org-v14-col-level dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'level'
ALTER TABLE base_org ADD COLUMN level INT;

--changeset jbm:org-v14-col-group-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'group_id'
ALTER TABLE base_org ADD COLUMN group_id VARCHAR(64);

--changeset jbm:org-v14-col-org-type dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'org_type'
ALTER TABLE base_org ADD COLUMN org_type VARCHAR(32);

--changeset jbm:org-v14-col-manager-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'manager_id'
ALTER TABLE base_org ADD COLUMN manager_id BIGINT;

--changeset jbm:org-v14-col-source-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'source_id'
ALTER TABLE base_org ADD COLUMN source_id VARCHAR(64);

--changeset jbm:org-v14-col-leader-id dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'leader_id'
ALTER TABLE base_org ADD COLUMN leader_id BIGINT;

--changeset jbm:org-v14-col-org-code dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'org_code'
ALTER TABLE base_org ADD COLUMN org_code VARCHAR(64);

--changeset jbm:org-v14-col-number-of-accounts dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'number_of_accounts'
ALTER TABLE base_org ADD COLUMN number_of_accounts INT;

--changeset jbm:org-v14-col-org-address dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'org_address'
ALTER TABLE base_org ADD COLUMN org_address TEXT;

--changeset jbm:org-v14-col-status dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'base_org' AND column_name = 'status'
ALTER TABLE base_org ADD COLUMN status INT DEFAULT 1;

--changeset jbm:org-v14-seed-default-org-retry splitStatements:true
--validCheckSum 8:63be7badd37bb88ca48936fcfc4777e0
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM base_org WHERE id = 1

INSERT INTO base_org (id, org_name, parent_id, group_id, level, leaf_path, status, create_time, update_time)
VALUES (1, '默认组织', NULL, '1', 1, '1', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
