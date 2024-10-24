package org.springframework.data.influx;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.template.simple.SimpleTemplateEngine;
import jbm.framework.boot.autoconfigure.influx.InfluxProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 简单的InfluxDB模板类
 */
@Slf4j
public class SimpleInfluxTemplate {

    private InfluxDB influxDB;
    @Getter
    private String database;

    @Getter
    private String retentionPolicy = "autogen";

    private TemplateEngine templateEngine;

    private final InfluxProperties influxProperties;

    public SimpleInfluxTemplate(InfluxProperties influxProperties) {
        this(influxProperties, "influx/sqls");
    }

//    public SimpleInfluxTemplate(InfluxDB influxDB) {
//        this(influxDB, "data", "influx/sqls");
//    }

    /**
     * 使用给定的InfluxProperties和路径创建SimpleInfluxTemplate的构造函数。
     *
     * @param influxProperties InfluxProperties对象，用于配置InfluxDB连接属性。
     * @param path             连接InfluxDB数据库的路径。
     */
    public SimpleInfluxTemplate(InfluxProperties influxProperties, String path) {
        this.influxProperties = influxProperties;
        this.influxDbBuild(influxProperties);
        this.database = StrUtil.isEmpty(influxProperties.getDatabase()) ? "data" : influxProperties.getDatabase();
        TemplateConfig templateConfig = new TemplateConfig(path, TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig.setCustomEngine(SimpleTemplateEngine.class);
        templateEngine = TemplateUtil.createEngine(templateConfig);
    }

    /**
     * 连接时序数据库 ，若不存在则创建
     *
     * @return
     */
    public InfluxDB influxDbBuild(InfluxProperties influxProperties) {
        try {
            if (influxDB == null) {
                influxDB = InfluxDBFactory.connect(influxProperties.getUrl(), influxProperties.getUsername(), influxProperties.getPassword());
            }
            this.createDB(database);
        } catch (Exception e) {
            // 该数据库可能设置动态代理，不支持创建数据库
            // e.printStackTrace();
        } finally {
            influxDB.setRetentionPolicy(retentionPolicy);
        }
        influxDB.setLogLevel(InfluxDB.LogLevel.NONE);
        return influxDB;
    }


    public InfluxDB getInfluxDB() {
        return this.influxDB;
    }

    /**
     * 选择查询列表
     *
     * @param mapper 映射器名称
     * @param params 映射器参数
     * @return 列表结果
     */
    public List<Map<String, Object>> selectList(String mapper, Object params) {
        return this.selectListByDB(this.database, mapper, params);
    }

    /**
     * 选择分页列表
     *
     * @param mapper   数据库操作的接口名
     * @param pageForm 分页参数对象
     * @param params   查询参数
     * @return 分页数据集合
     */

    public DataPaging<Map<String, Object>> selectPageList(String mapper, PageForm pageForm, Object params) {
        return this.selectPageList(mapper, pageForm, params);
    }

    /**
     * 通过分页查询数据库中的数据，并返回分页数据对象
     *
     * @param mapper   数据库操作的mapper接口
     * @param pageForm 分页信息对象，包含当前页码、每页显示条数等信息
     * @param clazz    查询结果的数据类型
     * @param params   数据库查询参数
     * @return 分页数据对象，包含查询结果的数据列表和分页信息
     */
    public <T> DataPaging<T> selectPageList(String mapper, PageForm pageForm, Class<T> clazz, Object params) {
        // 查询出一共的条数
        Long total = this.count(mapper, params);
        String pageSql = this.buildPageSql(mapper, pageForm, params);
        List<T> result = this.selectListBySql(this.database, pageSql, clazz);
        return new DataPaging<T>(result, total, pageForm);
    }


    private TimedCache<String, String> mapCountCache = CacheUtil.newTimedCache(1000 * 60 * 60 * 24);

    /**
     * 计算满足条件的记录数量
     *
     * @param mapper 数据库操作的mapper接口
     * @param params 查询参数
     * @return 满足条件的记录数量
     */
    public Long count(String mapper, Object params) {
        Map<String, Object> result = this.selectOneBySql(this.database, this.buildCountSql(mapper, params), Map.class);
        final Long[] count = {0L};
        if (MapUtil.isEmpty(result)) {
            return count[0];
        }
        JSONObject jsonObject = new JSONObject(result);
        result.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object val) {
                if ("count".equals(key)) {
                    count[0] = jsonObject.getLong(key);
                    return;
                }
                if (!"count".equals(StrUtil.subBefore(key, "_", false))) {
                    return;
                }
                Long v = jsonObject.getLong(key);
                //如果是最大
                if (NumberUtil.compare(v, count[0]) == 1) {
                    count[0] = v;
                    mapCountCache.put(mapper, StrUtil.removePrefix(key, "count_"));
                }
            }
        });
        return count[0];
    }

    public Map<String, Object> selectOne(String mapper, Object params) {
        return this.selectOneByDB(this.database, mapper, params);
    }


    public String buildCountSql(String mapper, Object params) {
        String sql = this.buildSql(mapper, params);
        //去除查询字段
        String reg = "(?<=SELECT).*?(?=FROM)";
        String ff = ReUtil.getGroup0(reg, sql);
//        System.out.println(ff);
//        sql = StrUtil.replace(sql, ff, StrUtil.format(" COUNT({}) ", field));
        String fidleName = ObjectUtil.defaultIfNull(mapCountCache.get(mapper), "*");
        sql = StrUtil.replace(sql, ff, StrUtil.format(" COUNT({}) ", fidleName));

        //去除尾部的分页
//        String reg2 = "(?<=LIMIT).*";
//        String dd = ReUtil.getGroup0(reg2, sql);
//        sql = StrUtil.replace(sql, dd, "");
//        if (influxProperties.getShowSql()) {
//            log.info("influx count sql:\r\n{}", sql);
//        }
        return vaSql(sql);
    }


    public String vaSql(String sql) {
        String[] regs = new String[]{"(?<=WHERE).*?AND", "WHERE.*?(?=ORDER BY)"};
        String[] jg = new String[]{"AND", "WHERE"};

        for (int i = 0; i < regs.length; i++) {
            String reg = regs[i];
            String j = jg[i];
            String dd = ReUtil.getGroup0(reg, sql);
            if (StrUtil.trimToEmpty(dd).equals(j)) {
                sql = StrUtil.replaceFirst(sql, dd, " ");
            }
        }
        return sql;
    }

    /**
     * 构建分页SQL语句
     *
     * @param mapper   数据源名
     * @param pageForm 分页参数对象
     * @param params   查询参数对象
     * @return 构建完成的分页SQL语句
     */
    public String buildPageSql(String mapper, PageForm pageForm, Object params) {
        String sql = this.buildSql(mapper, params);
        sql = StrUtil.format("{} LIMIT {} OFFSET {}", sql, pageForm.getPageSize(), PageUtil.getStart(pageForm.getCurrPage() - 1, pageForm.getPageSize()));
        if (influxProperties.getShowSql()) {
            log.info("influx page sql:\r\n{}", sql);
        }
        return vaSql(sql);
    }

    /**
     * 构建SQL语句
     *
     * @param mapper 映射器
     * @param params 参数
     * @return 构建的SQL语句
     */
    public String buildSql(String mapper, Object params) {
        Template template = templateEngine.getTemplate(mapper + ".sql");
        Map<?, ?> map = Maps.newHashMap();
        if (params instanceof Map) {
            map = (Map) params;
        } else {
            map = BeanUtil.beanToMap(params);
        }
        String sql = template.render(map);
        if (influxProperties.getShowSql()) {
            log.info("influx sql:\r\n{}", sql);
        }
        return vaSql(sql);
    }

    /**
     * 根据指定的数据库和 SQL 语句查询结果集并返回 List 对象。
     *
     * @param database 数据库名称
     * @param sql      SQL 语句
     * @param clazz    结果集中对象的类型
     * @return 结果集的 List 对象
     */
    public <T> List<T> selectListBySql(String database, String sql, Class<?> clazz) {
        InfluxDataDeserializer influxDataDeserializer = new InfluxDataDeserializer(clazz);
        QueryResult queryResult = this.influxDB.query(new Query(sql, database));
        return influxDataDeserializer.deserializerObject(queryResult);
    }

    /**
     * 根据给定的数据库名、SQL语句和类类型，在数据库中查询一条数据并返回。
     *
     * @param database 数据库名
     * @param sql      SQL语句
     * @param clazz    返回值的类类型
     * @param <T>      返回值的类型
     * @return 符合条件的一条数据，或者为null
     */
    public <T> T selectOneBySql(String database, String sql, Class<?> clazz) {
        List<T> list = this.selectListBySql(database, sql, clazz);
        return CollUtil.getFirst(list);
    }

    /**
     * 根据指定的数据库、映射器和参数，在数据库中执行查询操作，并返回结果集。
     *
     * @param database 数据库名称
     * @param mapper   映射器名称
     * @param params   查询参数
     * @return 查询结果集，以列表形式返回。每个结果使用键值对形式的Map表示。
     */
    public List<Map<String, Object>> selectListByDB(String database, String mapper, Object params) {
        String sql = this.buildSql(mapper, params);
        return this.selectListBySql(database, sql, Map.class);
    }

    /**
     * 通过指定的数据库、mapper和参数，在数据库中执行选择一条记录的操作。
     *
     * @param database 数据库名称
     * @param mapper   数据库mapper对象
     * @param params   执行查询的参数
     * @return 包含一条记录的映射（Map<String, Object>）。
     */
    public Map<String, Object> selectOneByDB(String database, String mapper, Object params) {
        List<Map<String, Object>> list = this.selectListByDB(database, mapper, params);
        return CollUtil.getFirst(list);
    }

    /**
     * 创建数据库
     *
     * @param dbName
     */
    @SuppressWarnings("deprecation")
    public void createDB(String dbName) {
        influxDB.createDatabase(dbName);
    }

    /**
     * 删除数据库
     *
     * @param dbName
     */
    @SuppressWarnings("deprecation")
    public void deleteDB(String dbName) {
        influxDB.deleteDatabase(dbName);
    }

    /**
     * 查询
     *
     * @param command 查询语句
     * @return
     */
    public QueryResult query(String command) {
        return influxDB.query(new Query(command, database));
    }


    /**
     * 插入
     *
     * @param measurement 表
     * @param item        插入对象
     * @param timeField   插入的标记字段
     * @param tagFields   tag字段
     */
    public void insert(String measurement, Object item, Object timeField, List<String> tagFields, InfluxFeature...
            influxFeatures) {
        Point.Builder builder = this.buildPoint(measurement, item, timeField, tagFields, influxFeatures);
        influxDB.write(database, retentionPolicy, builder.build());
    }


    /**
     * 创建自定义保留策略
     *
     * @param policyName  策略名
     * @param duration    保存天数
     * @param replication 保存副本数量
     * @param isDefault   是否设为默认保留策略
     */
    public void createRetentionPolicy(String policyName, String duration, int replication, Boolean isDefault) {
        String sql = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s ", policyName,
                database, duration, replication);
        if (isDefault) {
            sql = sql + " DEFAULT";
        }
        this.query(sql);
    }


    /**
     * 构建点位
     *
     * @param measurement    测量名称
     * @param item           数据项
     * @param timeField      时间字段
     * @param tagFields      标签字段列表
     * @param influxFeatures InfluxDB特性列表
     * @return Point.Builder对象
     */
    public Point.Builder buildPoint(String measurement, Object item, Object
            timeField, List<String> tagFields, InfluxFeature... influxFeatures) {
        Point.Builder builder = Point.measurement(measurement);
        Map<String, Object> fields = Maps.newHashMap();
        if (item instanceof Map) {
            fields = (Map<String, Object>) item;
        } else {
            fields = BeanUtil.beanToMap(item);
        }
        if (MapUtil.isEmpty(fields)) {
//            String err="所有字段为空";
            NullPointerException exception = new NullPointerException(StrUtil.format("所有字段为空:{}", JSON.toJSONString(item)));
            throw exception;
        }
        JSONObject jsonObject = new JSONObject(fields);
        if (timeField instanceof Date) {
            //设置点位时间
            Long nanoTime = System.nanoTime();
            long t = ((Date) timeField).getTime();
            builder.time(t, TimeUnit.MICROSECONDS);
        } else {
            //设置点位时间
            Long nanoTime = System.nanoTime();
            long t = jsonObject.getDate(timeField.toString()).getTime();
            builder.time(t, TimeUnit.MICROSECONDS);
//            jsonObject.remove(timeField);
            jsonObject.remove("time");
        }

        fields.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                if (ObjectUtil.isEmpty(value)) {
                    return;
                }
                String newKey = key;
                if (ArrayUtil.contains(influxFeatures, InfluxFeature.toUnderlineCase)) {
                    newKey = StrUtil.toUnderlineCase(key);
                }
                if (ArrayUtil.contains(influxFeatures, InfluxFeature.toCamelCase)) {
                    newKey = StrUtil.toCamelCase(key);
                }
                //说明是tag
                if (CollUtil.contains(tagFields, key)) {
                    if (value instanceof Number) {
                        builder.tag(newKey, StrUtil.toString(value));
                    } else if (value instanceof String) {
                        builder.tag(newKey, StrUtil.toString(value));
                    } else {
                        builder.tag(newKey, JSON.toJSONString(value));
                    }
                } else {
                    if (value instanceof Number) {
                        if (value instanceof Short) {
                            builder.addField(newKey, ((Short) value));
                        } else if (value instanceof Float) {
                            builder.addField(newKey, ((Float) value));
                        } else if (value instanceof Double) {
                            builder.addField(newKey, ((Double) value));
                        } else if (value instanceof Integer) {
                            builder.addField(newKey, ((Integer) value));
                        } else if (value instanceof Long) {
                            builder.addField(newKey, ((Long) value));
                        } else if (value instanceof BigInteger) {
                            builder.addField(newKey, ((BigInteger) value).doubleValue());
                        } else if (value instanceof BigDecimal) {
                            builder.addField(newKey, ((BigDecimal) value).doubleValue());
                        }
                    } else if (value instanceof String) {
                        builder.addField(newKey, StrUtil.toString(value));
                    } else {
                        builder.addField(newKey, JSON.toJSONString(value));
                    }
                }
            }
        });
        return builder;
    }

    /**
     * 批量插入数据点到指定的测量中。
     *
     * @param measurement    测量名称
     * @param items          要插入的数据点列表
     * @param timeField      时间字段
     * @param tagFields      标签字段列表
     * @param influxFeatures InfluxDB功能参数列表
     */
    public <T> void batchInsertItem(final String measurement, final List<T> items, Object
            timeField, List<String> tagFields, InfluxFeature... influxFeatures) {
        this.batchInsertForPolicy(measurement, this.retentionPolicy, items, timeField, tagFields, influxFeatures);
    }


    /**
     * 批量插入数据到指定的策略下
     *
     * @param measurement     测量指标
     * @param retentionPolicy 保留策略
     * @param items           数据项列表
     * @param timeField       时间字段
     * @param tagFields       标签字段列表
     * @param influxFeatures  InfluxDB功能参数
     * @param <T>             数据类型
     */
    public <T> void batchInsertForPolicy(final String measurement, final String retentionPolicy,
                                         final List<T> items, Object timeField, List<String> tagFields, InfluxFeature... influxFeatures) {
        //设置数据库
        BatchPoints.Builder batchBuilder = BatchPoints.database(database);
        //设置保存策略
        batchBuilder.retentionPolicy(retentionPolicy);
        items.forEach(new Consumer<Object>() {
            @Override
            public void accept(Object item) {
                Point.Builder builder = buildPoint(measurement, item, timeField, tagFields, influxFeatures);
                batchBuilder.point(builder.build());
            }
        });
        influxDB.write(batchBuilder.build());
        // influxDB.enableGzip();
        // influxDB.enableBatch(2000,100,TimeUnit.MILLISECONDS);
        // influxDB.disableGzip();
        // influxDB.disableBatch();
    }

    /**
     * 批量插入数据点
     *
     * @param batchPoints 包含数据点的批处理对象
     */
    public void batchInsert(BatchPoints batchPoints) {
        influxDB.write(batchPoints);
        // influxDB.enableGzip();
        // influxDB.enableBatch(2000,100,TimeUnit.MILLISECONDS);
        // influxDB.disableGzip();
        // influxDB.disableBatch();
    }

    /**
     * 批量写入数据
     *
     * @param database        数据库
     * @param retentionPolicy 保存策略
     * @param consistency     一致性
     * @param records         要保存的数据（调用BatchPoints.lineProtocol()可得到一条record）
     */
    public void batchInsert(final String database, final String retentionPolicy,
                            final InfluxDB.ConsistencyLevel consistency,
                            final List<String> records) {
        influxDB.write(database, retentionPolicy, consistency, records);
    }

    /**
     * 删除
     *
     * @param command 删除语句
     * @return 返回错误信息
     */
    public String deleteMeasurementData(String command) {
        QueryResult result = influxDB.query(new Query(command, database));
        return result.getError();
    }


}
