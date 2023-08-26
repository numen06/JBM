package com.jbm.cluster.doc.config;

import com.jbm.cluster.doc.config.wps.WpsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WpsProperties.class, RedirectProperties.class})
public class WpsAutoConfiguration {

    @Autowired
    private WpsProperties wpsProperties;
    @Autowired
    private RedirectProperties redirect;

    @Bean
    public WpsTemplate wpsTemplate() {
        return new WpsTemplate(wpsProperties, redirect);
    }

}
