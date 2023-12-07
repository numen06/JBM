package com.jbm.cluster.auth.configuration;

import com.jbm.cluster.auth.service.JbmPlatformClientModelSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthAutoConfiguration {

    @Bean
    public JbmPlatformClientModelSource jbmPlatformClientModelSource() {
        return new JbmPlatformClientModelSource();
    }
}
