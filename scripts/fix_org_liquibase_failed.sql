-- jaja7 MySQL：修复 V13 种子失败后 Center 无法启动
-- 在目标库执行后重启 Center。

-- 1) 补全历史 base_org 可能缺失的列（已存在则跳过对应语句）
ALTER TABLE base_org ADD COLUMN leaf_path VARCHAR(512);
ALTER TABLE base_org ADD COLUMN level INT;
ALTER TABLE base_org ADD COLUMN group_id VARCHAR(64);
ALTER TABLE base_org ADD COLUMN app_id BIGINT;
ALTER TABLE base_org ADD COLUMN org_type VARCHAR(32);
ALTER TABLE base_org ADD COLUMN manager_id BIGINT;
ALTER TABLE base_org ADD COLUMN source_id VARCHAR(64);
ALTER TABLE base_org ADD COLUMN leader_id BIGINT;
ALTER TABLE base_org ADD COLUMN org_code VARCHAR(64);
ALTER TABLE base_org ADD COLUMN number_of_accounts INT;
ALTER TABLE base_org ADD COLUMN org_address TEXT;
ALTER TABLE base_org ADD COLUMN status INT DEFAULT 1;

-- 2) 清除失败的 Liquibase 记录，便于下次启动重跑种子
DELETE FROM DATABASECHANGELOG
 WHERE ID = 'org-v13-seed-default-org' AND AUTHOR = 'jbm';

-- 3) 若尚无默认组织，可手工插入（可选）
-- INSERT INTO base_org (id, org_name, parent_id, group_id, level, leaf_path, status, create_time, update_time)
-- VALUES (1, '默认组织', NULL, '1', 1, '1', 1, NOW(), NOW())
-- ON DUPLICATE KEY UPDATE org_name = VALUES(org_name);
