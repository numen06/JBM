package com.jbm.cluster.common.basic.configuration;

import cn.hutool.core.util.BooleanUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.common.basic.configuration.apis.ApiBuild;
import com.jbm.cluster.common.basic.configuration.config.JbmApiScanProperties;
import com.jbm.util.ClassUtils;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义注解扫描
 *
 * @author wesley.zhang
 */
@Slf4j
public class RequestMappingScan implements ApplicationListener<ApplicationReadyEvent> {
    private JbmApiScanProperties scanProperties;

    public RequestMappingScan(JbmApiScanProperties scanProperties) {
        this.scanProperties = scanProperties;
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(2);


    /**
     * 初始化方法
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (BooleanUtil.isFalse(scanProperties.getRegisterRequestMapping())) {
            return;
        }
        // 服务名称
        String serviceId = SpringContextHolder.geteApplicationName();
        // 所有接口映射
        final RequestMappingHandlerMapping mapping = SpringContextHolder.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        List<JbmApi> jbmApis = Lists.newArrayList();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            ApiBuild apiBuild = new ApiBuild(handlerMethodEntry.getKey(), handlerMethodEntry.getValue(), serviceId);
            jbmApis.add(apiBuild.doBuild());
        }
        JbmApiResource jbmApiResource = JbmApiResource.builder()
                .serviceId(serviceId).jbmApiList(jbmApis).build();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // 获取发送器
                    final StreamBridge streamBridge = SpringContextHolder.getBean(StreamBridge.class);
                    final Message<JbmApiResource> message = MessageBuilder.withPayload(jbmApiResource).build();
                    streamBridge.send(ClassUtils.getShortBeanName(JbmApiResource.class) + "-in-0", message);
                } catch (Exception e) {
                    log.error("发送Api资源失败:{}", e);
                }
            }
        });
    }


}
