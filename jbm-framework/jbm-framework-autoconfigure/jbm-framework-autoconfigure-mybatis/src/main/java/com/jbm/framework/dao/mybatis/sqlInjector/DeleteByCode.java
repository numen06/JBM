package com.jbm.framework.dao.mybatis.sqlInjector;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

public class DeleteByCode extends AbstractMethod {
//	 DELETE_BY_ID("deleteById", "根据ID 删除一条数据", "<script>\nDELETE FROM %s WHERE %s=#{%s}\n</script>"),
	private final String method = "deleteByCode";
	private final String sqlScript = "<script>\\nDELETE FROM %s WHERE %s=#{%s}\\n</script>";
	@Override
	public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
//		SqlMethod sqlMethod = SqlMethod.DELETE_BY_ID;
		SqlSource sqlSource = languageDriver.createSqlSource(configuration,
				String.format(sqlScript, tableInfo.getTableName(), "code", "code"), modelClass);
		return this.addDeleteMappedStatement(mapperClass, MasterDataSqlInjector.DELETE_BY_CODE, sqlSource);
	}
}
