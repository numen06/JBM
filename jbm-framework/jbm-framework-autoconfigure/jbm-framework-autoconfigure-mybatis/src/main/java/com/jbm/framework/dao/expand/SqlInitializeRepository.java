package com.jbm.framework.dao.expand;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Session;
import cn.hutool.db.sql.SqlExecutor;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL执行记录数据库操作类（使用ORM方式）
 * 负责 sql_initialize 表的CRUD操作
 * 
 * @author wesley
 */
@Slf4j
public class SqlInitializeRepository {

    private final static String TABLE_NAME = "sql_initialize";
    private final static String SELECT_INIT_TABLE = "SELECT COUNT(*) FROM information_schema.TABLES WHERE table_name ='sql_initialize' AND table_schema = DATABASE();";
    private final static String DROP_INIT_TABLE = "DROP TABLE IF EXISTS `sql_initialize`";
    
    // 必需的字段列表
    private final static String[] REQUIRED_COLUMNS = {
        "file_name", "version", "module_name", "file_hash", 
        "execute_status", "error_message", "execution_time", "create_time"
    };

    private final Session session;

    public SqlInitializeRepository(Session session) {
        this.session = session;
    }

    /**
     * 检查表是否存在
     * 
     * @return true表示表存在，false表示不存在
     */
    public boolean tableExists() {
        try {
            ResultSet rs = SqlExecutor.callQuery(session.getConnection(), SELECT_INIT_TABLE);
            int count = 0;
            while (rs.next()) {
                count = rs.getInt(1);
            }
            return count > 0;
        } catch (Exception e) {
            log.error("检查表是否存在失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查表结构是否完整（所有必需字段是否存在）
     * 
     * @return true表示表结构完整，false表示缺少字段
     */
    public boolean isTableStructureValid() {
        try {
            for (String columnName : REQUIRED_COLUMNS) {
                String checkColumnSql = buildCheckColumnSql(columnName);
                ResultSet rs = SqlExecutor.callQuery(session.getConnection(), checkColumnSql);
                int columnCount = 0;
                while (rs.next()) {
                    columnCount = rs.getInt(1);
                }
                if (columnCount < 1) {
                    log.warn("sql_initialize 表缺少必需字段: {}", columnName);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查表结构失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除表
     */
    public void dropTable() {
        try {
            session.execute(DROP_INIT_TABLE);
        } catch (SQLException e) {
            log.error("删除表失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前数据库最大版本号
     * 优先日期格式（8位数字），其次语义化版本
     * 
     * @return 最大版本号，如果没有则返回"0"
     */
    public String getMaxVersion() {
        try {
            // 使用Entity方式查询所有记录（在内存中过滤）
            Entity queryEntity = Entity.create(TABLE_NAME);
            List<Entity> list = session.find(queryEntity);
            
            String maxDate = null;
            String maxVersion = null;
            
            // 在内存中处理，找到最大版本号
            for (Entity entity : list) {
                // 只处理版本号不为空且执行成功的记录
                String version = entity.getStr("version");
                String executeStatus = entity.getStr("execute_status");
                
                if (StrUtil.isNotBlank(version) && 
                    (StrUtil.isBlank(executeStatus) || "SUCCESS".equals(executeStatus))) {
                    
                    // 如果是8位数字（日期格式），更新maxDate
                    if (version.matches("^\\d{8}$")) {
                        if (maxDate == null || SqlVersionParser.compareVersion(version, maxDate) > 0) {
                            maxDate = version;
                        }
                    } else {
                        // 语义化版本
                        if (maxVersion == null || SqlVersionParser.compareVersion(version, maxVersion) > 0) {
                            maxVersion = version;
                        }
                    }
                }
            }
            
            // 优先使用日期格式的版本号
            if (StrUtil.isNotBlank(maxDate)) {
                log.info("当前数据库版本（日期）: {}", maxDate);
                return maxDate;
            } else if (StrUtil.isNotBlank(maxVersion)) {
                log.info("当前数据库版本（语义化）: {}", maxVersion);
                return maxVersion;
            } else {
                log.info("未找到已执行的SQL文件，使用默认版本: 0");
                return "0";
            }
        } catch (Exception e) {
            log.warn("获取数据库版本失败，使用默认版本0", e);
            return "0";
        }
    }

    /**
     * 加载所有已执行的SQL文件记录
     * 
     * @return 文件名到SqlInitialize的映射
     */
    public Map<String, SqlInitialize> loadAllRecords() {
        Map<String, SqlInitialize> initializeList = new ConcurrentHashMap<>();
        try {
            // 使用Entity方式查询所有记录
            Entity queryEntity = Entity.create(TABLE_NAME);
            List<Entity> list = session.find(queryEntity);
            
            list.forEach(entity -> {
                SqlInitialize sqlInitialize = new SqlInitialize();
                // 使用toBeanWithCamelCase自动转换下划线字段名为驼峰属性
                entity.toBeanWithCamelCase(sqlInitialize);
                // 确保日期字段正确转换
                if (entity.get("create_time") != null) {
                    sqlInitialize.setCreateTime(entity.getDate("create_time"));
                }

                initializeList.put(sqlInitialize.getFileName(), sqlInitialize);
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return initializeList;
    }

    /**
     * 插入执行成功记录
     * 
     * @param sqlFileName 文件名
     * @param version 版本号
     * @param moduleName 模块名
     * @param fileHash 文件哈希值
     * @param executionTime 执行耗时（毫秒）
     */
    public void insertSuccessRecord(String sqlFileName, String version, String moduleName, 
                                     String fileHash, long executionTime) {
        try {
            // 使用Entity方式插入记录
            Entity entity = Entity.create(TABLE_NAME)
                    .set("file_name", sqlFileName)
                    .set("version", version)
                    .set("module_name", moduleName)
                    .set("file_hash", fileHash)
                    .set("execute_status", "SUCCESS")
                    .set("error_message", null)
                    .set("execution_time", executionTime)
                    .set("create_time", DateUtil.now());
            
            int result = session.insert(entity);
                
            if (result < 1) {
                throw new RuntimeException("插入记录失败");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 插入执行失败记录
     * 
     * @param sqlFileName 文件名
     * @param version 版本号
     * @param moduleName 模块名
     * @param fileHash 文件哈希值
     * @param executionTime 执行耗时（毫秒）
     * @param errorMessage 错误信息
     */
    public void insertFailedRecord(String sqlFileName, String version, String moduleName, 
                                    String fileHash, long executionTime, String errorMessage) {
        try {
            // 使用Entity方式插入记录
            Entity entity = Entity.create(TABLE_NAME)
                    .set("file_name", sqlFileName)
                    .set("version", version)
                    .set("module_name", moduleName)
                    .set("file_hash", fileHash)
                    .set("execute_status", "FAILED")
                    .set("error_message", errorMessage)
                    .set("execution_time", executionTime)
                    .set("create_time", DateUtil.now());
            
            int result = session.insert(entity);
                
            if (result < 1) {
                throw new RuntimeException("插入记录失败");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查列是否存在
     * @param columnName 列名（必须是常量，安全）
     */
    private static String buildCheckColumnSql(String columnName) {
        return "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_name ='sql_initialize' AND column_name ='" + columnName + "' AND table_schema = DATABASE();";
    }
}
