package jbm.framework.boot.autoconfigure.extendfield;

import jbm.framework.boot.autoconfigure.extendfield.bootstrap.ExtendFieldLocalBootstrap;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionAdminService;
import jbm.framework.boot.autoconfigure.extendfield.service.RedisFieldDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Redis 相关扩展字段 Bean（由 {@link ExtendFieldRedisImportSelector} 按 classpath 条件导入）。
 */
@Configuration
@ConditionalOnClass(name = "jbm.framework.boot.autoconfigure.redis.RedisService")
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "source", havingValue = "REDIS", matchIfMissing = true)
@Import({
        RedisFieldDefinitionService.class,
        FieldDefinitionAdminService.class,
        ExtendFieldLocalBootstrap.class
})
public class RedisExtendFieldAutoConfiguration {
}
