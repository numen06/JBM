package jbm.framework.boot.autoconfigure.extendfield;

/**
 * 扩展字段定义数据来源。
 */
public enum FieldDefinitionSource {
    /**
     * 启动时从本地 YAML 同步到 Redis，运行时从 Redis 读取。
     */
    REDIS,
    /**
     * 仅从本地 YAML 读取（不依赖 Redis，适合单机或测试）。
     */
    LOCAL
}
