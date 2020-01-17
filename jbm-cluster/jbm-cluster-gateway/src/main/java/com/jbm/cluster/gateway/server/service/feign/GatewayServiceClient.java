package com.jbm.cluster.gateway.server.service.feign;

import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.service.IGatewayServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @author: liuyadu
 * @date: 2018/10/24 16:49
 * @description:
 */
@Component
@FeignClient(value = BaseConstants.BASE_SERVER)
public interface GatewayServiceClient extends IGatewayServiceClient {


}
