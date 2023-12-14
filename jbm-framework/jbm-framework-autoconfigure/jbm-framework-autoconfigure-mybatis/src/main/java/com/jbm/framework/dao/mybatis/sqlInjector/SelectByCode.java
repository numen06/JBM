package com.jbm.framework.dao.mybatis.sqlInjector;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;

public class SelectByCode extends AbstractMethod {
    //    SELECT_BY_ID("selectById", "根据ID 查询一条数据", "SELECT %s FROM %s WHERE %s=#{%s}"),
    private final String sqlScript = "SELECT %s FROM %s WHERE %s=#{%s}";
    private final String column = "code";

    /**
     * @param methodName 方法名
     * @since 3.5.0
     */
    protected SelectByCode(String methodName) {
        super(methodName);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        SqlMethod sqlMethod = SqlMethod.LOGIC_DELETE_BY_ID;
        SqlSource sqlSource = new RawSqlSource(configuration, String.format(sqlMethod.getSql(),
                sqlSelectColumns(tableInfo, false),
                tableInfo.getTableName(), column, column,
                tableInfo.getLogicDeleteSql(true, false)), Object.class);
        return this.addSelectMappedStatementForTable(mapperClass, MasterDataSqlInjector.SELECT_BY_CODE, sqlSource, tableInfo);
    }
}
