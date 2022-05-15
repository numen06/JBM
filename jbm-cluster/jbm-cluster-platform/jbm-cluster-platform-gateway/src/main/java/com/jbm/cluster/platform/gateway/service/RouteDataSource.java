package com.jbm.cluster.platform.gateway.service;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.RateLimitApi;

import java.util.List;

/**
 * @Created wesley.zhang
 * @Date 2022/5/10 19:15
 * @Description TODO
 */
public interface RouteDataSource {
    List<GatewayRoute> getRouteList();

    List<RateLimitApi> getLimitApiList();
}
