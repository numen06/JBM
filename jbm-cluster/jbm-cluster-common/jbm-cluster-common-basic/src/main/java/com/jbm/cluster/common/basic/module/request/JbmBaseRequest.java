package com.jbm.cluster.common.basic.module.request;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

@Slf4j
public abstract class JbmBaseRequest implements ICustomizeRequest {

    @Override
    public HttpResponse request(String url, String methodType, String jsonBody) {
        HttpRequest httpRequest = new HttpRequest(this.buildUrl(url));
        httpRequest.contentType(ContentType.JSON.getValue());
        httpRequest.setMethod(Method.GET);
        httpRequest.timeout(3000);
        httpRequest.body(jsonBody);
        httpRequest = this.buildRequest(httpRequest);
        if (HttpMethod.POST.matches(methodType)) {
            httpRequest.setMethod(Method.POST);
        }
        HttpResponse httpResponse = httpRequest.execute();
        log.info("执行URL状态为[{}],结果[{}]", httpResponse.getStatus(), httpResponse.body());
        return httpResponse;
    }

    public HttpRequest buildRequest(HttpRequest httpRequest) {
        return httpRequest;
    }

    public UrlBuilder buildUrl(String sourceUrl) {
        return UrlBuilder.of(sourceUrl);
    }


}
