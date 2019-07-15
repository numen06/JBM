package com.jbm.framework.dao.mybatis.sqlInjector;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

public class SelectByCode extends AbstractMethod {
    //    SELECT_BY_ID("selectById", "根据ID 查询一条数据", "SELECT %s FROM %s WHERE %s=#{%s}"),
    private final String method = "selectByCode";
    private final String sqlScript = "SELECT %s FROM %s WHERE %s=#{%s}";
    private final String column = "code";

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        SqlMethod sqlMethod = SqlMethod.LOGIC_SELECT_BY_ID;
        SqlSource sqlSource = new RawSqlSource(configuration, String.format(sqlMethod.getSql(),
                sqlSelectColumns(tableInfo, false),
                tableInfo.getTableName(), column, column,
                tableInfo.getLogicDeleteSql(true, false)), Object.class);
        return this.addSelectMappedStatementForTable(mapperClass, method, sqlSource, tableInfo);
    }
}
