-- ----------------------------
-- Flyway迁移测试表
-- 用于验证Flyway功能是否正常工作
-- 版本: V2
-- ----------------------------

-- 创建测试表
CREATE TABLE IF NOT EXISTS `flyway_test_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `test_name` varchar(100) DEFAULT NULL COMMENT '测试名称',
  `test_value` varchar(200) DEFAULT NULL COMMENT '测试值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_test_name` (`test_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flyway测试表';

-- 插入测试数据
INSERT INTO `flyway_test_table` (`test_name`, `test_value`) VALUES
('flyway_test', 'Flyway migration test successful'),
('version_check', 'V2 migration executed');
