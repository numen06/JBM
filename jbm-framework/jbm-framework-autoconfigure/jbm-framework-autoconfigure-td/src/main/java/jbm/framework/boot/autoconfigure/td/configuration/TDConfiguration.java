package jbm.framework.boot.autoconfigure.td.configuration;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author wesley
 */
@Slf4j
@EnableConfigurationProperties(TDProperties.class)
@ConditionalOnProperty(prefix = "spring.data.td", name = "url")
public class TDConfiguration {

}
