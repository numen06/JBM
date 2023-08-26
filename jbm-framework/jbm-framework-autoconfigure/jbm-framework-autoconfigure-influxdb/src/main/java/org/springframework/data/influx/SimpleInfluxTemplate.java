package org.springframework.data.influx;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.List;
import java.util.Map;

@Slf4j
public class SimpleInfluxTemplate {

    private InfluxDB influxDB;
    private String database;

    private TemplateEngine templateEngine;

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public SimpleInfluxTemplate(InfluxDB influxDB) {
        this(influxDB, "data", "influx/sqls");
    }

    public SimpleInfluxTemplate(InfluxDB influxDB, String database, String path) {
        this.influxDB = influxDB;
        this.database = database;
        TemplateConfig templateConfig = new TemplateConfig(path, TemplateConfig.ResourceMode.CLASSPATH);
        templateConfig.setCustomEngine(BeetlSqlEngine.class);
        templateEngine = TemplateUtil.createEngine(templateConfig);
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
        log.info("influx sql:{}", sql);
        QueryResult queryResult = this.influxDB.query(new Query(sql, database));
        return influxDataDeserializer.deserializer(queryResult);
    }

    public Map<String, Object> selectOneByDB(String database, String mapper, Object params) {
        List<Map<String, Object>> list = this.selectListByDB(mapper, database, params);
        return CollectionUtil.isEmpty(list) ? null : (Map) list.get(0);
    }

}
