package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimitApi;
import com.jbm.cluster.common.mysql.service.GatewayRateLimitApiService;
import com.jbm.framework.mvc.web.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:57:09
 */
@RestController
@RequestMapping("/gatewayRateLimitApi")
public class GatewayRateLimitApiController extends BaseController {
}