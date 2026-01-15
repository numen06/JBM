package com.jbm.framework.dao.mybatis.sqlInjector;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.ConverterRegistry;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReadableSqlUtil {

    private final static ConverterRegistry converterRegistry = ConverterRegistry.getInstance();

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
            String formattedValue = formatValueForSql(value);
            // 替换第一个 ? （注意：不能全局替换，避免 SQL 中有 ? 字符）
            sql = sql.replaceFirst("\\?", formattedValue);
        }

        // 转换为单行：将换行符和多个空格压缩为单个空格
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * 格式化参数值用于 SQL 语句
     * 处理日期、字符串、数字等类型的格式化
     * 
     * @param value 参数值
     * @return 格式化后的字符串，用于替换 SQL 中的 ?
     */
    private static String formatValueForSql(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        // 1. 处理日期时间类型（需要加单引号）
        if (value instanceof Date) {
            // java.util.Date, java.sql.Date, java.sql.Timestamp
            return "'" + DateUtil.formatDateTime((Date) value) + "'";
        } else if (value instanceof LocalDateTime) {
            return "'" + ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'";
        } else if (value instanceof LocalDate) {
            return "'" + ((LocalDate) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "'";
        } else if (value instanceof LocalTime) {
            return "'" + ((LocalTime) value).format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "'";
        } else if (value instanceof Instant) {
            return "'" + DateUtil.formatDateTime(Date.from((Instant) value)) + "'";
        } else if (value instanceof ZonedDateTime) {
            return "'" + DateUtil.formatDateTime(Date.from(((ZonedDateTime) value).toInstant())) + "'";
        } else if (value instanceof OffsetDateTime) {
            return "'" + DateUtil.formatDateTime(Date.from(((OffsetDateTime) value).toInstant())) + "'";
        }
        
        // 2. 处理数字类型（不加引号，直接转换为字符串）
        if (value instanceof Number) {
            // 包括：Integer, Long, Double, Float, BigDecimal, BigInteger, Short, Byte 等
            return String.valueOf(value);
        }
        
        // 3. 处理布尔类型（不加引号，转换为数字或字符串，根据数据库习惯使用 true/false）
        if (value instanceof Boolean) {
            // 布尔值转换为字符串，大多数数据库支持 true/false
            return String.valueOf(value);
        }
        
        // 4. 处理字符串类型（必须加单引号，并转义单引号）
        if (value instanceof String) {
            String str = (String) value;
            // 转义单引号：' -> ''
            return "'" + str.replace("'", "''") + "'";
        }
        
        // 5. 处理其他类型（使用转换器转换后判断）
        String converted = converterRegistry.convert(String.class, value);
        if (converted == null) {
            return "NULL";
        }
        
        // 判断转换后的字符串是否为数字格式
        // 匹配：整数、小数（正负号可选）
        if (isNumericString(converted)) {
            // 数字格式，不加引号
            return converted;
        } else {
            // 非数字格式，当作字符串处理，加单引号并转义
            return "'" + converted.replace("'", "''") + "'";
        }
    }
    
    /**
     * 判断字符串是否为数字格式
     * 
     * @param str 待判断的字符串
     * @return true 表示是数字格式，false 表示不是
     */
    private static boolean isNumericString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 匹配整数和小数（包括负数）
        // 支持：123, -123, 123.45, -123.45, .5, -.5 等格式
        return str.matches("^-?\\d+(\\.\\d+)?$|^-?\\d*\\.\\d+$");
    }

    /**
     * 格式化参数为官方格式，用于 Parameters 行输出
     * 格式：123(String), 'text'(String), NULL 等
     */
    public static String formatParameterForOfficial(Object value) {
        if (value == null) {
            return "NULL";
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
        if (rows.isEmpty()) {
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
                    String convertedValue = converterRegistry.convert(String.class, value);
                    // 如果转换结果为 null，使用 "NULL" 字符串
                    rowValues.add(convertedValue != null ? convertedValue : "NULL");
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

}
