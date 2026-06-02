--liquibase formatted sql
-- V17 已执行后追加：按 HEX 修复默认组织乱码、清理空名称 E2E 残留

--changeset jbm:org-v18-repair-default-org-by-hex dbms:mysql splitStatements:true
--validCheckSum 8:cf32f592713cb7b48b132a8bd8e85bcd
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM base_org WHERE id = 1 AND HEX(org_name) <> 'E9BB98E8AEA4E7BB84E7BB87'
UPDATE base_org SET org_name = '默认组织' WHERE id = 1;

--changeset jbm:org-v18-cleanup-empty-org-names dbms:mysql splitStatements:true
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM base_org WHERE id <> 1 AND (org_name IS NULL OR TRIM(org_name) = '')
DELETE FROM base_org WHERE id <> 1 AND (org_name IS NULL OR TRIM(org_name) = '');
