package com.jbm.cluster.job.util;

import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

/**
 * 站点执行辅助工具类
 * 提供常用的站点操作函数，供code脚本使用
 * 
 * @author scolin
 */
@Slf4j
public class StationExecutorHelper {

    /**
     * HTTP 请求调用
     * 使用示例：callHttp('POST', 'http://api.example.com/send', {...params})
     */
    public static Map<String, Object> callHttp(String method, String url, Map<String, Object> params) {
        return callHttp(method, url, params, 5000);
    }

    public static Map<String, Object> callHttp(String method, String url, Map<String, Object> params, int timeout) {
        try {
            log.info("HTTP调用 - 方法: {}, URL: {}, 参数: {}", method, url, params);
            // 这里会在运行时被动态替换为实际的HTTP调用实现
            // 返回格式: {code: 200, data: {...}, message: ""}
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            return response;
        } catch (Exception e) {
            log.error("HTTP调用失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Drools 规则引擎调用
     * 使用示例：evaluateRule('quality_routing', {checkResult: 1, score: 85})
     */
    public static Map<String, Object> evaluateRule(String ruleName, Map<String, Object> context) {
        try {
            log.info("执行规则 - 规则名: {}, 上下文: {}", ruleName, context);
            // 这里会在运行时被动态替换为实际的Drools规则引擎调用
            // 返回格式: {nextStation: "QUALIFIED_ROUTE", appliedRule: "rule_name", ...}
            Map<String, Object> ruleResult = new HashMap<>();
            ruleResult.put("appliedRule", ruleName);
            return ruleResult;
        } catch (Exception e) {
            log.error("规则执行失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 获取前置站点数据
     * 使用示例：getPreviousData(inputParams)
     */
    public static Map<String, Object> getPreviousData(Map<String, Object> inputParams) {
        return (Map<String, Object>) inputParams.getOrDefault("previousData", new HashMap<>());
    }

    /**
     * 构建输出数据
     * 使用示例：buildOutput(status, nextRoute, data)
     */
    public static Map<String, Object> buildOutput(String status, String nextRoute, Map<String, Object> data) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", status);
        output.put("nextRoute", nextRoute);
        output.put("data", data);
        output.put("timestamp", System.currentTimeMillis());
        return output;
    }

    /**
     * 等待外部触发
     * 使用示例：waitForTrigger("MANUAL_INSPECTION")
     */
    public static void waitForTrigger(String reason) {
        log.info("设置等待触发: {}", reason);
        // 在脚本中设置 __WAIT_TRIGGER__ = true
    }

    /**
     * 日志打印
     * 使用示例：log("处理完成", data)
     */
    public static void log(String message, Object data) {
        log.info("[站点执行] {} - 数据: {}", message, data);
    }

    /**
     * 条件判断（简化分支逻辑）
     * 使用示例：decideRoute(checkResult, {"1": "QUALIFIED", "0": "REJECT"})
     */
    public static String decideRoute(Object value, Map<String, String> routeMap) {
        return routeMap.getOrDefault(String.valueOf(value), "UNKNOWN");
    }

    /**
     * 构建HTTP请求体
     * 使用示例：buildHttpBody("goodsId", siteId, "status", "ready")
     */
    public static Map<String, Object> buildHttpBody(Object... params) {
        Map<String, Object> body = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                body.put(String.valueOf(params[i]), params[i + 1]);
            }
        }
        return body;
    }

    /**
     * 检查响应是否成功
     * 使用示例：if (isSuccess(response)) { ... }
     */
    public static boolean isSuccess(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        Object code = response.get("code");
        return code != null && (code.equals(200) || code.equals("200"));
    }

    /**
     * 获取响应数据
     * 使用示例：Object result = getResponseData(response)
     */
    public static Object getResponseData(Map<String, Object> response) {
        return response != null ? response.get("data") : null;
    }

    /**
     * 获取错误消息
     * 使用示例：String error = getErrorMessage(response)
     */
    public static String getErrorMessage(Map<String, Object> response) {
        return response != null ? (String) response.getOrDefault("message", "Unknown error") : "Unknown error";
    }
}
