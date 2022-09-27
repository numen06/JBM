package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.util.BooleanUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.common.basic.configuration.apis.ApiBuild;
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
 * 自定义注解扫描
 *
 * @author wesley.zhang
 */
@Slf4j
public class JbmApiResourceScan extends JbmClusterResourceScan<JbmApiResource> {

    @Override
    public String queue() {
        return QueueConstants.API_RESOURCE_STREAM;
    }

    @Override
    public boolean enable(JbmClusterProperties jbmClusterProperties) {
        return BooleanUtil.isTrue(jbmClusterProperties.getApiRegister());
    }

    @Override
    public JbmApiResource scan() {
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
        JbmApiResource jbmApiResource = new JbmApiResource();
        jbmApiResource.setServiceId(serviceId);
        jbmApiResource.setJbmApiList(jbmApis);
        return jbmApiResource;
    }


}
