package jbm.framework.boot.autoconfigure.beetl;

import com.google.common.io.Resources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ResourceUtils;

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
