package com.jbm.cluster.platform.gateway.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import com.jbm.cluster.platform.gateway.service.AccessLogService;
import com.jbm.cluster.platform.gateway.utils.ReactiveWebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author: wesley.zhang
 * @date: 2019/5/8 11:27
 * @description:
 */
@Slf4j
@Component
public class StreamAccessLogService implements AccessLogService {

    @Value("${spring.application.name}")
    private String defaultServiceId;

//    @Autowired
//    private AmqpTemplate amqpTemplate;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @JsonIgnore
    private Set<String> ignores = new HashSet<>(Arrays.asList(new String[]{
            "/**/oauth/check_token/**",
            "/**/gateway/access/logs/**",
            "/**/v2/api-docs/**",
            "/webjars/**"
    }));

    /**
     * 不记录日志
     *
     * @param requestPath
     * @return
     */
    public boolean ignore(String requestPath) {
        Iterator<String> iterator = ignores.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            if (antPathMatcher.match(path, requestPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendLog(ServerWebExchange exchange, Exception ex) {
        this.sendLog(exchange, null, ex);
    }

    @Override
    public void sendLog(ServerWebExchange exchange, String responseBody, Exception ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        try {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            int httpStatus = response.getStatusCode().value();
            String requestPath = request.getURI().getPath();
            String method = request.getMethodValue();
            Map<String, String> headers = request.getHeaders().toSingleValueMap();
            Map data = Maps.newHashMap();
            GatewayContext gatewayContext = exchange.getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
            if (gatewayContext != null) {
                data = gatewayContext.getAllRequestData().toSingleValueMap();
            }
            String serviceId = null;
            if (route != null) {
                serviceId = route.getUri().toString().replace("lb://", "");
            }
            String ip = ReactiveWebUtils.getRemoteAddress(exchange);
            String userAgent = headers.get(HttpHeaders.USER_AGENT);
            Object requestTime = exchange.getAttribute(GatewayContext.REQUEST_TIME_HEAD);
            String error = null;
            if (ex != null) {
                error = ex.getMessage();
            }
            if (ignore(requestPath)) {
                return;
            }
            GatewayLogInfo gatewayLogs = new GatewayLogInfo();
            gatewayLogs.setServiceId(serviceId == null ? defaultServiceId : serviceId);
            gatewayLogs.setRequestTime(DateUtil.parseDateTime(requestTime.toString()));
            gatewayLogs.setHttpStatus(httpStatus);
            gatewayLogs.setHeaders(JSONObject.toJSONString(headers));
            gatewayLogs.setPath(requestPath);
            gatewayLogs.setParams(JSONObject.toJSONString(data));
            gatewayLogs.setIp(ip);
            gatewayLogs.setMethod(method);
            gatewayLogs.setUserAgent(userAgent);
            gatewayLogs.setResponseTime(DateTime.now());
            gatewayLogs.setError(error);
            gatewayLogs.setResponseBody(responseBody);
            gatewayLogs.setUseTime(DateUtil.between(gatewayLogs.getResponseTime(), gatewayLogs.getRequestTime(), DateUnit.SECOND));
//            Mono<Authentication> authenticationMono = exchange.getPrincipal();
//            Mono<JbmLoginUser> authentication = authenticationMono
//                    .map(Authentication::getPrincipal)
//                    .cast(OpenUserDetails.class);
//            authentication.subscribe(new Consumer<OpenUserDetails>() {
//                @Override
//                public void accept(OpenUserDetails user) {
//                    user.getAuthorities().clear();
//                    gatewayLogs.setAuthentication(JSONObject.toJSONString(user));
//                }
//            });
            //发送数据
            log.info("假装发送了数据:{}", JSON.toJSONString(gatewayLogs));
//            amqpTemplate.convertAndSend(QueueConstants.QUEUE_ACCESS_LOGS, JSON.toJSONString(gatewayLogs));
        } catch (Exception e) {
            log.error("access logs save error:{}", e);
        }

    }


}
