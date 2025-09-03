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

    /**
     * 请求服务
     * @param url 示例：支持http和feign
     *            地址1：<a href="http://127.0.0.1:8080/api/test">...</a>
     *            地址2：feign://jbm-cluster-platform-center/api/test
     * @param methodType 请求方式
     * @param jsonBody 请求体
     * @return 请求结果
     * @throws UnknownHostException 找不到服务
     */
    public okhttp3.Response request(String url, String methodType, String jsonBody) throws UnknownHostException {
        ICustomizeRequest iCustomizeRequest = this.findCustomizeRequest(url);
        if (iCustomizeRequest != null) {
            return iCustomizeRequest.request(url, methodType, jsonBody);
        } else {
            throw new RuntimeException("未找到对应的请求方式");
        }
    }


    /**
     * 请求服务
     * @param requestBuilder 请求构造器
     * @return 请求结果
     */
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
