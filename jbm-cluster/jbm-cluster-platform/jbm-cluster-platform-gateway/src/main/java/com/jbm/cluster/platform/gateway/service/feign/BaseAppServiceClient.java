package com.jbm.cluster.platform.gateway.service.feign;

import com.jbm.cluster.api.service.IBaseAppServiceClient;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @author: wesley.zhang
 * @date: 2018/10/24 16:49
 * @description:
 */
@Component
@FeignClient(value = JbmClusterConstants.BASE_SERVER)
public interface BaseAppServiceClient extends IBaseAppServiceClient {

}
