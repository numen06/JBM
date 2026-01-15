package jbm.framework.spring.config;

import jbm.framework.spring.ApplicationInstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * ApplicationInstanceInfo 事件监听器
 * 监听 Spring 应用准备完成事件（ApplicationPreparedEvent），直接使用事件中的 ApplicationContext 初始化
 * 确保在上下文就绪后能够正确获取应用信息
 * 
 * @author wesley
 */
@Slf4j
public class ApplicationInstanceInfoListener implements ApplicationListener<ApplicationPreparedEvent> {

    private static volatile boolean initialized = false;

    public ApplicationInstanceInfoListener() {
        log.info("ApplicationInstanceInfoListener 已创建并注册");
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        if (initialized) {
            log.debug("ApplicationInstanceInfo 已经初始化过，跳过");
            return;
        }
        
        try {
            ApplicationContext context = event.getApplicationContext();
            log.info("Spring 应用准备完成，开始初始化 ApplicationInstanceInfo");
            
            // 直接使用事件中的 ApplicationContext 初始化，而不是通过 SpringContextHolder
            // 因为此时 SpringContextHolder 可能还未通过 ApplicationContextAware 注入
            ApplicationInstanceInfo.reset();
            ApplicationInstanceInfo.initializeWithContext(context);
            initialized = true;
            
            log.info("ApplicationInstanceInfo 初始化完成 - 应用名称: {}, 端口: {}, 实例ID: {}", 
                    ApplicationInstanceInfo.getApplicationName(),
                    ApplicationInstanceInfo.getPort(),
                    ApplicationInstanceInfo.getInstanceId());
        } catch (Exception e) {
            log.error("ApplicationInstanceInfo 初始化失败", e);
        }
    }
}
