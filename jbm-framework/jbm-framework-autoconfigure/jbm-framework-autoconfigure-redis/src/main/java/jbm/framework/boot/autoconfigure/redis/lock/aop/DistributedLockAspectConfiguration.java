package jbm.framework.boot.autoconfigure.redis.lock.aop;

import cn.hutool.core.util.StrUtil;
import jbm.framework.boot.autoconfigure.redis.DistributedLockAutoConfiguration;
import jbm.framework.boot.autoconfigure.redis.annotation.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;


/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-09 19:20
 **/
@Slf4j
@Aspect
@Configuration
@ConditionalOnClass(RedisLockRegistry.class)
@AutoConfigureAfter(DistributedLockAutoConfiguration.class)
public class DistributedLockAspectConfiguration {


    @Autowired
    private RedisLockRegistry redisLockRegistry;

    @Pointcut("@annotation(jbm.framework.boot.autoconfigure.redis.annotation.RedisLock)")
    private void lockPoint() {
    }

    @Around("lockPoint()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();
        RedisLock redisLockAnnotation = method.getAnnotation(RedisLock.class);
        final String key = this.parseKey(redisLockAnnotation.scope(), redisLockAnnotation.key(), method, args);
        Lock redisLock = redisLockRegistry.obtain(key);
        boolean lock = redisLock.tryLock(redisLockAnnotation.time(), redisLockAnnotation.unit());
        try {
            if (!lock) {
                log.debug("get lock failed : " + key);
                if (RedisLock.LockFailAction.GIVEUP.equals(redisLockAnnotation.action())) {
                    return null;
                }
            }
            //得到锁,执行方法，释放锁
            log.debug("get lock success : " + key);
            return pjp.proceed();
        } catch (Exception e) {
            log.error("execute locked method occured an exception", e);
        } finally {
            try {
                redisLock.unlock();
                log.debug("release lock : " + key + " success");
            } catch (Exception e) {
                log.debug("release lock : " + key + " failed");
            }

        }
        return null;
    }


    /**
     * 获取缓存的key
     * key 定义在注解上，支持SPEL表达式
     *
     * @return
     */
    private String parseKey(String scope, String key, Method method, Object[] args) {
        if (StrUtil.isBlank(scope)) {
            scope = method.toString();
        }
        String val = "";
        if (StrUtil.isNotBlank(key)) {
            //获取被拦截方法参数名列表(使用Spring支持类库)
            LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
            String[] paraNameArr = u.getParameterNames(method);
            //使用SPEL进行key的解析
            ExpressionParser parser = new SpelExpressionParser();
            //SPEL上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            //把方法参数放入SPEL上下文中
            for (int i = 0; i < paraNameArr.length; i++) {
                context.setVariable(paraNameArr[i], args[i]);
            }
            val = parser.parseExpression(key).getValue(context, String.class);
        }
        final String fianlKey = scope + ":" + val;
        return fianlKey;
    }

}