package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.date.StopWatch;
import com.jbm.cluster.api.model.JbmClusterResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.module.JbmClusterStreamTemplate;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
public abstract class JbmClusterResourceScan<T extends JbmClusterResource> implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private JbmClusterProperties jbmClusterProperties;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private JbmClusterStreamTemplate streamTemplate;
    @Autowired(required = false)
    protected RequestMappingHandlerMapping mapping;
    protected String serviceId;

    public JbmClusterResourceScan() {
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        if (!this.enable(jbmClusterProperties)) {
            return;
        }
        log.info("资源采集开始，线程数量:{}", threadPoolTaskExecutor.getCorePoolSize());
        StopWatch stopWatch = new StopWatch("资源采集开始");
        stopWatch.start();
        try {
            // 获取 serviceId 一次并缓存，避免重复获取
            this.serviceId = SpringContextHolder.geteApplicationName();
            T resource = this.scan(this.serviceId);
            resource.setServiceId(this.serviceId);
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


    public abstract T scan(String serviceId);

}
