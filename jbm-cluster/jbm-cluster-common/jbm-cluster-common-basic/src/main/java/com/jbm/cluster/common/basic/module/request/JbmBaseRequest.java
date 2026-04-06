package com.jbm.cluster.common.basic.module.request;

import cn.hutool.core.net.url.UrlBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;


@Slf4j
public abstract class JbmBaseRequest implements ICustomizeRequest {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)  // 任务提交和查询都是轻量操作
            .callTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build();

    /**
     * 动态请求入口
     */
    @Override
    public okhttp3.Response request(String url, String methodType, String jsonBody) {
        // 1. 使用 buildUrl 解析并可能替换为真实地址
        UrlBuilder urlBuilder = null;
        try {
            urlBuilder = buildUrl(url);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        String finalUrl = urlBuilder.toString();

        // 2. 构建 OkHttp Request
        RequestBody body = null;
        if (POST.matches(methodType) || PUT.matches(methodType) || "PATCH".equalsIgnoreCase(methodType)) {
            MediaType mediaType = MediaType.get("application/json; charset=utf-8");
            body = RequestBody.create(jsonBody != null ? jsonBody : "{}", mediaType);
        }
        Request.Builder requestBuilder = new Request.Builder().url(finalUrl);
        requestBuilder.method(methodType, body);

        // 允许子类扩展
        requestBuilder = buildRequest(requestBuilder);

        Request request = requestBuilder.build();
        return executeRequest(request);
    }

    /**
     * 构建 URL
     */
    public UrlBuilder buildUrl(String sourceUrl) throws UnknownHostException {
        return UrlBuilder.of(sourceUrl);
    }

    /**
     * 执行 OkHttp 请求
     */
    private okhttp3.Response executeRequest(Request request) {
        try {
            okhttp3.Response response = httpClient.newCall(request).execute();
            String bodyString = readResponseBody(response);
            log.info("执行URL[{}]状态为[{}], 结果[{}]", request.url(), response.code(), bodyString);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    public static String readResponseBody(Response response) throws IOException {
        if (!response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                responseBody.close();
            }
            return null;
        }
        ResponseBody responseBody = response.body();
        if (responseBody == null) return null;
        // 可以多次读取 buffer 的克隆
        return response.peekBody(Integer.MAX_VALUE).string();
    }


    /**
     * 允许子类扩展 Request（如添加 header）
     */
    public Request.Builder buildRequest(Request.Builder requestBuilder) {
        return requestBuilder;
    }

    /**
     * 兼容旧接口（可选）
     */
    @Override
    public okhttp3.Response request(okhttp3.Request request) {
        return executeRequest(request);
    }
}