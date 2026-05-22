package com.jbm.cluster.common.feign.request;

import cn.dev33.satoken.id.SaIdUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.common.basic.module.request.JbmBaseRequest;
import com.jbm.cluster.common.feign.AppPreRequestInterceptor;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request.Builder;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.net.URI;
import java.net.UnknownHostException;

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
    public Builder buildRequest(Builder httpRequest) {
        httpRequest.header(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
        httpRequest.header(JbmSecurityConstants.INTERNAL_SERVICE, SpringUtil.getApplicationName());
        httpRequest.header(JbmSecurityConstants.INTERNAL_INSTANCE,
                SpringUtil.getApplicationName() + ":" + SpringUtil.getProperty("server.port", "0"));
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
        URI uri = getServiceUrl(serviceId);
        if (ObjectUtil.isEmpty(uri)) {
            throw new UnknownHostException(serviceId + "服务没有启动");
        }
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
