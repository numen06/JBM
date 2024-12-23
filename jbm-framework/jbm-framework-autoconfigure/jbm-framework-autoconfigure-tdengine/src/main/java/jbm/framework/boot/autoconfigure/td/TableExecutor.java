package jbm.framework.boot.autoconfigure.td;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author wesley
 */
@Slf4j
public class TableExecutor extends AbstractTableExecutor {


    public TableExecutor(DataSource dataSource, String tableName) throws SQLException {
        super(dataSource, tableName);
        this.initTable();
    }


//    public TableExecutor(Connection conn, TableCache tableCache) throws SQLException {
//        super(conn, tableCache.getTableName());
//        this.initTable();
//    }

}