package com.jbm.cluster.api.service;

import cn.hutool.json.JSONObject;
import com.jbm.cluster.api.entitys.message.drools.DroolsFeignTemplate;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface IDroolsRuleServiceClient {
    @PostMapping("/execute")
    ResultBody<DroolsFeignTemplate> executeRule(@RequestBody DroolsFeignTemplate droolsFeignTemplate);
    @PostMapping("/parseAndExecuteRule")
    ResultBody<JSONObject> parseAndExecuteRule(@RequestBody DroolsParseAndExecuteForm form);
    @PostMapping("/parseNextNode")
    ResultBody<JSONObject> parseNextNode(@RequestBody DroolsParseAndExecuteForm form);
}