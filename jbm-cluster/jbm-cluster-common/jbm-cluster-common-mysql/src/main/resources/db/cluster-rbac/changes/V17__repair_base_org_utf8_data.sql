--liquibase formatted sql
-- 幂等修复 base_org UTF-8：表/列字符集、默认组织名称、清理 E2E 测试脏数据

--changeset jbm:org-v17-ensure-utf8mb4-table dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'base_org' AND table_collation <> 'utf8mb4_unicode_ci'
ALTER TABLE base_org CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

--changeset jbm:org-v17-repair-default-org-name dbms:mysql splitStatements:true validCheckSum:8:cf32f592713cb7b48b132a8bd8e85bcd validCheckSum:8:c7ae499160c087b167e3f1b6fe195684
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM base_org WHERE id = 1 AND org_name <> '默认组织'
UPDATE base_org SET org_name = '默认组织' WHERE id = 1;

--changeset jbm:org-v17-cleanup-e2e-test-orgs dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM base_org WHERE id <> 1 AND (org_name LIKE '计划测试组织\_%' ESCAPE '\\' OR org_name LIKE '测试组织UTF8%')
DELETE FROM base_org WHERE id <> 1 AND org_name LIKE '计划测试组织\_%' ESCAPE '\\';
DELETE FROM base_org WHERE id <> 1 AND org_name LIKE '测试组织UTF8%';
