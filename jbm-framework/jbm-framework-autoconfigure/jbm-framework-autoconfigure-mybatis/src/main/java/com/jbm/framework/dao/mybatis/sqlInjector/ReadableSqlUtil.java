package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.Date;
import java.util.List;

public class ReadableSqlUtil {
    /**
     * 将 MyBatis 的 BoundSql 转换为可读的完整 SQL（用于日志）
     * 安全处理：字符串加单引号，日期格式化，null 转为 NULL
     */
    public static String getReadableSql(BoundSql boundSql) {
        String sql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Object parameterObject = boundSql.getParameterObject();

        MetaObject metaObject = SystemMetaObject.forObject(parameterObject);

        // 逐个替换 ?
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            Object value;

            if (boundSql.hasAdditionalParameter(property)) {
                value = boundSql.getAdditionalParameter(property);
            } else if (property.contains(".")) {
                // 处理嵌套属性，如 user.name
                value = metaObject.getValue(property);
            } else {
                value = parameterObject;
            }

            // 格式化值
            String formattedValue = formatValue(value);
            // 替换第一个 ? （注意：不能全局替换，避免 SQL 中有 ? 字符）
            sql = sql.replaceFirst("\\?", formattedValue);
        }

        // 在所有参数替换完成后格式化 SQL
        return SqlUtil.formatSql(sql);
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            // 转义单引号
            return "'" + value.toString().replace("'", "''") + "'";
        } else if (value instanceof Date) {
            return "'" + DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss.SSS") + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else {
            return "'" + StrUtil.toString(value) + "'";
        }
    }
}
