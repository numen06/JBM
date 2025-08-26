package com.jbm.cluster.job.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.jbm.cluster.api.entitys.job.DroolsRule;
import com.jbm.cluster.api.form.job.DroolsParseAndExecuteForm;
import com.jbm.framework.masterdata.service.IMasterDataService;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-08-12 14:03:24
 */
public interface DroolsRuleService extends IMasterDataService<DroolsRule> {
    /**
     * 保存规则
     *
     * @param droolsRule
     * @return
     */
    DroolsRule saveData(DroolsRule droolsRule);

    /**
     * 升版
     *
     * @param droolsRule
     * @return
     */

    DroolsRule updateVersion(DroolsRule droolsRule);

    /**
     * 前端传入的原始json解析drool内容
     *
     * @param originalJson
     * @return
     */

    JSONArray compileRule(String originalJson, String nodeId);

    /**
     * 执行规则
     *
     * @param droolsParseAndExecuteForm
     * @return
     */
    JSONObject parseAndExecuteRule(DroolsParseAndExecuteForm droolsParseAndExecuteForm);

}
