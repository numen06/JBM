package com.jbm.cluster.job.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * 站点Code代码生成器
 * 用于动态生成标准化的站点执行代码
 * 
 * 使用示例：
 * String code = StationCodeBuilder.create()
 *     .waitIfNoTrigger("triggerData")
 *     .callHttp("POST", "http://api/send", new HashMap<>())
 *     .applyRule("quality_routing")
 *     .build();
 *
 * @author scolin
 */
public class StationCodeBuilder {
    private StringBuilder code = new StringBuilder();
    private boolean hasWaitLogic = false;
    private boolean hasHttpLogic = false;
    private boolean hasRuleLogic = false;

    public static StationCodeBuilder create() {
        return new StationCodeBuilder();
    }

    /**
     * 添加获取前置数据的代码
     */
    public StationCodeBuilder withPreviousData() {
        code.append("var previousData = getPreviousData(inputParams);\n");
        code.append("log('获取前置数据', previousData);\n\n");
        return this;
    }

    /**
     * 添加等待触发的逻辑
     */
    public StationCodeBuilder waitIfNoTrigger(String triggerField) {
        if (hasWaitLogic) return this;
        hasWaitLogic = true;
        
        code.append("var triggerData = inputParams.").append(triggerField).append(";\n");
        code.append("if (triggerData === undefined) {\n");
        code.append("  __WAIT_TRIGGER__ = true;\n");
        code.append("  outputData = buildOutput('waiting', 'hold', {station: site.siteCode});\n");
        code.append("} else {\n");
        code.append("  // 已有触发数据，继续处理\n");
        code.append("  log('收到触发数据', triggerData);\n");
        code.append("  // 后续逻辑在此添加\n");
        code.append("}\n");
        return this;
    }

    /**
     * 添加简单的等待逻辑（自动判断）
     */
    public StationCodeBuilder waitForTrigger() {
        return waitIfNoTrigger("triggerData");
    }

    /**
     * 添加HTTP请求代码
     */
    public StationCodeBuilder callHttp(String method, String url, Map<String, Object> bodyParams) {
        if (hasHttpLogic) return this;
        hasHttpLogic = true;
        
        code.append("var httpBody = buildHttpBody(\n");
        int index = 0;
        for (Map.Entry<String, Object> entry : bodyParams.entrySet()) {
            if (index > 0) code.append(",\n");
            code.append("  '").append(entry.getKey()).append("', ").append(entry.getValue());
            index++;
        }
        code.append("\n);\n");
        code.append("var httpResponse = callHttp('").append(method).append("', '").append(url).append("', httpBody);\n");
        code.append("if (isSuccess(httpResponse)) {\n");
        code.append("  log('HTTP请求成功', getResponseData(httpResponse));\n");
        code.append("} else {\n");
        code.append("  log('HTTP请求失败', getErrorMessage(httpResponse));\n");
        code.append("}\n\n");
        return this;
    }

    /**
     * 添加Drools规则调用代码
     */
    public StationCodeBuilder applyRule(String ruleName) {
        if (hasRuleLogic) return this;
        hasRuleLogic = true;
        
        code.append("var ruleContext = {\n");
        code.append("  checkResult: inputParams.checkResult,\n");
        code.append("  siteType: site.siteType,\n");
        code.append("  siteCode: site.siteCode\n");
        code.append("};\n");
        code.append("var ruleResult = evaluateRule('").append(ruleName).append("', ruleContext);\n");
        code.append("var nextRoute = ruleResult.nextStation || 'UNKNOWN';\n\n");
        return this;
    }

    /**
     * 添加简单的条件判断代码
     */
    public StationCodeBuilder decideByCondition(String conditionField, String passRoute, String failRoute) {
        code.append("var condition = inputParams.").append(conditionField).append(";\n");
        code.append("var nextRoute = (condition === 1 || condition === true) ? '").append(passRoute).append("' : '").append(failRoute).append("';\n");
        code.append("var status = (nextRoute === '").append(passRoute).append("') ? 'passed' : 'failed';\n\n");
        return this;
    }

    /**
     * 添加输出代码
     */
    public StationCodeBuilder withOutput(String status, String route) {
        code.append("outputData = buildOutput('").append(status).append("', '").append(route).append("', {\n");
        code.append("  station: site.siteCode,\n");
        code.append("  timestamp: new Date().getTime()\n");
        code.append("});\n");
        return this;
    }

    /**
     * 添加自定义代码
     */
    public StationCodeBuilder custom(String customCode) {
        code.append(customCode).append("\n");
        return this;
    }

    /**
     * 添加日志记录
     */
    public StationCodeBuilder logInfo(String message) {
        code.append("log('").append(message).append("', {station: site.siteCode});\n");
        return this;
    }

    /**
     * 生成最终的代码字符串
     */
    public String build() {
        // 如果没有输出代码，添加默认输出
        if (!code.toString().contains("outputData")) {
            code.append("outputData = buildOutput('completed', 'next', {station: site.siteCode});\n");
        }
        return code.toString();
    }

    /**
     * 生成带注释的代码
     */
    public String buildWithComment(String stationName, String description) {
        StringBuilder result = new StringBuilder();
        result.append("/**\n");
        result.append(" * 站点: ").append(stationName).append("\n");
        result.append(" * 描述: ").append(description).append("\n");
        result.append(" */\n\n");
        result.append(code.toString());
        return result.toString();
    }
}
