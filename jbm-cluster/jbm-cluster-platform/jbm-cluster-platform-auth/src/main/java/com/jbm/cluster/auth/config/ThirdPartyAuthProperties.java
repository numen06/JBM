package com.jbm.cluster.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wesley
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.auth")
public class ThirdPartyAuthProperties {
    
    private Map<String, PlatformConfig> platforms = new HashMap<>();
    
    @Data
    public static class PlatformConfig {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String tokenUrl;
        private String userInfoUrl;
        private String logoutUrl;
        private String loginUrl;
        private String scope;
    }
}