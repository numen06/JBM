package jbm.framework.spring.config;

import jbm.framework.spring.ApplicationInstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;

/**
 * ApplicationInstanceInfo 事件监听器
 * 监听 Spring 应用准备完成事件（ApplicationPreparedEvent），这是最早能访问 ApplicationContext 的事件
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
            log.info("Spring 应用准备完成，开始初始化 ApplicationInstanceInfo");
            ApplicationInstanceInfo.reset();
            ApplicationInstanceInfo.initialize();
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
