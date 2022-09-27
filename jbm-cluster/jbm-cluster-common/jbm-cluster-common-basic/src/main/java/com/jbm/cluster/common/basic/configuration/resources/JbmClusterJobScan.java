package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.job.SchedulerJob;
import com.jbm.cluster.api.model.job.JbmClusterJob;
import com.jbm.cluster.api.model.job.JbmClusterJobResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterResourceScan;
import com.jbm.cluster.core.constant.QueueConstants;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * 集群任务扫描
 *
 * @author wesley
 */
@Slf4j
public class JbmClusterJobScan extends JbmClusterResourceScan<JbmClusterJobResource> {


    public JbmClusterJobScan() {
    }


    @Override
    public String queue() {
        return QueueConstants.SCHEDULED_JOB_STREAM;
    }

    @Override
    public boolean enable(JbmClusterProperties jbmClusterProperties) {
        return true;
    }

    @Override
    public JbmClusterJobResource scan() {
        // 服务名称
        String serviceId = SpringContextHolder.geteApplicationName();
        // 所有接口映射
        final RequestMappingHandlerMapping mapping = SpringContextHolder.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        List<JbmClusterJob> jbmClusterJobs = Lists.newArrayList();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            if (handlerMethodEntry.getValue().hasMethodAnnotation(SchedulerJob.class)) {
                if (CollUtil.isEmpty(handlerMethodEntry.getKey().getMethodsCondition().getMethods())) {
                    continue;
                }
                SchedulerJob schedulerJob = handlerMethodEntry.getValue().getMethodAnnotation(SchedulerJob.class);
                String path = CollUtil.getFirst(handlerMethodEntry.getKey().getPatternsCondition().getPatterns());
                JbmClusterJob jbmClusterJob = new JbmClusterJob();
                jbmClusterJob.setJobName(schedulerJob.name());
                jbmClusterJob.setServiceName(serviceId);
                String url = UrlBuilder.create().setHost(serviceId).addPath(path).build();
                jbmClusterJob.setUrl(StrUtil.replace(url, UrlBuilder.create().getSchemeWithDefault(), "feign"));
//                jbmClusterJob.setJobName(handlerMethodEntry.getKey().toString());
                jbmClusterJob.setMethodType(CollUtil.getFirst(handlerMethodEntry.getKey().getMethodsCondition().getMethods()).toString());
                jbmClusterJob.setCron(schedulerJob.cron());
                jbmClusterJob.setEnable(schedulerJob.enable());
                jbmClusterJob.setContentType(ContentType.JSON.getValue());
                jbmClusterJobs.add(jbmClusterJob);
            }
        }
        JbmClusterJobResource jbmClusterJobResource = new JbmClusterJobResource();
        jbmClusterJobResource.setServiceId(serviceId);
        jbmClusterJobResource.setJbmClusterJobs(jbmClusterJobs);
        return jbmClusterJobResource;
    }


}
