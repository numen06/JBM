package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.util.BooleanUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.model.api.JbmApi;
import com.jbm.cluster.api.model.api.JbmApiResource;
import com.jbm.cluster.common.basic.configuration.apis.ApiBuild;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.configuration.resources.JbmClusterResourceScan;
import com.jbm.cluster.core.constant.QueueConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    public JbmApiResource scan(String serviceId) {
        // 使用父类共享的 mapping 资源，避免重复初始化
        if (mapping == null) {
            log.warn("RequestMappingHandlerMapping 未找到，无法扫描 API 资源");
            return new JbmApiResource();
        }
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        List<JbmApi> jbmApis = Lists.newArrayList();
        String[] permitAll = jbmClusterProperties.getPermitAll();
        for (Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            ApiBuild apiBuild = new ApiBuild(handlerMethodEntry.getKey(), handlerMethodEntry.getValue(), serviceId, permitAll);
            JbmApi jbmApi = apiBuild.doBuild();
            if (jbmApi != null) {
                jbmApis.add(jbmApi);
            }
        }
        JbmApiResource jbmApiResource = new JbmApiResource();
        jbmApiResource.setServiceId(serviceId);
        jbmApiResource.setJbmApiList(jbmApis);
        return jbmApiResource;
    }


}
