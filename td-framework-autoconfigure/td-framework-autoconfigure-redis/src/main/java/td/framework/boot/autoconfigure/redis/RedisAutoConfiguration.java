package td.framework.boot.autoconfigure.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 默认的缓存注入
 * 
 * @author wesley
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = "host")
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		return new RedisCacheManager(redisTemplate(connectionFactory));
	}

	@Bean
	@ConditionalOnMissingBean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(connectionFactory);
		// template.setValueSerializer(jackson2JsonRedisSerializer);
		// template.setKeySerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

}
