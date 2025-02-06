package com.jbm.framework.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wesley
 */
@ConfigurationProperties(
        prefix = "spring.datasource"
)
@Data
public class JdbcDataSourceProperties {
    private String name;
    private String driverClassName;
    private String url;
    private String username;
    private String password;
}
