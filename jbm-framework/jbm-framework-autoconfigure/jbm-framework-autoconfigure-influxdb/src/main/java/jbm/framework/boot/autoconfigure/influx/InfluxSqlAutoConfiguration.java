package jbm.framework.boot.autoconfigure.influx;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.influx.InfluxTemplate;

@Slf4j
@ConditionalOnClass(name = "org.apache.ibatis.session.SqlSession")
@EnableConfigurationProperties(InfluxProperties.class)
@ConditionalOnProperty(prefix = "spring.data.influx", name = "url")
public class InfluxSqlAutoConfiguration {

    @Autowired
    private InfluxProperties influxProperties;

    @Autowired(required = false)
    private SqlSession sqlSession;

    @Bean

    public InfluxTemplate influxTemplate() {
        log.info("jbm influxTemplate init,url:{},database:{},username:{}", influxProperties.getUrl(), influxProperties.getDatabase(), influxProperties.getUsername());
        final InfluxDB influxDB = InfluxDBFactory.connect(influxProperties.getUrl(), influxProperties.getUsername(), influxProperties.getPassword());
        InfluxTemplate influxTemplate = new InfluxTemplate(influxDB, sqlSession);
        if (StrUtil.isNotBlank(influxProperties.getDatabase())) {
            influxTemplate.setDatabase(influxProperties.getDatabase());
        }
        return influxTemplate;
    }



}
