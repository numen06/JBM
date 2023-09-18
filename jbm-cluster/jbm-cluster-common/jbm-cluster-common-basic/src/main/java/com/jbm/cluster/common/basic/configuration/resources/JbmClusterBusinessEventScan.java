package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.event.annotation.BusinessEvent;
import com.jbm.cluster.api.event.annotation.BusinessEventListener;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventBean;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.core.constant.QueueConstants;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
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
public class JbmClusterBusinessEventScan extends JbmClusterResourceScan<JbmClusterBusinessEventResource> {


    public JbmClusterBusinessEventScan() {
    }


    @Override
    public String queue() {
        return QueueConstants.BUSINESS_EVENT_RESOURCE_STREAM;
    }

    @Override
    public boolean enable(JbmClusterProperties jbmClusterProperties) {
        return true;
    }

//    private Map<String, JbmClusterBusinessEventBean> jbmClusterBusinessEventBeanMap = new ConcurrentHashMap<>();


    @Override
    public JbmClusterBusinessEventResource scan() {
        // 服务名称
        String serviceId = SpringContextHolder.geteApplicationName();
        // 所有接口映射
        final RequestMappingHandlerMapping mapping = SpringContextHolder.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        List<JbmClusterBusinessEventBean> jbmClusterBusinessEventBeans = Lists.newArrayList();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            if (handlerMethodEntry.getValue().hasMethodAnnotation(BusinessEventListener.class)) {
                if (CollUtil.contains(handlerMethodEntry.getKey().getMethodsCondition().getMethods(), RequestMethod.POST)) {
                    //参数方法不是一个说明方法错误
//                    if (handlerMethodEntry.getValue().getMethodParameters().length == 1) {
//                        log.error("监听业务事件方法必须为一个参数");
//                        continue;
//                    }
                } else if (CollUtil.contains(handlerMethodEntry.getKey().getMethodsCondition().getMethods(), RequestMethod.GET)) {

                } else {
                    //没有注册请求方法无效
                    log.error("监听业务事件方法必须为POST或GET");
                    continue;
                }
                BusinessEventListener businessEventListener = handlerMethodEntry.getValue().getMethodAnnotation(BusinessEventListener.class);
                BusinessEvent businessEvent = AnnotationUtil.getAnnotation(businessEventListener.eventClass(), BusinessEvent.class);
                if (ObjectUtil.isEmpty(businessEvent)) {
                    log.error("业务事件监听的类没有[BusinessEvent]注解");
                    continue;
                }
                final String path = CollUtil.getFirst(handlerMethodEntry.getKey().getPatternsCondition().getPatterns());
                JbmClusterBusinessEventBean jbmClusterBusinessEventBean = new JbmClusterBusinessEventBean();
                jbmClusterBusinessEventBean.setEventName(businessEvent.name());
                jbmClusterBusinessEventBean.setGlobal(businessEvent.global());
                jbmClusterBusinessEventBean.setEventCode(ClassUtil.getClassName(businessEventListener.eventClass(), false));
                //设定的监听分组
                UrlBuilder eventGroupBuilder = UrlBuilder.of().addPath(businessEventListener.eventGroup()).addPath(path);
                jbmClusterBusinessEventBean.setEventGroup(eventGroupBuilder.getPathStr());
                jbmClusterBusinessEventBean.setServiceName(serviceId);
                if (StrUtil.isNotBlank(businessEvent.url())) {
                    jbmClusterBusinessEventBean.setUrl(businessEvent.url());
                } else {
                    String url = UrlBuilder.of().setHost(serviceId).addPath(path).build();
                    jbmClusterBusinessEventBean.setUrl(StrUtil.replace(url, UrlBuilder.of().getSchemeWithDefault(), "feign"));
                }
                jbmClusterBusinessEventBean.setMethodType(CollUtil.getFirst(handlerMethodEntry.getKey().getMethodsCondition().getMethods()).toString());
                jbmClusterBusinessEventBean.setContentType(ContentType.JSON.getValue());
                jbmClusterBusinessEventBeans.add(jbmClusterBusinessEventBean);
            }
        }
        JbmClusterBusinessEventResource jbmClusterBusinessEventResource = new JbmClusterBusinessEventResource();
        jbmClusterBusinessEventResource.setServiceId(serviceId);
        jbmClusterBusinessEventResource.setJbmClusterBusinessEventBeans(jbmClusterBusinessEventBeans);
        //放入缓存
//        jbmClusterBusinessEventBeans.forEach(jbmClusterBusinessEventBean -> {
//            jbmClusterBusinessEventBeanMap.put(jbmClusterBusinessEventBean.getEventCode(), jbmClusterBusinessEventBean);
//        });
        return jbmClusterBusinessEventResource;
    }


}
