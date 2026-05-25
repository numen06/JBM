--liquibase formatted sql
-- 确保 base_org 使用 utf8mb4，避免中文 org_name 在 latin1 环境下乱码

--changeset jbm:org-v16-utf8mb4 dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'base_org'
ALTER TABLE base_org CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

--changeset jbm:org-v16-repair-default-org-name dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM base_org WHERE id = 1
UPDATE base_org SET org_name = '默认组织' WHERE id = 1 AND org_name <> '默认组织';
