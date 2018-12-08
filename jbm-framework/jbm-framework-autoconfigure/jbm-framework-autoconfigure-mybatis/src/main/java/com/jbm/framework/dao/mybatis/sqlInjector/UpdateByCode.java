package com.jbm.framework.dao.mybatis.sqlInjector;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.Constants;

/**
 * 
 * 通过code更新实体
 * 
 * @author wesley.zhang
 *
 */
public class UpdateByCode extends AbstractMethod {
//	UPDATE_BY_ID("updateById", "根据ID 选择修改数据", "<script>\nUPDATE %s %s WHERE %s=#{%s} %s\n</script>"),
	private final String method = "updateByCode";
	private final String sqlScript = "<script>\\nUPDATE %s %s WHERE %s=#{%s} %s\\n</script>";

	@Override
	public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
		String sql = String.format(sqlScript, tableInfo.getTableName(),
				sqlSet(false, false, tableInfo, Constants.ENTITY_DOT), "code", Constants.ENTITY_DOT + "code",
				new StringBuilder("<if test=\"et instanceof java.util.Map\">")
						.append("<if test=\"et.MP_OPTLOCK_VERSION_ORIGINAL!=null\">")
						.append(" AND ${et.MP_OPTLOCK_VERSION_COLUMN}=#{et.MP_OPTLOCK_VERSION_ORIGINAL}")
						.append("</if></if>"));
		SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
		return addUpdateMappedStatement(mapperClass, modelClass, method, sqlSource);
	}

}
