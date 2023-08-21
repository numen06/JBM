package com.jbm.cluster.common.basic.module;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.jbm.cluster.common.basic.module.request.ICustomizeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Slf4j
public class JbmRequestTemplate {

    @Autowired
    private ApplicationContext applicationContext;

    public HttpResponse request(String url, String methodType, String jsonBody) {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        return iCustomizeRequest.request(url, methodType, jsonBody);
    }


    public HttpRequest buildHttpRequest(String url, String methodType, String jsonBody) {
        HttpRequest httpRequest = new HttpRequest(url);
        httpRequest.body(jsonBody);
        httpRequest.setMethod(Method.GET);
        if (HttpMethod.POST.matches(methodType)) {
            httpRequest.setMethod(Method.POST);
        }
        return httpRequest;

    }

    public HttpResponse request(String url,HttpRequest httpRequest) {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        return iCustomizeRequest.request( httpRequest);
    }


    private ICustomizeRequest findCustomizeRequest(String url) {
        Map<String, ICustomizeRequest> iCustomizeRequestMap = applicationContext.getBeansOfType(ICustomizeRequest.class);
        if (MapUtil.isEmpty(iCustomizeRequestMap)) {
            return null;
        }
        String prefix = StrUtil.subBefore(url, "://", false);
        for (ICustomizeRequest iCustomizeRequest : iCustomizeRequestMap.values()) {
            if (iCustomizeRequest.prefix().equalsIgnoreCase(prefix)) {
                return iCustomizeRequest;
            }
        }
        return null;
    }


}
