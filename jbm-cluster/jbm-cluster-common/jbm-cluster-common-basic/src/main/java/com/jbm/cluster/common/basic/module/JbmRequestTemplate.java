package com.jbm.cluster.common.basic.module;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.jbm.cluster.common.basic.module.request.ICustomizeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@Slf4j
public class JbmRequestTemplate {

    @Autowired
    private ApplicationContext applicationContext;

    public HttpResponse request(String url, String methodType, String jsonBody) {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        return iCustomizeRequest.request(url, methodType, jsonBody);
    }

    public HttpResponse request(String url,HttpRequest httpRequest) {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        return iCustomizeRequest.request(httpRequest);
    }


    public ICustomizeRequest findCustomizeRequest(String url) {
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
