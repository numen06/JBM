package org.springframework.data.influx;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import jbm.framework.boot.autoconfigure.influx.InfluxProperties;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Slf4j
public class SimpleInfluxTemplate {

    private InfluxDB influxDB;
    private String database;

    private String retentionPolicy = "autogen";

    private TemplateEngine templateEngine;

    private final InfluxProperties influxProperties;

    public SimpleInfluxTemplate(InfluxProperties influxProperties) {
        this(influxProperties, "influx/sqls");
    }

//    public SimpleInfluxTemplate(InfluxDB influxDB) {
//        this(influxDB, "data", "influx/sqls");
//    }

    public SimpleInfluxTemplate(InfluxProperties influxProperties, String path) {
        this.influxProperties = influxProperties;
        this.influxDbBuild(influxProperties);
        this.database = StrUtil.isEmpty(influxProperties.getDatabase()) ? "data" : influxProperties.getDatabase();
        TemplateConfig templateConfig = new TemplateConfig(path, TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig.setCustomEngine(BeetlSqlEngine.class);
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

    public List<Map<String, Object>> selectList(String mapper, Object params) {
        return this.selectListByDB(this.database, mapper, params);
    }

    public Map<String, Object> selectOne(String mapper, Object params) {
        return this.selectOneByDB(this.database, mapper, params);
    }

    public List<Map<String, Object>> selectListByDB(String database, String mapper, Object params) {
        Template template = templateEngine.getTemplate(mapper + ".sql");
        InfluxDataDeserializer influxDataDeserializer = new InfluxDataDeserializer(Map.class);
        Map<?, ?> map = Maps.newHashMap();
        if (params instanceof Map) {
            map = (Map) params;
        } else {
            map = BeanUtil.beanToMap(params);
        }
        String sql = template.render(map);
        log.info("influx sql:\r\n{}", sql);
        QueryResult queryResult = this.influxDB.query(new Query(sql, database));
        return influxDataDeserializer.deserializer(queryResult);
    }

    public Map<String, Object> selectOneByDB(String database, String mapper, Object params) {
        List<Map<String, Object>> list = this.selectListByDB(mapper, database, params);
        return CollectionUtil.isEmpty(list) ? null : (Map) list.get(0);
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
     * @param tags        标签
     * @param fields      字段
     */
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, Date time) {
        Point.Builder builder = Point.measurement(measurement);
        if (MapUtil.isNotEmpty(tags)) {
            builder.tag(tags);
        }
        JSONObject jsonObject = new JSONObject(fields);
        fields.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                if (ObjectUtil.isEmpty(value)) {
                    return;
                }
                if (value instanceof Number) {
                    if (value instanceof Short) {
                        builder.addField(key, ((Short) value).doubleValue());
                    } else if (value instanceof Integer) {
                        builder.addField(key, ((Integer) value).doubleValue());
                    } else if (value instanceof Long) {
                        builder.addField(key, ((Long) value).doubleValue());
                    } else if (value instanceof BigInteger) {
                        builder.addField(key, ((BigInteger) value).doubleValue());
                    }
                } else if (value instanceof String) {
                    builder.addField(key, StrUtil.toString(value));
                } else {
                    builder.addField(key, JSON.toJSONString(value));
                }
            }
        });

//        if (ObjectUtil.isNotEmpty(time)) {
        builder.time(time.getTime(), TimeUnit.MILLISECONDS);
//        }
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
     * 批量写入测点
     *
     * @param batchPoints
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
    public void batchInsert(final String database, final String retentionPolicy, final InfluxDB.ConsistencyLevel consistency,
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
