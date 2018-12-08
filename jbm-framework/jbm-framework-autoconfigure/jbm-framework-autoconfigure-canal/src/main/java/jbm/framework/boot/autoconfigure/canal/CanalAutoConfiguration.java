package jbm.framework.boot.autoconfigure.canal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author wesley
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "canal", name = "host")
@EnableConfigurationProperties(CanalProperties.class)
public class CanalAutoConfiguration {

	@Bean
	public CanalTemplate canalTemplate(CanalProperties canalProperties) throws Exception {
		CanalTemplate canalTemplate = new CanalTemplate(canalProperties);
		return canalTemplate;
	}

	@Bean
	public CanalEventCenter canalEventCenter(CanalTemplate canalTemplate) throws Exception {
		CanalEventCenter canalEventCenter = new CanalEventCenter(canalTemplate);
		canalEventCenter.startAsync();
		return canalEventCenter;
	}

}
