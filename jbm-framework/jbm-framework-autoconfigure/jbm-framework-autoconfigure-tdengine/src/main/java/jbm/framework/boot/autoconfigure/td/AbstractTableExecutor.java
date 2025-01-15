package jbm.framework.boot.autoconfigure.td;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wesley
 */
@Slf4j
public abstract class AbstractTableExecutor {

    protected final DataSource dataSource;

    @Getter
    private final String tableName;

    protected List<String> filedColumns = new ArrayList<>();

    protected List<String> tagColumns = new ArrayList<>();

    protected List<TableColumn> tableColumns = new ArrayList<>();

    protected AbstractTableExecutor(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
    }


    public void initTable() throws SQLException {
        this.tableColumns = TableHelper.getTableColumns(dataSource, this.tableName);
        this.tagColumns = TableHelper.getTagColumns(tableColumns);
        this.filedColumns = TableHelper.getFiledColumns(tableColumns);
//        log.info("loadColumns: {}", tagColumns);
    }


//    protected void createSubTableIfNotExists(String subTableName, String stableName, Map<String, Object> data) throws SQLException {
//        TableHelper.createSubTableIfNotExists(conn, subTableName, stableName, data);
//    }

    protected void insertBatch(String tableName, List<Map<String, Object>> value) throws SQLException {
        TableHelper.insertBatch(dataSource, tableName, value);
    }

    protected void insert(Map<String, Object> data) throws SQLException {
        TableHelper.insert(dataSource, this.tableName, data);
    }
}
