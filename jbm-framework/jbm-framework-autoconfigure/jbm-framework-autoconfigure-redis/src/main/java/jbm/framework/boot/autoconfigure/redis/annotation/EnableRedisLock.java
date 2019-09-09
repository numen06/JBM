package jbm.framework.boot.autoconfigure.redis.annotation;

import jbm.framework.boot.autoconfigure.redis.lock.aop.DistributedLockAspectConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-09 19:25
 **/
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({DistributedLockAspectConfiguration.class})
public @interface EnableRedisLock {
}
