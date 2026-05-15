package com.jbm.cluster.common.basic.module.log;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import com.jbm.cluster.common.basic.module.JbmBusinessLogTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 运行时向 Logback ROOT 挂载 {@link BusinessLogMdcAppender}（及异步包装）。
 * <p>框架 {@code jbm-framework-autoconfigure-logger} 的 {@code logback-spring.xml} 不再静态声明该类，
 * 避免无 {@code jbm-cluster-common-basic} 依赖的应用在解析 logback 阶段失败；有 {@link JbmBusinessLogTemplate} 时由本组件完成挂载。</p>
 */
@Slf4j
@Component
@ConditionalOnBean(JbmBusinessLogTemplate.class)
public class BusinessLogAppenderConfiguration implements InitializingBean,
        ApplicationListener<ApplicationEvent> {

    private static final String BUSINESS_APPENDER = "businessLog";
    private static final String ASYNC_APPENDER = "businessLogAsync";

    @Value("${business.log.appender.enabled:true}")
    private boolean enabled;

    @Value("${business.log.appender.queue-size:1024}")
    private int queueSize;

    @Value("${business.log.appender.never-block:true}")
    private boolean neverBlock;

    @Value("${business.log.appender.discard-threshold:0}")
    private int discardThreshold;

    @Value("${business.log.appender.idle-timeout-ms:600000}")
    private long idleTimeoutMs;

    @Value("${business.log.appender.include-throwable:true}")
    private boolean includeThrowable;

    @Override
    public void afterPropertiesSet() {
        attachIfNecessary("bean-initialization");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof org.springframework.context.event.ContextRefreshedEvent) {
            attachIfNecessary("context-refreshed");
        } else if (event instanceof org.springframework.context.event.ContextClosedEvent) {
            LoggerContext context = getLoggerContext();
            if (context == null) {
                return;
            }
            Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            detach(root, ASYNC_APPENDER);
            detach(root, BUSINESS_APPENDER);
        }
    }

    private void attachIfNecessary(String trigger) {
        if (!enabled) {
            log.debug("BusinessLogMdcAppender disabled, skip register. trigger={}", trigger);
            return;
        }
        LoggerContext context = getLoggerContext();
        if (context == null) {
            return;
        }
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        if (root == null) {
            return;
        }
        synchronized (root) {
            if (root.getAppender(ASYNC_APPENDER) != null) {
                log.debug("BusinessLogMdcAppender already attached.");
                return;
            }
            log.info("Registering BusinessLogMdcAppender dynamically, trigger={}", trigger);
            BusinessLogMdcAppender delegate = new BusinessLogMdcAppender();
            delegate.setContext(context);
            delegate.setName(BUSINESS_APPENDER);
            delegate.setIdleTimeoutMs(idleTimeoutMs);
            delegate.setIncludeThrowable(includeThrowable);
            delegate.start();

            AsyncAppender async = new AsyncAppender();
            async.setContext(context);
            async.setName(ASYNC_APPENDER);
            async.setQueueSize(queueSize);
            async.setDiscardingThreshold(discardThreshold);
            async.setNeverBlock(neverBlock);
            async.addAppender(delegate);
            async.start();

            root.addAppender(async);
        }
    }

    private LoggerContext getLoggerContext() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            log.warn("LoggerFactory is not Logback context, skip BusinessLogMdcAppender register.");
            return null;
        }
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private void detach(Logger root, String name) {
        if (root == null) {
            return;
        }
        Appender<?> appender = root.getAppender(name);
        if (appender != null) {
            appender.stop();
            root.detachAppender(appender.getName());
        }
    }
}

