package jbm.framework.boot.autoconfigure.emqx.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EMQX HTTP 钩子配置（路径、是否启用）
 */
@Data
@ConfigurationProperties(prefix = "emqx.hooks")
public class EmqxHooksProperties {
    /**
     * 是否启用 HTTP 钩子 Controller
     */
    private boolean enabled = true;
    /**
     * 钩子路径前缀，如 /iot/broker/hooks 或 /emqx/hooks
     */
    private String path = "/emqx/hooks";
}
