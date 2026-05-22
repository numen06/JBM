package com.jbm.cluster.api.service.feign.client;

import com.jbm.cluster.api.service.feign.IBaseAreaFeignClient;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@Component
@FeignClient(value = JbmClusterConstants.BASE_SERVER, path = "/baseArea")
public interface BaseAreaServiceClient extends IBaseAreaFeignClient {
}