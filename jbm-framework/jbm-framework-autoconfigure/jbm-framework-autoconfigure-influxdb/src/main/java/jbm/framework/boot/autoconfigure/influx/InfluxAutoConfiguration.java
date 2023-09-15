package jbm.framework.boot.autoconfigure.influx;

import lombok.extern.slf4j.Slf4j;
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
        SimpleInfluxTemplate influxTemplate = new SimpleInfluxTemplate(influxProperties);
        return influxTemplate;
    }


}
