package com.jbm.cluster.common.satoken.standardjwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Standard OAuth2 access token issuer settings used by the Sa-Token OAuth2 server.
 */
@Data
@ConfigurationProperties(prefix = "jbm.auth.jwt")
public class StandardJwtIssuerProperties {

    /**
     * Empty means disabled and the legacy Sa-Token token value is used.
     */
    private String privateKey;

    private String issuer;

    private String audience;

    private String keyId = "jbm-auth-rs256";

    private long accessTokenTtlSeconds = 7200;

    private long clientTokenTtlSeconds = 7200;
}
