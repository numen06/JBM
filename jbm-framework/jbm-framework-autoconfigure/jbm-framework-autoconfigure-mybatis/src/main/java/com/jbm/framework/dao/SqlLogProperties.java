package com.jbm.framework.dao;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "sql-log")
public class SqlLogProperties {
    private List<String> whitelist;
}