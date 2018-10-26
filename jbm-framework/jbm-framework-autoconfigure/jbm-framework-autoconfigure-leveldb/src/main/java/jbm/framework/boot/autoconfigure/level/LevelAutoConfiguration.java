package jbm.framework.boot.autoconfigure.level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.level.LevelKeyValueAdapter;
import org.springframework.data.level.LevelKeyValueTemplate;
import org.springframework.data.level.LevelMappingContext;
import org.springframework.data.level.LevelTemplate;

@Configuration
@EnableConfigurationProperties(LevelProperties.class)
@ConditionalOnProperty(prefix = "spring.data.level", name = "root")
public class LevelAutoConfiguration {

	@Autowired
	private LevelProperties levelProperties;

	@Bean
	public LevelTemplate<Object, Object> levelTemplate() {
		return new LevelTemplate<Object, Object>(levelProperties);
	}

	@Bean
	public LevelKeyValueTemplate levelKeyValueTemplate(LevelTemplate<Object, Object> levelTemplate) throws Exception {
		LevelKeyValueAdapter keyValueAdapter = new LevelKeyValueAdapter(levelTemplate, levelProperties);
		keyValueAdapter.afterPropertiesSet();
		return new LevelKeyValueTemplate(keyValueAdapter, new LevelMappingContext());
	}

}
