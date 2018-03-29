package td.framework.boot.autoconfigure.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.td.framework.mongo.AdvMongoTemplate;
import com.td.framework.service.support.MongoFileSupportService;

/**
 * 默认的Mongo注入
 * 
 * @author wesley
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "database")
@AutoConfigureAfter({ MongoDataAutoConfiguration.class })
@ConditionalOnClass({ MongoDbFactory.class, AdvMongoTemplate.class })
public class MongoAutoConfiguration {

	@Autowired
	private MongoDbFactory mongoDbFactory;

	@Bean(name = "mongoTemplate")
	@ConditionalOnMissingBean

	@ConditionalOnBean({ MongoDbFactory.class })
	public AdvMongoTemplate advMongoTemplate() {
		return new AdvMongoTemplate(mongoDbFactory);
	}

	@Configuration
	@ConditionalOnBean({ GridFsTemplate.class })
	@ConditionalOnClass(MongoFileSupportService.class)
	@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "grid-fs-database")
	public static class MongoFileSupportServiceConfig {
		@Autowired
		private GridFsTemplate gridFsTemplate;

		@Bean
		@ConditionalOnMissingBean
		public MongoFileSupportService mongoFileSupportService() {
			return new MongoFileSupportService(gridFsTemplate);
		}
	}

}
