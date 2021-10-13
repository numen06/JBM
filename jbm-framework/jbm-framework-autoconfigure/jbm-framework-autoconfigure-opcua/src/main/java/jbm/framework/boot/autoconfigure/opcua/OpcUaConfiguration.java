package jbm.framework.boot.autoconfigure.opcua;

import com.jbm.framework.opcua.OpcUaTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
@EnableConfigurationProperties(OpcUaProperties.class)
@ConditionalOnProperty(prefix = "opcua", name = "enabled", matchIfMissing = false)
public class OpcUaConfiguration {

    @Bean
    public OpcUaTemplate opcUaTemplate(OpcUaProperties opcUaProperties) {
        OpcUaTemplate opcUaTemplate = new OpcUaTemplate();
        opcUaTemplate.loadClients(opcUaProperties.getClients());
        return opcUaTemplate;
    }


}
