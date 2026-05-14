-- ----------------------------
-- 为 webhook_task 和 webhook_event_config 表创建索引以优化查询性能
-- 注意：如果索引已存在，CREATE INDEX 会报错，请先手动删除已存在的索引
-- 或者使用以下命令删除索引：
-- DROP INDEX idx_webhook_task_event_id ON webhook_task;
-- DROP INDEX idx_webhook_task_http_status ON webhook_task;
-- DROP INDEX idx_webhook_task_create_time ON webhook_task;
-- DROP INDEX idx_webhook_task_event_status_time ON webhook_task;
-- DROP INDEX idx_webhook_event_config_business_code ON webhook_event_config;
-- DROP INDEX idx_webhook_event_config_event_name ON webhook_event_config;
-- DROP INDEX idx_webhook_event_config_event_group ON webhook_event_config;
-- ----------------------------

-- 为 webhook_task 表创建索引

-- 为 event_id 创建索引（JOIN 条件）
CREATE INDEX idx_webhook_task_event_id ON webhook_task(event_id);

-- 为 http_status 创建索引（WHERE 条件）
CREATE INDEX idx_webhook_task_http_status ON webhook_task(http_status);

-- 为 create_time 创建索引（时间范围查询）
CREATE INDEX idx_webhook_task_create_time ON webhook_task(create_time);

-- 创建复合索引以优化常见查询组合（event_id + http_status + create_time）
CREATE INDEX idx_webhook_task_event_status_time ON webhook_task(event_id, http_status, create_time);

-- 为 webhook_event_config 表创建索引

-- 为 business_event_code 创建索引（LIKE 查询优化）
CREATE INDEX idx_webhook_event_config_business_code ON webhook_event_config(business_event_code);

-- 为 event_name 创建索引（LIKE 查询优化）
CREATE INDEX idx_webhook_event_config_event_name ON webhook_event_config(event_name);

-- 为 event_group 创建索引（LIKE 查询优化）
CREATE INDEX idx_webhook_event_config_event_group ON webhook_event_config(event_group);

-- ----------------------------
-- 可选：创建全文索引以优化 LIKE '%xxx%' 查询（MySQL 5.6+ 支持，InnoDB 引擎）
-- 注意：如果表使用 InnoDB 引擎且 MySQL 版本 >= 5.6，可以取消注释以下语句使用全文索引
-- 全文索引可以更好地支持模糊查询，但需要额外的存储空间
-- ----------------------------
-- ALTER TABLE webhook_event_config ADD FULLTEXT INDEX ft_business_event_code(business_event_code);
-- ALTER TABLE webhook_event_config ADD FULLTEXT INDEX ft_event_name(event_name);
-- ALTER TABLE webhook_event_config ADD FULLTEXT INDEX ft_event_group(event_group);
