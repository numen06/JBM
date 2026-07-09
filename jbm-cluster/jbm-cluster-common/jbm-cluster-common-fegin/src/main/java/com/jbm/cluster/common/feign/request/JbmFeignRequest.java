package com.jbm.cluster.common.feign.request;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.common.basic.module.request.JbmBaseRequest;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author wesley
 */
@Slf4j
public class JbmFeignRequest extends JbmBaseRequest {

    @Override
    public UrlBuilder buildUrl(String sourceUrl) throws UnknownHostException {
        String url = feignToUrl(sourceUrl);
        if (StrUtil.isEmpty(url)) {
            throw new UnknownHostException("远程服务没有启动");
        }
        return UrlBuilder.of(url);
    }

    @Override
    public Request.Builder buildRequest(Request.Builder httpRequest) {
        SaOAuth2Template saOAuth2Template = SpringUtil.getBean(SaOAuth2Template.class);
        ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
        log.debug("[互信诊断] JbmFeignRequest.buildRequest 生成ClientToken: clientId={}, clientToken={}, tokenPrefix={}",
                clientTokenModel.clientId, clientTokenModel.clientToken, SaManager.getConfig().getTokenPrefix());
        final String authorization = SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken;
        log.debug("[互信诊断] JbmFeignRequest.buildRequest 注入Authorization header: {}", authorization.substring(0, Math.min(authorization.length(), 30)) + "...");
        httpRequest.header(JbmSecurityConstants.AUTHORIZATION_HEADER, authorization);
        httpRequest.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        httpRequest.header(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER);
        return httpRequest;
    }

    @Override
    public String prefix() {
        return "feign";
    }


    public String getServiceIdByUrl(String url) {
        String serviceId = ReUtil.get("(?<=://)[^//]*?/", url, 0);
        serviceId = StrUtil.removeSuffix(serviceId, "/");
        return serviceId;
    }

    public String feignToUrl(String url) throws UnknownHostException {
        String serviceId = getServiceIdByUrl(url);
        //得到服务的真实地址127.0.0.1:8080
        URI uri = getServiceUrl(serviceId);
        if (ObjectUtil.isEmpty(uri)) {
            throw new UnknownHostException(serviceId + "服务没有启动");
        }
        //将feign://替换成为真实URL
        String realUrl = uri.toString();
        return StrUtil.replace(url, "feign://" + serviceId, realUrl);
    }

    public URI getServiceUrl(String serviceId) {
        LoadBalancerClient loadBalancer = SpringUtil.getBean(LoadBalancerClient.class);
        if (loadBalancer == null) {
            throw new RuntimeException("Spring LoadBalancerClient not found");
        }
        return loadBalancer.choose(serviceId).getUri();
    }

}
