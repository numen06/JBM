package com.jbm.cluster.api.service.feign.drools;

import com.jbm.cluster.api.entitys.message.WeixinNotification;
import com.jbm.cluster.api.entitys.message.drools.DroolsFeignTemplate;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author scolin
 * @description
 * @date 2025/8/12 19:15
 */
public interface IDroolsRuleServiceClient {
    @PostMapping("/execute")
    ResultBody<DroolsFeignTemplate> executeRule(@RequestBody DroolsFeignTemplate droolsFeignTemplate);
}
