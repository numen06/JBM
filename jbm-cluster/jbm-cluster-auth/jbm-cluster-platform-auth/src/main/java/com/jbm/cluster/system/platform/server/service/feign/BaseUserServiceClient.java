package com.jbm.cluster.system.platform.server.service.feign;

import com.opencloud.base.client.service.IBaseUserServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

/**
 * @author: liuyadu
 * @date: 2018/10/24 16:49
 * @description:
 */
@Component
@FeignClient(value = BaseConstants.BASE_SERVER)
public interface BaseUserServiceClient extends IBaseUserServiceClient {


}
