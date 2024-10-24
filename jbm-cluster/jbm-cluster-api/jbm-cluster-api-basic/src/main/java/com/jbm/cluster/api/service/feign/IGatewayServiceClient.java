package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.gateway.GatewayRoute;
import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @author wesley.zhang
 */
public interface IGatewayServiceClient {

    /**
     * 获取接口黑名单列表
     *
     * @return
     */
    @GetMapping("/api/blackList")
    ResultBody<List<IpLimitApi>> getApiBlackList();

    /**
     * 获取接口白名单列表
     *
     * @return
     */
    @GetMapping("/api/whiteList")
    ResultBody<List<IpLimitApi>> getApiWhiteList();

    /**
     * 获取限流列表
     *
     * @return
     */
    @GetMapping("/api/rateLimit")
    ResultBody<List<RateLimitApi>> getApiRateLimitList();

    /**
     * 获取路由列表
     *
     * @return
     */
    @GetMapping("/api/route")
    ResultBody<List<GatewayRoute>> getApiRouteList();
}
