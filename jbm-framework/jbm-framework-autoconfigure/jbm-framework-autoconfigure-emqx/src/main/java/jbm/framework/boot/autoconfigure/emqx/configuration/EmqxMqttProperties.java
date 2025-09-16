package jbm.framework.boot.autoconfigure.emqx.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 01:01
 **/
@Data
@ConfigurationProperties(prefix = "emqx.mqtt")
public class EmqxMqttProperties {

    private String url;
    private String clientId;
    private String username;
    private String password;
}
