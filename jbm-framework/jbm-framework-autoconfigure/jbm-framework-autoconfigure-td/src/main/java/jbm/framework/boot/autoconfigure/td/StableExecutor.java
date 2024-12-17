package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
@Slf4j
public class StableExecutor extends AbstractTableExecutor {


    public StableExecutor(DataSource dataSource, String stable) throws SQLException {
        super(dataSource, stable);
        initTable();
    }

    public <T> void insertSubTable(Function<T, String> function, T bean) throws SQLException {
        String subTableName = function.apply(bean);
        Map<String, Object> data = BeanUtil.beanToMap(bean, true, true);
        TableCache tableCache = TableHelper.createSubTableIfNotExists(dataSource, subTableName, this.getTableName(), data);
        TableHelper.insert(dataSource, tableCache.getTableName(), data);
    }

    public <T> void insertSubTable(Function<T, String> function, Function<T, Map<String, Object>> tagFunction, Function<T, Map<String, Object>> fieldFunction, T bean) throws SQLException {
        String subTableName = function.apply(bean);
        JSONObject data = new JSONObject();
        data.putAll(tagFunction.apply(bean));
        data.putAll(fieldFunction.apply(bean));
        if (!data.containsKey("ts")) {
            data.putOpt("ts", System.currentTimeMillis());
        }
        TableCache tableCache = TableHelper.createSubTableIfNotExists(dataSource, subTableName, this.getTableName(), data);
        TableHelper.insert(dataSource, tableCache.getTableName(), data);
    }


    public void insertSubTable(Function<Map<String, Object>, String> function, Map<String, Object> data) throws SQLException {
        String subTableName = function.apply(data);
        TableCache tableCache = TableHelper.createSubTableIfNotExists(dataSource, subTableName, this.getTableName(), data);
        TableHelper.insert(dataSource, tableCache.getTableName(), data);
    }

    public void insertSubTableBatch(Function<Map<String, Object>, String> function, List<Map<String, Object>> data) throws SQLException {
        Map<String, List<Map<String, Object>>> map = data.stream().collect(Collectors.groupingBy(function));
        for (Map.Entry<String, List<Map<String, Object>>> entry : map.entrySet()) {
            TableCache tableCache = TableHelper.createSubTableIfNotExists(dataSource, entry.getKey(), this.getTableName(), CollUtil.getFirst(entry.getValue()));
            TableHelper.insertBatch(dataSource, tableCache.getTableName(), entry.getValue());
        }
    }

}