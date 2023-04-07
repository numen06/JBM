package com.jbm.cluster.common.feign.request;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import com.jbm.cluster.common.basic.module.request.JbmBaseRequest;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.List;

@Slf4j
public class JbmFeignRequest extends JbmBaseRequest {

//    private SaOAuth2Template saOAuth2Template = new SaOAuth2Template() {
//        @Override
//        public SaClientModel getClientModel(String clientToken) {
//            if (SpringUtil.getApplicationName().equals(clientToken)) {
//                return new SaClientModel()
//                        .setClientId(SpringUtil.getApplicationName())
//                        .setClientSecret(SaIdUtil.getToken())
//                        .setAllowUrl("*")
//                        .setContractScope("*")
//                        .setIsAutoMode(true);
//            }
//            return null;
//        }
//    };


    @Override
    public UrlBuilder buildUrl(String sourceUrl) {
        String url = feignToUrl(sourceUrl);
        if (StrUtil.isEmpty(url)) {
            throw new RuntimeException("远程服务没有启动");
        }
        return UrlBuilder.of(url);
    }

    @Override
    public HttpRequest buildRequest(HttpRequest httpRequest) {
        SaOAuth2Template saOAuth2Template = SpringUtil.getBean(SaOAuth2Template.class);
        ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        httpRequest.header(JbmSecurityConstants.AUTHORIZATION_HEADER, SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken);
        return httpRequest;
    }

    @Override
    public String prefix() {
        return "feign";
    }


    public static String getServiceIdByUrl(String url) {
        String serviceId = ReUtil.get("(?<=://)[^//]*?/", url, 0);
        serviceId = StrUtil.removeSuffix(serviceId, "/");
        return serviceId;
    }

    public static String feignToUrl(String url) {
        String serviceId = getServiceIdByUrl(url);
        URI uri = getServiceUrl(serviceId);
        if (ObjectUtil.isEmpty(uri)) {
            return null;
        }
        String realUrl = uri.toString();
        return StrUtil.replace(url, "feign://" + serviceId, realUrl);
    }

    public static URI getServiceUrl(String serviceId) {
        DiscoveryClient discoveryClient = SpringUtil.getBean(DiscoveryClient.class);
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        if (CollUtil.isEmpty(serviceInstances)) {
            return null;
        }
        ServiceInstance serviceInstance = CollUtil.getFirst(serviceInstances);
        return serviceInstance.getUri();
    }

}
