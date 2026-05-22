package com.jbm.cluster.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jbm.api")
public class ApiLoginSecurityProperties {

    private boolean loginPlaintextDevOnly = false;
}
