-- 补全密码最后修改时间：优先 update_time，其次 create_time，最后用当前时间
UPDATE base_account
SET password_update_time = COALESCE(update_time, create_time, NOW())
WHERE password_update_time IS NULL
  AND password IS NOT NULL
  AND password != '';
