package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.BeanListHandler;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.math.BigDecimal;
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

    public static List<TableColumn> getTableColumns(DataSource dataSource, String tableName) throws SQLException {
        String sql = StrUtil.format("DESCRIBE {}", tableName);
        return useTDengine(dataSource).query(sql, new BeanListHandler<>(TableColumn.class));
    }

    public static TableCache getTableCache(DataSource dataSource, String tableName, boolean reflush) throws SQLException {
        if (!TABLE_CACHE.containsKey(tableName) || !reflush) {
            List<TableColumn> tableColumns = getTableColumns(dataSource, tableName);
            TableCache tableCache = new TableCache(tableName, tableColumns);
            TABLE_CACHE.put(tableName, tableCache);
        }
        return TABLE_CACHE.get(tableName);
    }

    public static TableCache getTableCache(DataSource dataSource, String tableName) throws SQLException {
        return getTableCache(dataSource, tableName, false);
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

    public static void insert(DataSource dataSource, String tableName, Map<String, Object> data) throws SQLException {
//        String insertSql = StrUtil.format("INSERT INTO {} VALUES ({})", tableName, filedColumns.stream().map(tc -> "?").collect(Collectors.joining(",")));
//        SqlExecutor.execute(dataSource, insertSql, buildFiledValues(data));
        insertBatch(dataSource, tableName, Collections.singletonList(data));
    }

    private static TdSqlDialect tdSqlDialect = new TdSqlDialect();

    public static void insertBatch(DataSource dataSource, String tableName, List<Map<String, Object>> dataList) throws SQLException {
        if (CollUtil.isEmpty(dataList)) {
            return;
        }
        TableCache tableCache = getTableCache(dataSource, tableName);
        List<Entity> entities = dataList.stream().map(data -> {
            tableCache.getTagColumns().forEach(data::remove);
            Entity entity = Entity.create(tableName);
            entity.putAll(data);
            return entity;
        }).collect(Collectors.toList());
        try {
//            for (Entity entity : entities) {
//                SqlBuilder insert = SqlBuilder.create().insert(entity);
//                useTDengine(dataSource).execute(insert.build(), insert.getParamValueArray());
//            }
            Db.use(dataSource, tdSqlDialect).insert(entities);
        } catch (SQLException e) {
            log.error("insertBatch error", e);
            throw e;
        }
    }

    protected static List<Object> buildTagValues(List<String> tagColumns, Map<String, Object> data) throws SQLException {
        //前后包裹#{},最后join,拼接成字符串
        return tagColumns.stream().map(data::get).collect(Collectors.toList());
    }

    protected static List<Object> buildFiledValues(List<String> filedColumns, Map<String, Object> data) {
        return filedColumns.stream().map(data::get).collect(Collectors.toList());
    }

    private static Db useTDengine(DataSource dataSource) {
        return Db.use(dataSource, tdSqlDialect);
    }


    public static TableCache createSubTableIfNotExists(DataSource dataSource, String tableName, String stableName, Map<String, Object> data) throws SQLException {
        TableCache tableCache = null;
        if (TABLE_CACHE.containsKey(tableName)) {
            tableCache = TABLE_CACHE.get(tableName);
        }
        if (ObjectUtil.isNotNull(tableCache)) {
            return addTableColumn(dataSource, tableCache, data);
        }
        final String checkTableSql = "SHOW TABLES LIKE \"?\"";
        useTDengine(dataSource).queryString(checkTableSql, tableName);
        return createSubTable(dataSource, tableName, stableName, data);
    }


    public static TableCache createSubTable(DataSource dataSource, String tableName, String stableName, Map<String, Object> data) throws SQLException {
        TableCache stableCache = getTableCache(dataSource, stableName);
        addTableColumn(dataSource, stableCache, data);
        String createTableSql = StrUtil.format(
                "CREATE TABLE IF NOT EXISTS {} USING {} TAGS ({});",
                tableName, stableName,
                stableCache.getTagColumns().stream().map(tc -> "?").collect(Collectors.joining(","))
        );
        List<Object> values = new ArrayList<>(buildTagValues(stableCache.getTagColumns(), data));
        try {
            useTDengine(dataSource).execute(createTableSql, values.toArray());
        } catch (Exception e) {
            log.error("createSubTable error sql:{}", createTableSql, e);
        }
        return getTableCache(dataSource, tableName);
    }

    public static TableCache addTableColumn(DataSource dataSource, TableCache tableCache, Map<String, Object> data) throws SQLException {
        List<String> newColumn = tableCache.hasNewColumn(data.keySet());
        List<String> alterTable = addTableColumnSql(tableCache.getTableName(), newColumn, data);
        if (CollUtil.isNotEmpty(alterTable)) {
            try {
                useTDengine(dataSource).executeBatch(alterTable);
            } catch (SQLException e) {
                log.error("addTableColumn error sql:{}", alterTable, e);
                throw e;
            }
        }
        return getTableCache(dataSource, tableCache.getTableName(), true);
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
