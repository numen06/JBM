package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
@Data
public class TableCache {

    private final String tableName;
    private List<String> filedColumns = new ArrayList<>();
    private List<String> tagColumns = new ArrayList<>();
    private List<TableColumn> tableColumns = new ArrayList<>();

    public TableCache(String tableName) {
        this.tableName = tableName;
    }

    public TableCache(String tableName, List<TableColumn> tableColumns) {
        this.tableName = tableName;
        this.tableColumns = tableColumns;
        this.filedColumns = TableHelper.getFiledColumns(tableColumns);
        this.tagColumns = TableHelper.getTagColumns(tableColumns);
    }

    public List<String> hasNewColumn(Collection<String> columnNames) {
        return columnNames.stream().map(StrUtil::toUnderlineCase)
                .filter(columnName -> !filedColumns.contains(columnName))
                .filter(columnName -> !tagColumns.contains(columnName))
                .collect(Collectors.toList());
    }



}
