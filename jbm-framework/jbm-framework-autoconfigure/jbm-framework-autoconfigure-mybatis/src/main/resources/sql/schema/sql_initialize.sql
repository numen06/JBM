SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sql_initialize
-- SQL自动执行记录表，支持多程序共享数据库场景
-- 使用日期版本号（8位数字：YYYYMMDD），按日期顺序执行，支持不同程序独立维护
-- ----------------------------
DROP TABLE IF EXISTS `sql_initialize`;
CREATE TABLE `sql_initialize`  (
  `file_name` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'SQL文件路径（相对路径）',
  `version` varchar(50) NULL COMMENT 'SQL文件版本号（日期格式：8位数字，如20240101）',
  `module_name` varchar(100) NULL COMMENT '模块/应用名称（标识是哪个应用执行的，用于多程序场景）',
  `file_hash` varchar(64) NULL COMMENT 'SQL文件哈希值（MD5/SHA256，用于检测文件是否被修改）',
  `execute_status` varchar(20) NULL DEFAULT 'SUCCESS' COMMENT '执行状态：SUCCESS-成功, FAILED-失败',
  `error_message` text NULL COMMENT '错误信息（执行失败时记录）',
  `execution_time` bigint NULL COMMENT '执行耗时（毫秒）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`file_name`) USING BTREE,
  INDEX `idx_version` (`version`) USING BTREE COMMENT '版本号索引，用于快速查询最大版本',
  INDEX `idx_module_version` (`module_name`, `version`) USING BTREE COMMENT '模块和版本联合索引，支持按模块查询',
  INDEX `idx_create_time` (`create_time`) USING BTREE COMMENT '执行时间索引，用于按时间查询'
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic COMMENT = 'SQL自动执行记录表（支持多程序共享数据库）';

-- ----------------------------
-- Records of sql_initialize
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
