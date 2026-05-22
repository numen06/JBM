package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.model.RateLimitApi;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

public interface IGatewayFeignClient {
    @GetMapping("/api/blackList")
    List<IpLimitApi> getApiBlackList();
    @GetMapping("/api/whiteList")
    List<IpLimitApi> getApiWhiteList();
    @GetMapping("/api/rateLimit")
    List<RateLimitApi> getApiRateLimitList();
    @GetMapping("/api/route")
    List<GatewayRoute> getApiRouteList();
}