package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.*;

public class ReadableSqlUtil {
    /**
     * 将 MyBatis 的 BoundSql 转换为可读的完整 SQL（用于日志）
     * 安全处理：字符串加单引号，日期格式化，null 转为 NULL
     * 返回单行 SQL，与官方格式保持一致
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

        // 转换为单行：将换行符和多个空格压缩为单个空格
        return sql.replaceAll("\\s+", " ").trim();
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

    /**
     * 格式化参数为官方格式，用于 Parameters 行输出
     * 格式：123(String), 'text'(String), NULL 等
     */
    public static String formatParameterForOfficial(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'(String)";
        } else if (value instanceof Integer) {
            return value + "(Integer)";
        } else if (value instanceof Long) {
            return value + "(Long)";
        } else if (value instanceof Short) {
            return value + "(Short)";
        } else if (value instanceof Byte) {
            return value + "(Byte)";
        } else if (value instanceof Float) {
            return value + "(Float)";
        } else if (value instanceof Double) {
            return value + "(Double)";
        } else if (value instanceof Boolean) {
            return value + "(Boolean)";
        } else if (value instanceof Date) {
            return "'" + DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss.SSS") + "'(Date)";
        } else if (value instanceof Number) {
            return value + "(Number)";
        } else {
            return "'" + StrUtil.toString(value) + "'(" + value.getClass().getSimpleName() + ")";
        }
    }

    /**
     * 获取 BoundSql 的参数列表，用于官方格式输出
     */
    public static List<Object> getParameterValues(BoundSql boundSql) {
        List<Object> values = new java.util.ArrayList<>();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            return values;
        }

        Object parameterObject = boundSql.getParameterObject();
        MetaObject metaObject = SystemMetaObject.forObject(parameterObject);

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

            values.add(value);
        }
        return values;
    }

    /**
     * 格式化参数列表为官方格式的字符串
     * 格式：123(String), 'text'(String), NULL
     */
    public static String formatParametersForOfficial(BoundSql boundSql) {
        List<Object> values = getParameterValues(boundSql);
        if (values.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatParameterForOfficial(values.get(i)));
        }
        return sb.toString();
    }

    /**
     * 格式化查询结果为官方格式
     * 返回包含 Columns、Rows、Total 信息的格式化字符串列表
     */
    public static List<String> formatResultForOfficial(Object result, boolean showColumns, boolean showRows, boolean showTotal) {
        List<String> lines = new ArrayList<>();

        if (result == null) {
            if (showTotal) {
                lines.add("<==      Total: 0");
            }
            return lines;
        }

        List<Map<String, Object>> rows = convertToListOfMaps(result);
        if (rows == null || rows.isEmpty()) {
            if (showTotal) {
                lines.add("<==      Total: 0");
            }
            return lines;
        }

        // 提取列名（从第一行）
        Map<String, Object> firstRow = rows.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());

        // 输出列信息
        if (showColumns && !columns.isEmpty()) {
            lines.add("<==    Columns: " + String.join(", ", columns));
        }

        // 输出行数据
        if (showRows) {
            for (Map<String, Object> row : rows) {
                List<String> rowValues = new ArrayList<>();
                for (String column : columns) {
                    Object value = row.get(column);
                    rowValues.add(formatResultValue(value));
                }
                lines.add("<==        Row: " + String.join(", ", rowValues));
            }
        }

        // 输出总数
        if (showTotal) {
            lines.add("<==      Total: " + rows.size());
        }

        return lines;
    }

    /**
     * 将查询结果转换为 List<Map<String, Object>> 格式
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> convertToListOfMaps(Object result) {
        if (result == null) {
            return new ArrayList<>();
        }

        if (result instanceof List) {
            List<?> list = (List<?>) result;
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Object item : list) {
                if (item instanceof Map) {
                    resultList.add((Map<String, Object>) item);
                } else {
                    // 将对象转换为 Map
                    Map<String, Object> map = BeanUtil.beanToMap(item, false, true);
                    resultList.add(map);
                }
            }

            return resultList;
        } else if (result instanceof Map) {
            // 单个 Map 结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            resultList.add((Map<String, Object>) result);
            return resultList;
        } else {
            // 单个对象结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            Map<String, Object> map = BeanUtil.beanToMap(result, false, true);
            resultList.add(map);
            return resultList;
        }
    }

    /**
     * 格式化结果值用于输出
     */
    private static String formatResultValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof Date) {
            return DateUtil.formatDateTime((Date) value);
        } else {
            return StrUtil.toString(value);
        }
    }
}
