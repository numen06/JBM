package com.jbm.framework.dao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Created wesley.zhang
 * @Date 2022/5/11 19:53
 * @Description TODO
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
