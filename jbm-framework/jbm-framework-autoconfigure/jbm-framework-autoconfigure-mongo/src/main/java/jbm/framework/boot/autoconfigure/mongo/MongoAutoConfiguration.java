package jbm.framework.boot.autoconfigure.mongo;

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

import com.jbm.framework.mongo.AdvMongoTemplate;
import com.jbm.framework.mongo.MongoFileSupportService;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

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

	@Bean
	@ConditionalOnBean({ MongoDbFactory.class })
	public GridFSBucket gridFSBuckets() {
		MongoDatabase db = mongoDbFactory.getDb();
		return GridFSBuckets.create(db);
	}

	@Configuration
	@ConditionalOnBean({ GridFsTemplate.class })
	@ConditionalOnClass(MongoFileSupportService.class)
	@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "grid-fs-database")
	public static class MongoFileSupportServiceConfig {
		@Autowired
		private GridFsTemplate gridFsTemplate;

		@Autowired
		private GridFSBucket gridFSBucket;

		@Bean
		@ConditionalOnMissingBean
		public MongoFileSupportService mongoFileSupportService() {
			return new MongoFileSupportService(gridFsTemplate, gridFSBucket);
		}
	}

}
