package com.jbm.cluster.platform.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "jbm.api")
public class ApiSecurityProperties {

    private boolean checkSign = false;

    private boolean checkSensitiveEncrypt = false;

    private boolean loginPlaintextDevOnly = false;

    private long signExpireMs = 300_000L;

    private List<String> signIgnores = new ArrayList<>();

    private List<String> permitAll = new ArrayList<>();

    /** 是否校验 base_api_key 的接口授权范围 */
    private boolean checkAuth = false;

    private List<String> authIgnores = new ArrayList<>();
}
