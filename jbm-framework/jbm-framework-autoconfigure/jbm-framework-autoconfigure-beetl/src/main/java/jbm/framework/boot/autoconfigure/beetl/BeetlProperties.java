package jbm.framework.boot.autoconfigure.beetl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "beetl")
public class BeetlProperties {

    private Resource config;

    public Resource getConfig() {
        return config;
    }

    public void setConfig(Resource config) {
        this.config = config;
    }

}
