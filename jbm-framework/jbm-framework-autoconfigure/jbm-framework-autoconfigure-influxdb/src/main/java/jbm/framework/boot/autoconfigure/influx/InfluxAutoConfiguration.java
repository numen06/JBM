package jbm.framework.boot.autoconfigure.influx;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.influx.SimpleInfluxTemplate;

@Slf4j
@EnableConfigurationProperties(InfluxProperties.class)
@ConditionalOnProperty(prefix = "spring.data.influx", name = "url")
public class InfluxAutoConfiguration {

    @Autowired
    private InfluxProperties influxProperties;

    @Bean
    public SimpleInfluxTemplate simpleInfluxTemplate() {
        final InfluxDB influxDB = InfluxDBFactory.connect(influxProperties.getUrl(), influxProperties.getUsername(), influxProperties.getPassword());
        SimpleInfluxTemplate influxTemplate = new SimpleInfluxTemplate(influxDB);
        if (StrUtil.isNotBlank(influxProperties.getDatabase())) {
            influxTemplate.setDatabase(influxProperties.getDatabase());
        }
        return influxTemplate;
    }


}
