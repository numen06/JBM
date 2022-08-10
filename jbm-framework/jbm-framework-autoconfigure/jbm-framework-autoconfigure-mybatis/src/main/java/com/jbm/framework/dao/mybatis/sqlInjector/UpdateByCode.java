package com.jbm.framework.dao.mybatis.sqlInjector;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 通过code更新实体
 *
 * @author wesley.zhang
 */
public class UpdateByCode extends AbstractMethod {
    //	UPDATE_BY_ID("updateById", "根据ID 选择修改数据", "<script>\nUPDATE %s %s WHERE %s=#{%s} %s\n</script>"),
    private final String method = "updateByCode";
    private final String sqlScript = "<script>\\nUPDATE %s %s WHERE %s=#{%s} %s\\n</script>";
    private final String column = "code";

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        boolean logicDelete = tableInfo.isLogicDelete();
        SqlMethod sqlMethod = SqlMethod.UPDATE_BY_ID;
        final String additional = optlockVersion(tableInfo) + tableInfo.getLogicDeleteSql(true, false);
        String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(),
                sqlSet(logicDelete, false, tableInfo, false, ENTITY, ENTITY_DOT),
                column, ENTITY_DOT + column, additional);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return addUpdateMappedStatement(mapperClass, modelClass, method, sqlSource);
    }

}
