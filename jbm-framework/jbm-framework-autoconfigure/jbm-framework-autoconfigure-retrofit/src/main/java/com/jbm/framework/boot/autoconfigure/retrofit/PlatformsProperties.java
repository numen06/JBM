package com.jbm.framework.boot.autoconfigure.retrofit;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wesley
 */
@ConfigurationProperties(prefix = "retrofit")
@Data
public class PlatformsProperties implements InitializingBean {

    private Map<String, Platform> platforms = new HashMap<>();

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        platforms.forEach((k, platform) -> {
            platform.setName(k);
        });
    }

    @Data
    public static class Platform {
        private String name;
        private String baseUrl;
        private String clientId;
        private String clientSecret;
        //        private String signatureStrategy;
//        private String authStrategy;
        private Map<String, String> extend;
    }

    public static Platform NonePlatform() {
        Platform platform = new Platform();
        platform.setName("None");
        return platform;
    }

}