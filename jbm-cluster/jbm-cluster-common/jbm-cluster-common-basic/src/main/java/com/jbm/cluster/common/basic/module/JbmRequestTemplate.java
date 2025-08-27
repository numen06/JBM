package com.jbm.cluster.common.basic.module;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.module.request.ICustomizeRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.net.UnknownHostException;
import java.util.Map;

@Slf4j
public class JbmRequestTemplate {

    @Autowired
    private ApplicationContext applicationContext;

    public okhttp3.Response request(String url, String methodType, String jsonBody) throws UnknownHostException {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        if (iCustomizeRequest != null) {
            return iCustomizeRequest.request(url, methodType, jsonBody);
        } else {
            throw new RuntimeException("未找到对应的请求方式");
        }
    }


    public okhttp3.Response request(Request.Builder requestBuilder) {
        Request httpRequest = requestBuilder.build();
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(httpRequest.url().toString());
        if (iCustomizeRequest != null) {
            return iCustomizeRequest.request(httpRequest);
        }else {
            throw new RuntimeException("未找到对应的请求方式");
        }
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
