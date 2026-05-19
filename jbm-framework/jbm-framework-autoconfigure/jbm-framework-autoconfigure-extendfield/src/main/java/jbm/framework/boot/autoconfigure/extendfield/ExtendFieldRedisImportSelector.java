package jbm.framework.boot.autoconfigure.extendfield;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * 仅当 Redis 模块在 classpath 时导入 {@link RedisExtendFieldAutoConfiguration}。
 */
public class ExtendFieldRedisImportSelector implements ImportSelector {

    private static final String REDIS_SERVICE = "jbm.framework.boot.autoconfigure.redis.RedisService";

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        if (ClassUtils.isPresent(REDIS_SERVICE, ClassUtils.getDefaultClassLoader())) {
            return new String[]{RedisExtendFieldAutoConfiguration.class.getName()};
        }
        return new String[0];
    }
}
