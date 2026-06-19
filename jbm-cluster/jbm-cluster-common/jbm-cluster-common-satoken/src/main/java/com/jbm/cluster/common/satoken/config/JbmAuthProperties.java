package com.jbm.cluster.common.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JBM downstream auth integration mode.
 */
@Data
@ConfigurationProperties(prefix = "jbm.security.auth")
public class JbmAuthProperties {

    /**
     * mixed: shared Redis Sa-Token plus standard OAuth JWT.
     * redis: shared Redis Sa-Token only.
     * oauth: standard OAuth JWT only.
     */
    private Mode mode = Mode.MIXED;

    public boolean isRedisEnabled() {
        return mode == Mode.REDIS || mode == Mode.MIXED;
    }

    public boolean isOauthEnabled() {
        return mode == Mode.OAUTH || mode == Mode.MIXED;
    }

    public enum Mode {
        REDIS,
        OAUTH,
        MIXED
    }
}
