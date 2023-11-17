package jbm.framework.boot.autoconfigure.influx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.data.influx")
public class InfluxProperties {

    private String url;
    private String username;
    private String password;
    private String database;
    private Boolean showSql = false;


}
