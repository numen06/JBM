package org.springframework.data.influx;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wesley.zhang
 *
 */
public class InfluxTemplate {

	private static Logger logger = LoggerFactory.getLogger(InfluxTemplate.class);

	private SqlSession sqlSession;

	private InfluxDB influxDB;

	private String database;

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public InfluxTemplate(InfluxDB influxDB) {
		super();
		this.influxDB = influxDB;
	}

	public InfluxTemplate(InfluxDB influxDB, String database) {
		super();
		this.influxDB = influxDB;
		this.database = database;
	}

	public InfluxTemplate(InfluxDB influxDB, SqlSession sqlSession) {
		super();
		this.sqlSession = sqlSession;
		this.influxDB = influxDB;
	}

	public InfluxDB getInfluxDB() {
		return influxDB;
	}

	public List<Map<String, Object>> selectList(String mapper, Object params) {
		return selectListByDB(this.database, mapper, params);
	}

	public Map<String, Object> selectOne(String mapper, Object params) {
		return selectOneByDB(this.database, mapper, params);
	}

	public List<Map<String, Object>> selectListByDB(String database, String mapper, Object params) {
		MapperConfigBean mapperConfigBean = null;
		InfluxDataDeserializer influxDataDeserializer = null;
		if (params instanceof InfluxQueryParam) {
			InfluxQueryParam param = (InfluxQueryParam) params;
			mapperConfigBean = getMapperBean(mapper, param.getParams());
			if (param.getDatabase() != null)
				database = param.getDatabase();
			influxDataDeserializer = new InfluxDataDeserializer(mapperConfigBean.getClass(), param.getSupplementColumns());
		} else {
			mapperConfigBean = getMapperBean(mapper, params);
			influxDataDeserializer = new InfluxDataDeserializer(mapperConfigBean.getClass());
		}
		String sql = mapperConfigBean.getSql();
		logger.info("influx sql:" + "{}", sql);
		QueryResult queryResult = influxDB.query(new Query(sql, database));
		return influxDataDeserializer.deserializer(queryResult);
	}

	public Map<String, Object> selectOneByDB(String database, String mapper, Object params) {
		List<Map<String, Object>> list = selectListByDB(mapper, database, params);
		if (com.td.util.CollectionUtils.isEmpty(list))
			return null;
		return list.get(0);
	}

	public MapperConfigBean getMapperBean(String namespace) {
		return getMapperBean(namespace, null);
	}

	public MapperConfigBean getMapperBean(String namespace, Object params) {
		MapperConfigBean bean = new MapperConfigBean();
		Configuration configuration = this.sqlSession.getConfiguration();
		MappedStatement mappedStatement = configuration.getMappedStatement(namespace);
		try {
			bean.setResultType(mappedStatement.getParameterMap().getType());
		} catch (Exception e) {
			bean.setResultType(Map.class);
		}
		TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
		BoundSql boundSql = mappedStatement.getBoundSql(params);
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		String sql = boundSql.getSql();
		if (parameterMappings != null) {
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					Object value;
					String propertyName = parameterMapping.getProperty();
					if (boundSql.hasAdditionalParameter(propertyName)) {
						value = boundSql.getAdditionalParameter(propertyName);
					} else if (params == null) {
						value = null;
					} else if (typeHandlerRegistry.hasTypeHandler(params.getClass())) {
						value = params;
					} else {
						MetaObject metaObject = configuration.newMetaObject(params);
						value = metaObject.getValue(propertyName);
					}
					JdbcType jdbcType = parameterMapping.getJdbcType();
					if (value == null && jdbcType == null)
						jdbcType = configuration.getJdbcTypeForNull();
					sql = replaceParameter(sql, value, jdbcType, parameterMapping.getJavaType());
				}
			}
		}
		bean.setSql(sql);
		return bean;
	}

	private static String replaceParameter(String sql, Object value, JdbcType jdbcType, Class<?> javaType) {
		String strValue = String.valueOf(value);
		if (jdbcType != null) {
			switch (jdbcType) {
			// 数字
			case BIT:
			case TINYINT:
			case SMALLINT:
			case INTEGER:
			case BIGINT:
			case FLOAT:
			case REAL:
			case DOUBLE:
			case NUMERIC:
			case DECIMAL:
				break;
			// 日期
			case DATE:
			case TIME:
			case TIMESTAMP:
				// 其他，包含字符串和其他特殊类型
			default:
				strValue = "'" + strValue + "'";

			}
		} else if (Number.class.isAssignableFrom(javaType)) {
			// 不加单引号
		} else {
			strValue = "'" + strValue + "'";
		}
		return sql.replaceFirst("\\?", strValue);
	}

}
