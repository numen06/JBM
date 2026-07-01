package com.jbm.cluster.doc.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocUploadSecurityProperties.class)
public class DocUploadAutoConfiguration {
}
