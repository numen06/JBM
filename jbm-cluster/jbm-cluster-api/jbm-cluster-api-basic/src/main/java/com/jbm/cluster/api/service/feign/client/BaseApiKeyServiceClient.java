package com.jbm.cluster.api.service.feign.client;

import com.jbm.cluster.api.service.feign.IBaseApiKeyFeignClient;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@Component
@FeignClient(value = JbmClusterConstants.BASE_SERVER, path = "/apikey")
public interface BaseApiKeyServiceClient extends IBaseApiKeyFeignClient {
}
