package com.jbm.cluster.platform.gateway.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author: wesley.zhang
 * @date: 2019/5/17 17:37
 * @description:
 */
public class ReactiveWebUtils {

    /**
     * 获取IP地址
     *
     * @param exchange
     * @return
     */
    public static String getRemoteAddress(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String, String> headers = request.getHeaders().toSingleValueMap();
        String unknown = "unknown";
        String forwarded = headers.get("X-Forwarded-For");
        String ip = null;
        if (StrUtil.isNotBlank(forwarded)) {
            String realIp = headers.get("X-Real-IP");
            if (StrUtil.isNotBlank(realIp) && realIp.equalsIgnoreCase(forwarded)) {
                ip = realIp;
            } else {
                ip = StrUtil.split(forwarded, ",").get(0);
            }
        }
//        String ip = headers.get("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = headers.get("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = headers.get("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = headers.get("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = headers.get("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = headers.get("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || unknown.equalsIgnoreCase(ip)) {
            ip = getRemoteAddress(request);
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 0) {
            String[] ips = ip.split(",");
            if (ips.length > 0) {
                ip = ips[0];
            }
        }
        return StrUtil.blankToDefault(ip, "0.0.0.0");
    }

    private static String getRemoteAddress(ServerHttpRequest request) {
        try {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress == null) {
                return null;
            }
            if (remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
            return remoteAddress.getHostString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
