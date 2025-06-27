package jbm.framework.boot.autoconfigure.openobserve;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

//@Configuration
@EnableConfigurationProperties(OpenObserveProperties.class)
@ConditionalOnProperty(prefix = "open-observe", name = "url")
public class OpenObserveConfiguration {

    @Bean
    public OpenObserveTemplate openObserveTemplate(OpenObserveProperties openObserveProperties) {
        return new OpenObserveTemplate(openObserveProperties);
    }


}
