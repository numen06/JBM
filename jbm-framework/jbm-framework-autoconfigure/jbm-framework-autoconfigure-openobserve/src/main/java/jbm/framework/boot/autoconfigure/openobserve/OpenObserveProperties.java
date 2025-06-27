package jbm.framework.boot.autoconfigure.openobserve;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "open-observe")
public class OpenObserveProperties {

    private String url;
    private String organization;
    private String stream;
    private String username;
    private String password;
}
