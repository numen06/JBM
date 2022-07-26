package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.gateway.GatewayIpLimitApi;
import com.jbm.cluster.center.service.GatewayIpLimitApiService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:57:09
 */
@RestController
@RequestMapping("/gatewayIpLimitApi")
public class GatewayIpLimitApiController extends MasterDataCollection<GatewayIpLimitApi, GatewayIpLimitApiService> {
}