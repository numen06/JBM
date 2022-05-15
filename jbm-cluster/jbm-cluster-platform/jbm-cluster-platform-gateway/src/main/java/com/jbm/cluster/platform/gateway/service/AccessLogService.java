package com.jbm.cluster.platform.gateway.service;

import org.springframework.web.server.ServerWebExchange;

/**
 * @Created wesley.zhang
 * @Date 2022/5/10 19:16
 * @Description TODO
 */
public interface AccessLogService {
    void sendLog(ServerWebExchange exchange, Exception ex);

    void sendLog(ServerWebExchange exchange, String toString, Exception ex);
}
