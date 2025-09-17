package jbm.framework.boot.autoconfigure.emqx.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "emqx.api")
public class EmqxProperties {

    private String url;
    private String username;
    private String password;

    private EmqxMqttProperties mqtt;


}
