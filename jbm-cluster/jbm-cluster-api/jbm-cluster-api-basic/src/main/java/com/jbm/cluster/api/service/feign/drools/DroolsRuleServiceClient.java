package com.jbm.cluster.api.service.feign.drools;

import cn.hutool.json.JSONObject;
import com.jbm.cluster.api.entitys.message.drools.DroolsFeignTemplate;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.cluster.core.constant.JbmClusterConstants;
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
@FeignClient(value = JbmClusterConstants.JOB_SERVER, path = "/droolsRule")
public interface DroolsRuleServiceClient {
    @PostMapping("/execute")
    DroolsFeignTemplate executeRule(@RequestBody DroolsFeignTemplate droolsFeignTemplate);
    @PostMapping("/parseAndExecuteRule")
    JSONObject parseAndExecuteRule(@RequestBody DroolsParseAndExecuteForm droolsParseAndExecuteForm);
    @PostMapping("/parseNextNode")
    JSONObject parseNextNode(@RequestBody DroolsParseAndExecuteForm droolsParseAndExecuteForm);
}
