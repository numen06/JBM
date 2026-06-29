-- 密码最后修改时间
ALTER TABLE base_account ADD COLUMN password_update_time DATETIME NULL COMMENT '密码最后修改时间';
UPDATE base_account SET password_update_time = update_time WHERE password_update_time IS NULL;

-- 账号唯一性约束
CREATE UNIQUE INDEX uk_base_account_identity ON base_account (account, account_type, domain);
