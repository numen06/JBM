package jbm.framework.boot.autoconfigure.extendfield.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 配置为 REDIS 但 classpath 无 Redis 模块时，回退读本地 YAML（不引用 Redis 类型）。
 */
@Service
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "source", havingValue = "REDIS", matchIfMissing = true)
@ConditionalOnMissingClass("jbm.framework.boot.autoconfigure.redis.RedisService")
public class LocalFieldDefinitionFallbackService extends LocalFieldDefinitionService {
}
