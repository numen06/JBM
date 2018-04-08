package com.baidu.disconf.app;

import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import com.baidu.disconf.web.innerapi.zookeeper.ZooKeeperDriver;
import com.baidu.disconf.web.innerapi.zookeeper.impl.ZookeeperDriverImpl;
import com.baidu.disconf.web.service.zookeeper.config.ZooConfig;
import com.baidu.disconf.web.web.auth.login.impl.RedisLoginImpl;
import com.github.knightliao.apollo.redis.RedisCacheManager;
import com.github.knightliao.apollo.redis.RedisClient;
import com.github.knightliao.apollo.redis.config.RedisHAClientConfig;

@Configuration
public class StartInit {

	@Bean
	public ZooConfig zooConfig() {
		ZooConfig config = new ZooConfig();
		config.setZooHosts("127.0.0.1:2181");
		config.setZookeeperUrlPrefix("/disconfig");
		return config;
	}

	@Bean
	public ZooKeeperDriver zooKeeperDriver() {
		ZooKeeperDriver dri = new ZookeeperDriverImpl();
		return dri;
	}

	@Autowired
	private Environment environment;

	// @Bean
	public RedisCacheManager redisCacheManager() {
		RedisCacheManager redis = new RedisCacheManager();
		RedisHAClientConfig config = new RedisHAClientConfig();
		config.setCacheName("BeidouRedis1");
		config.setRedisServerHost("192.168.1.6");
		config.setRedisAuthKey("password");
		RedisClient client = new RedisClient(config);
		redis.setClientList(Lists.newArrayList(client));
		return redis;
	}

	@Bean
	public RedisLoginImpl redisLoginImpl() {
		return new RedisLoginImpl();
	}
}
