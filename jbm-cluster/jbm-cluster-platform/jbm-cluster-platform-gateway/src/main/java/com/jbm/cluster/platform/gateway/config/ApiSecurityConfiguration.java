package com.jbm.cluster.platform.gateway.config;

import com.jbm.cluster.platform.gateway.config.properties.ApiSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ApiSecurityConfiguration {
}
