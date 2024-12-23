package jbm.framework.boot.autoconfigure.td.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.data.td")
public class TDProperties {

    private String url;
    private String username;
    private String password;
    private String database;
    private Boolean showSql = false;


}
