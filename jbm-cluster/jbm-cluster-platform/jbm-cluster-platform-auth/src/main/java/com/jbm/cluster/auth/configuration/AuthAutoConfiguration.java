package com.jbm.cluster.auth.configuration;

import com.jbm.cluster.auth.service.JbmPlatformClientModelSource;
import com.jbm.cluster.common.satoken.config.SaTokenConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(SaTokenConfiguration.class)
public class AuthAutoConfiguration {

    @Bean
    public JbmPlatformClientModelSource jbmPlatformClientModelSource() {
        return new JbmPlatformClientModelSource();
    }
}
