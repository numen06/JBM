package com.jbm.cluster.api.service.feign.client;

import com.jbm.cluster.api.service.IExtendFormDefinitionServiceClient;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@Component
@FeignClient(value = JbmClusterConstants.BASE_SERVER, path = "/extend-field/forms")
public interface ExtendFormDefinitionClient extends IExtendFormDefinitionServiceClient {
}
