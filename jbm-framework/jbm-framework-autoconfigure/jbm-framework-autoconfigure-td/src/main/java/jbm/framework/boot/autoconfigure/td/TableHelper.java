package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.BeanListHandler;
import cn.hutool.db.sql.SqlBuilder;
import cn.hutool.db.sql.SqlExecutor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
@Slf4j
public class TableHelper {

    private static final Map<String, TableCache> TABLE_CACHE = new ConcurrentHashMap<>();

    public static List<TableColumn> getTableColumns(Connection conn, String tableName) throws SQLException {
        String sql = StrUtil.format("DESCRIBE {}", tableName);
        List<TableColumn> tableColumns = SqlExecutor.query(conn, sql, new BeanListHandler<>(TableColumn.class));
        return tableColumns;
    }


    public static TableCache getTableCache(Connection conn, String tableName, boolean reflush) throws SQLException {
        if (!TABLE_CACHE.containsKey(tableName) || !reflush) {
            List<TableColumn> tableColumns = getTableColumns(conn, tableName);
            TableCache tableCache = new TableCache(tableName, tableColumns);
            TABLE_CACHE.put(tableName, tableCache);
        }
        return TABLE_CACHE.get(tableName);
    }

    public static TableCache getTableCache(Connection conn, String tableName) throws SQLException {
        return getTableCache(conn, tableName, false);
    }

//    public static TableCache getTableCache(String tableName) {
//        return TABLE_CACHE.get(tableName);
//    }

//    public static List<TableColumn> getTableColumns(String tableName) throws SQLException {
//        return TABLE_COLUMNS.get(tableName);
//    }


    public static List<String> getFiledColumns(List<TableColumn> tableColumns) {
        return tableColumns.stream().filter(column -> !"TAG".equals(column.getNote())).map(TableColumn::getField).collect(Collectors.toList());
    }

    public static List<String> getTagColumns(List<TableColumn> tableColumns) {
        return tableColumns.stream().filter(column -> "TAG".equals(column.getNote())).map(TableColumn::getField).collect(Collectors.toList());
    }

    public static void insert(Connection conn, String tableName, Map<String, Object> data) throws SQLException {
//        String insertSql = StrUtil.format("INSERT INTO {} VALUES ({})", tableName, filedColumns.stream().map(tc -> "?").collect(Collectors.joining(",")));
//        SqlExecutor.execute(conn, insertSql, buildFiledValues(data));
        insertBatch(conn, tableName, Collections.singletonList(data));
    }

    public static void insertBatch(Connection conn, String tableName, List<Map<String, Object>> dataList) throws SQLException {
        if (CollUtil.isEmpty(dataList)) {
            return;
        }
        ArrayList<Object> values = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(StrUtil.format("INSERT INTO {} VALUES ", tableName));

        TableCache tableCache = getTableCache(conn, tableName);

        Iterable<Object[]> paramsBatch = new ArrayList<>();
        final String insertParamSql = StrUtil.format("({})", tableCache.getFiledColumns().stream().map(tc -> "?").collect(Collectors.joining(",")));
        dataList.forEach(data -> {
            if (CollUtil.isEmpty(data)) {
                return;
            }
//            sqlBuilder.append(insertParamSql);
            paramsBatch.add(buildFiledValues(tableCache.getFiledColumns(), data).toArray());
            List<Object> v = buildFiledValues(tableCache.getFiledColumns(), data);
            values.addAll(v);
        });
        String sql = sqlBuilder.toString();
        if (StrUtil.isBlank(sql)) {
            return;
        }
        try {
            SqlExecutor.executeBatch(conn, sql, values.toArray());
        } catch (SQLException e) {
            log.error("insertBatch error sql:{}", sql, e);
            throw e;
        }
    }

    private String insertSql(String tableName, Map<String, Object> data) {
        Entity entity = Entity.create(tableName);
        entity.putAll(data);
        return SqlBuilder.create().insert(entity).build();
    }

