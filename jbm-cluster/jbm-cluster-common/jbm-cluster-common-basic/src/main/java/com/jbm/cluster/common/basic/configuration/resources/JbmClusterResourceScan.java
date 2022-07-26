package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.date.StopWatch;
import com.jbm.cluster.api.model.JbmClusterResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.module.JbmClusterStreamTemplate;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public abstract class JbmClusterResourceScan<T extends JbmClusterResource> implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private JbmClusterProperties jbmClusterProperties;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public JbmClusterResourceScan() {
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!this.enable(jbmClusterProperties)) {
            return;
        }
        log.info("资源采集开始，线程数量:{}", threadPoolTaskExecutor.getCorePoolSize());
        StopWatch stopWatch = new StopWatch("资源采集开始");
        stopWatch.start();
        try {
            T resource = this.scan();
            String serviceId = SpringContextHolder.geteApplicationName();
            resource.setServiceId(serviceId);
            final JbmClusterStreamTemplate streamTemplate = SpringContextHolder.getBean(JbmClusterStreamTemplate.class);
            streamTemplate.sendResource(this.queue(), resource);
        } catch (Exception e) {
            log.error("资源采集失败");
        } finally {
            stopWatch.stop();
            // 打印出耗时
            log.info("资源采集结束,用时:{}秒", stopWatch.getTotalTimeSeconds());
        }
    }

    public abstract String queue();

    public abstract boolean enable(JbmClusterProperties jbmClusterProperties);


    public abstract T scan();

}
