package com.jbm.cluster.api.service.feign.drools;

import com.jbm.cluster.api.entitys.message.drools.DroolsFeignTemplate;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * @author scolin
 * @description
 * @date 2025/8/12 19:12
 */
@Component
@FeignClient(value = JbmClusterConstants.JOB_SERVER + "-scolin", path = "/droolsRule")
public interface DroolsRuleServiceClient {
    @PostMapping("/execute")
    ResultBody<DroolsFeignTemplate> executeRule(@RequestBody DroolsFeignTemplate droolsFeignTemplate);
}
