package com.jbm.cluster.platform.gateway.filter.context;

import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 网关拦截内容传输
 *
 * @author wesley.zhang
 */
@Data
@ToString
public class GatewayContext {

    public static final String CACHE_GATEWAY_CONTEXT = "cacheGatewayContext";

    public static final String REQUEST_TIME_HEAD = "requestTime";
    /**
     * cache json body
     */
    private String requestBody;
    /**
     * cache Response Body
     */
    private Object responseBody;
    /**
     * request headers
     */
    private HttpHeaders requestHeaders;
    /**
     * cache form result
     */
    private MultiValueMap<String, String> formData;
    /**
     * cache all request result include:form result and query param
     */
    private MultiValueMap<String, String> allRequestData = new LinkedMultiValueMap<>(0);

}
