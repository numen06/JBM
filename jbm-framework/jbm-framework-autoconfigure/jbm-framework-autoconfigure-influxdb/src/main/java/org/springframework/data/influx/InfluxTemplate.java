package org.springframework.data.influx;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Slf4j
public class InfluxTemplate {

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
        InfluxQueryBean influxQueryBean = getInfluxQueryBean(mapper, params);
        log.info("influx sql:{}", influxQueryBean.getMapperConfigBean().getSql());
        QueryResult queryResult = influxDB.query(new Query(influxQueryBean.getMapperConfigBean().getSql(), database));
        return influxQueryBean.getInfluxDataDeserializer().deserializer(queryResult);
    }

    public Map<String, Object> selectOneByDB(String database, String mapper, Object params) {
        List<Map<String, Object>> list = selectListByDB(mapper, database, params);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    public Long selectCount(String mapper, Object params) {
        InfluxQueryBean influxQueryBean = getInfluxQueryBean(mapper, params);
        return influxQueryBean.queryCount();
    }

    public PageResult<Map<String, Object>> selectPage(String mapper, Object params, Page page) {
        Map<String, Object> paramsMap = new HashMap<>();
        if (params instanceof Map) {
            paramsMap = (Map<String, Object>) params;
        } else {
             BeanUtil.beanToMap(params,paramsMap, CopyOptions.create().ignoreNullValue());
        }
        paramsMap.put("page", page);
        InfluxQueryBean influxQueryBean = getInfluxQueryBean(mapper, params);
        String regex = "(?i)SELECT\\s+.*?\\s+FROM";
        // 使用正则表达式替换
        String countSql = ReUtil.replaceAll(influxQueryBean.getSql(), regex, "SELECT count(*) FROM");
        log.info("influx count sql:{}", countSql);
        Long total = influxQueryBean.queryCount(countSql);
          influxQueryBean = getInfluxQueryBean(mapper, paramsMap);
        log.info("influx sql:{}", influxQueryBean.getSql());
        List<Map<String, Object>> list = influxQueryBean.selectList();
        PageResult<Map<String, Object>> pageResult = new PageResult<>(page.getPageNumber(), page.getPageSize(), total.intValue());
        pageResult.addAll(list);
        return pageResult;
    }

    InfluxQueryBean getInfluxQueryBean(String mapper, Object params) {
        InfluxQueryBean influxQueryBean = new InfluxQueryBean(influxDB, database);
        if (params instanceof InfluxQueryParam) {
            InfluxQueryParam param = (InfluxQueryParam) params;
            influxQueryBean.setMapperConfigBean(getMapperBean(mapper, param.getParams()));
            if (param.getDatabase() != null) {
                influxQueryBean.setDatabase(param.getDatabase());
            }
            influxQueryBean.setInfluxDataDeserializer(new InfluxDataDeserializer(influxQueryBean.getMapperConfigBean().getClass(), param.getSupplementColumns()));
        } else {
            influxQueryBean.setMapperConfigBean(getMapperBean(mapper, params));
            influxQueryBean.setInfluxDataDeserializer(new InfluxDataDeserializer(influxQueryBean.getMapperConfigBean().getClass()));
        }
        return influxQueryBean;
    }


    @Data
    @AllArgsConstructor
    static class InfluxQueryBean {
        private org.springframework.data.influx.MapperConfigBean mapperConfigBean = null;
        private InfluxDataDeserializer influxDataDeserializer = null;
        private String database;
        private final InfluxDB influxDB;

        InfluxQueryBean(InfluxDB influxDB, String database) {
            this.influxDB = influxDB;
            this.database = database;
        }

        public String getSql() {
            return mapperConfigBean.getSql();
        }

        public List<Map<String, Object>> selectList() {
            return selectList(this.getSql());
        }

        public List<Map<String, Object>> selectList(String sql) {
            QueryResult queryResult = influxDB.query(new Query(sql, database));
            return influxDataDeserializer.deserializer(queryResult);
        }


        public Map<String, Object> selectOne() {
            List<Map<String, Object>> list = selectList();
            return CollUtil.getFirst(list);
        }

        public Map<String, Object> selectOne(String sql) {
            List<Map<String, Object>> list = selectList();
            return CollUtil.getFirst(list);
        }

        public QueryResult query() {
            return this.query(this.getSql());
        }

        public QueryResult query(String sql) {
            return influxDB.query(new Query(sql, database));
        }

        public Long queryCount() {
            return this.queryCount(this.getSql());
        }

        public Long queryCount(String sql) {
            long total = 0L;
            Map<String, Object> map = this.selectOne(sql);
            for (Object v : map.values()) {
                total = NumberUtil.parseLong(StrUtil.toString(v), 0L);
                break;
            }
            return total;
        }
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
