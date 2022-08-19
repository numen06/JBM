SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sql_initialize
-- ----------------------------
DROP TABLE IF EXISTS `sql_initialize`;
CREATE TABLE `sql_initialize`  (
  `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`file_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sql_initialize
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
