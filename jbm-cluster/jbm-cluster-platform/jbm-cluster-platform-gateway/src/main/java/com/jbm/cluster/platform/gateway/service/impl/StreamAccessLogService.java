package com.jbm.cluster.platform.gateway.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.platform.gateway.filter.context.GatewayContext;
import com.jbm.cluster.platform.gateway.logfilter.AccessLogFilter;
import com.jbm.cluster.platform.gateway.service.AccessLogService;
import com.jbm.cluster.platform.gateway.utils.ReactiveWebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * 将访问日志通过流的模式传输到MQ进行存储
 *
 * @author: wesley.zhang
 * @date: 2019/5/8 11:27
 * @description:
 */
@Slf4j
@Component
public class StreamAccessLogService implements AccessLogService {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();


    /**
     * 忽略的路径
     */
    @JsonIgnore
    private Set<String> ignores = Sets.newHashSet(
            "/**/oauth/check_token/**",
            "/**/GatewayLogs/**",
            "/**/v2/api-docs/**",
            "/webjars/**",
            "/auth/captcha/**",
            "/auth/oauth2/token"
    );
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private ApplicationContext applicationContext;

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
    public void sendLog(ServerWebExchange exchange, Throwable ex) {
        this.sendLog(exchange, null, ex);
    }

    @Override
    public void sendLog(ServerWebExchange exchange, String responseBody, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        try {
            //获取访问地址
            String requestPath = request.getURI().getPath();
            //是否是忽略地址
            if (ignore(requestPath)) {
                return;
            }
            //获取路由
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            //获取HTTP状态
            int httpStatus = response.getStatusCode().value();
            //获取访问方法
            String method = request.getMethodValue();
            Map<String, String> headers = request.getHeaders().toSingleValueMap();
            Map data = Maps.newHashMap();
            GatewayContext gatewayContext = exchange.getAttribute(GatewayContext.CACHE_GATEWAY_CONTEXT);
            if (gatewayContext != null) {
                data = gatewayContext.getAllRequestData().toSingleValueMap();
            }
            //获取服务号
            String serviceId = null;
            if (route != null) {
                serviceId = route.getUri().getAuthority();
            }
            //获取IP地址
            String ip = ReactiveWebUtils.getRemoteAddress(exchange);
            //获取终端信息
            String userAgent = headers.get(HttpHeaders.USER_AGENT);
            //请求时间
            String requestTime = exchange.getAttribute(GatewayContext.REQUEST_TIME_HEAD);
            if (StrUtil.isBlank(requestTime)) {
                requestTime = DateUtil.now();
            }
            String error = null;
            if (ex != null) {
                error = ex.getMessage();
            }
            GatewayLogInfo gatewayLogs = new GatewayLogInfo();
            gatewayLogs.setLoglevel(1);
            gatewayLogs.setServiceId(StrUtil.isBlank(serviceId) ? SpringUtil.getApplicationName() : serviceId);
            gatewayLogs.setRequestTime(DateUtil.parseDateTime(requestTime));
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
            //计算耗用时间秒
            Long userTime = DateUtil.between(gatewayLogs.getResponseTime(), gatewayLogs.getRequestTime(), DateUnit.SECOND);
            gatewayLogs.setUseTime(userTime);
            Map<String, AccessLogFilter> accessLogFilterMap = applicationContext.getBeansOfType(AccessLogFilter.class);

            //异步处理所有数据
            accessLogFilterMap.values().parallelStream().forEach(new Consumer<AccessLogFilter>() {
                @Override
                public void accept(AccessLogFilter accessLogFilter) {
                    try {
                        accessLogFilter.filter(gatewayLogs, headers);
                    } catch (Exception e) {
                        log.error("日志过滤器[{}]失败", accessLogFilter.getClass(), e);
                    }
                }
            });
            //大于0代表记录日志
            if (gatewayLogs.getLoglevel() >= 0) {
                //发送数据
                streamBridge.send(QueueConstants.ACCESS_LOGS_STREAM, JSON.toJSONString(gatewayLogs));
            }

        } catch (Exception e) {
            log.error("access logs save error:{}", e);
        }
    }


}
