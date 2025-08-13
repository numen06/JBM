package com.jbm.cluster.api.service.feign.drools.client;

import com.jbm.cluster.api.service.feign.drools.IDroolsRuleServiceClient;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;


/**
 * @author scolin
 * @description
 * @date 2025/8/12 19:12
 */
@Component
@FeignClient(value = JbmClusterConstants.JOB_SERVER, path = "/droolsRule")
public interface DroolsRuleServiceClient extends IDroolsRuleServiceClient {
}
