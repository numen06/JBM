package jbm.framework.boot.autoconfigure.redis.annotation;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-09 19:23
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
//@Cacheable
public @interface RedisLock {

    /**
     * 锁的资源，redis的key
     */
    String key() default "";

    String scope() default "";

    /**
     * 持锁时间
     */
    long time() default 30000;

    /**
     * 当获取失败时候动作
     */
    LockFailAction action() default LockFailAction.CONTINUE;

    enum LockFailAction {
        /**
         * 放弃
         */
        GIVEUP,
        /**
         * 继续
         */
        CONTINUE;
    }

    TimeUnit unit() default TimeUnit.MILLISECONDS;
}