    protected static List<Object> buildTagValues(List<String> tagColumns, Map<String, Object> data) throws SQLException {
        //前后包裹#{},最后join,拼接成字符串
        return tagColumns.stream().map(data::get).collect(Collectors.toList());
    }

    protected static List<Object> buildFiledValues(List<String> filedColumns, Map<String, Object> data) {
        return filedColumns.stream().map(data::get).collect(Collectors.toList());
    }

    public static TableCache createSubTableIfNotExists(Connection conn, String tableName, String stableName, Map<String, Object> data) throws SQLException {
        TableCache tableCache = null;
        if (TABLE_CACHE.containsKey(tableName)) {
            tableCache = TABLE_CACHE.get(tableName);
        }
        if (ObjectUtil.isNotNull(tableCache)) {
            return addTableColumn(conn, tableCache, data);
        }
        final String checkTableSql = "SHOW TABLES LIKE \"?\"";
        try (PreparedStatement pstmt = conn.prepareStatement(checkTableSql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    tableCache = getTableCache(conn, tableName);
                    return tableCache;
                }
            }
        }
        return createSubTable(conn, tableName, stableName, data);
    }


    public static TableCache createSubTable(Connection conn, String tableName, String stableName, Map<String, Object> data) throws SQLException {
        TableCache stableCache = getTableCache(conn, stableName);
        addTableColumn(conn, stableCache, data);
        String createTableSql = StrUtil.format(
                "CREATE TABLE IF NOT EXISTS {} USING {} TAGS ({});",
                tableName, stableName,
                stableCache.getTagColumns().stream().map(tc -> "?").collect(Collectors.joining(","))
        );
        List<Object> values = new ArrayList<>(buildTagValues(stableCache.getTagColumns(), data));
        try {
            SqlExecutor.execute(conn, createTableSql, values.toArray());
        } catch (Exception e) {
            log.error("createSubTable error sql:{}", createTableSql, e);
        }
        return getTableCache(conn, tableName);
    }

    public static TableCache addTableColumn(Connection conn, TableCache tableCache, Map<String, Object> data) throws SQLException {
        List<String> newColumn = tableCache.hasNewColumn(data.keySet());
        List<String> alterTable = addTableColumnSql(tableCache.getTableName(), newColumn, data);
        if (CollUtil.isNotEmpty(alterTable)) {
            try {
                SqlExecutor.executeBatch(conn, alterTable);
            } catch (SQLException e) {
                log.error("addTableColumn error sql:{}", alterTable, e);
                throw e;
            }

        }
        return getTableCache(conn, tableCache.getTableName(), true);
    }


    public static List<String> addTableColumnSql(String tableName, List<String> filedColumns, Map<String, Object> data) {
        List<String> sqls = new ArrayList<>();
        for (String column : filedColumns) {
            if (!data.containsKey(column)) {
                continue;
            }
            Object value = data.get(column);
            String columnSql = addColumnSql(tableName, column, value);
            if (StrUtil.isNotBlank(columnSql)) {
                sqls.add(columnSql);
            }
        }
        return sqls;
    }

    private static String addColumnSql(String tableName, String column, Object value) {
        String temp = StrUtil.format("alter TABLE {} ADD COLUMN {} {};", tableName);
        if (value instanceof String) {
            return StrUtil.format(temp, column, "VARCHAR(100)");
        }
        if (value instanceof Integer) {
            return StrUtil.format(temp, column, "INT");
        }
        if (value instanceof Long) {
            return StrUtil.format(temp, column, "BIGINT");
        }
        if (value instanceof Double) {
            return StrUtil.format(temp, column, "DOUBLE");
        }
        if (value instanceof Float) {
            return StrUtil.format(temp, column, "FLOAT");
        }
        if (value instanceof Short) {
            return StrUtil.format(temp, column, "FLOAT");
        }
        if (value instanceof Boolean) {
            return StrUtil.format(temp, column, "BOOL");
        }
        if (value instanceof Date) {
            return StrUtil.format(temp, column, "TIMESTAMP");
        }
        if (value instanceof BigDecimal) {
            return StrUtil.format(temp, column, "DOUBLE");
        }
        return null;
    }
}
