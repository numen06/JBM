package com.jbm.framework.dao;

import com.jbm.framework.dao.mybatis.sqlAudit.SqlAuditPushType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "sql-log")
public class SqlLogProperties {
    /**
     * SQL日志白名单，匹配的mapper方法会输出日志
     */
    private List<String> whitelist;

    /**
     * 日志格式类型：MERGED（合并格式，SQL和参数合并）或 OFFICIAL（官方格式，分别显示Preparing和Parameters）
     * 默认 MERGED，保持向后兼容
     */
    private SqlLogFormat format = SqlLogFormat.MERGED;
    
    /**
     * 自定义日志格式化字符串（类似 p6spy）
     * 支持占位符：%(currentTime)、%(dataSource)、%(executionTime)、%(sql)、%(mapperId)、%(operationType)、%(applicationName)、%(instanceId)、%(slowQuery)、%(result)
     * 默认格式：%(currentTime) | DS: %(dataSource) | took %(executionTime)ms | %(sql)
     * 如果设置了此配置，将使用此格式替代 format 配置
     * 如果为空，则使用 format 配置的格式（MERGED 或 OFFICIAL）
     */
    private String customFormat = "%(currentTime) | DS: %(dataSource) | took %(executionTime)ms | %(sql)";

    /**
     * 是否显示列信息（仅official格式时有效）
     * 默认 false
     */
    private Boolean showColumns = false;

    /**
     * 是否显示行数据（仅official格式时有效）
     * 默认 false
     */
    private Boolean showRows = false;

    /**
     * 是否显示总数（仅official格式时有效）
     * 默认 false
     */
    private Boolean showTotal = false;
    
    /**
     * 是否启用 SQL 过滤（过滤掉无用语句，如 SET NAMES、SELECT 1 等）
     * 默认 true（启用）
     */
    private Boolean filter = true;
    
    /**
     * SQL 排除规则（正则表达式，多个规则用 | 分隔）
     * 匹配的 SQL 语句将被过滤掉，不记录日志
     * 默认：SELECT 1;|SHOW VARIABLES;|SET .*
     */
    private String exclude = "SELECT 1;|SHOW VARIABLES;|SET .*";
    
    /**
     * 慢查询配置
     */
    private SlowQueryProperties slowQuery = new SlowQueryProperties();
    
    /**
     * SQL 审计配置
     */
    private SqlAuditProperties audit = new SqlAuditProperties();
    
    /**
     * SQL 审计配置类
     */
    @Data
    public static class SqlAuditProperties {
        /**
         * 是否启用审计
         * 默认 true（启用）
         */
        private Boolean enabled = true;
        
        /**
         * 审计推送方式
         * 默认 LOCAL_LOG（本地打印）
         */
        private SqlAuditPushType pushType = SqlAuditPushType.LOCAL_LOG;
        
        /**
         * 是否启用本地打印（默认启用，即使使用其他推送方式也会保留本地打印）
         * 默认 true
         */
        private Boolean enableLocalLog = true;
        
        /**
         * 数据库推送配置
         */
        private DatabasePushProperties database = new DatabasePushProperties();
        
        /**
         * 消息队列推送配置
         */
        private MessageQueuePushProperties messageQueue = new MessageQueuePushProperties();
        
        /**
         * HTTP 推送配置
         */
        private HttpPushProperties http = new HttpPushProperties();
        
        @Data
        public static class DatabasePushProperties {
            /**
             * 是否启用数据库推送
             * 默认 false
             */
            private Boolean enabled = false;
            
            /**
             * 表名（如果为空则使用默认表名）
             */
            private String tableName;
        }
        
        @Data
        public static class MessageQueuePushProperties {
            /**
             * 是否启用消息队列推送
             * 默认 false
             */
            private Boolean enabled = false;
            
            /**
             * Topic（Kafka）或 Exchange（RabbitMQ）
             */
            private String topic;
            
            /**
             * Exchange（RabbitMQ 专用）
             */
            private String exchange;
            
            /**
             * Routing Key（RabbitMQ 专用）
             */
            private String routingKey;
        }
        
        @Data
        public static class HttpPushProperties {
            /**
             * 是否启用 HTTP 推送
             * 默认 false
             */
            private Boolean enabled = false;
            
            /**
             * 推送 URL
             */
            private String url;
            
            /**
             * 请求超时时间（毫秒）
             * 默认 5000
             */
            private Integer timeout = 5000;
            
            /**
             * 是否异步推送
             * 默认 true
             */
            private Boolean async = true;
        }
    }
    
    /**
     * 慢查询配置类
     */
    @Data
    public static class SlowQueryProperties {
        /**
         * 是否启用慢查询检测
         * 默认 true（启用）
         */
        private Boolean enabled = true;
        
        /**
         * 慢查询阈值（毫秒）
         * 默认 3000（3秒）
         */
        private Long threshold = 3000L;
        
        /**
         * 是否打印慢查询日志
         * 默认 true（打印）
         */
        private Boolean logSlowQuery = true;
    }
}