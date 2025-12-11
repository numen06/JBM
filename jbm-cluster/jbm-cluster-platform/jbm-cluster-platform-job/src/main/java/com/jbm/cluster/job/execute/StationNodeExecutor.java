package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.constants.job.ProcessStatusEnum;
import com.jbm.cluster.api.model.job.rule.NodeData;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class StationNodeExecutor implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        try {
            Map<String, Object> nodeData = node.getData();
            Map<String, Object> outputData = new HashMap<>(inputData != null ? inputData : new HashMap<>());
            
            // 将 site 信息添加到输出
            if (nodeData != null && nodeData.containsKey("site")) {
                outputData.put("site", nodeData.get("site"));
            }
            
            // code 字段中配置的是代码字符串（需要转换成 JSON）
            String codeConfig = (String) nodeData.get("code");
            if (codeConfig != null && !codeConfig.trim().isEmpty()) {
                try {
                    // 将字符串代码转换成 JSON 配置
                    Map<String, Object> config = parseCodeStringToJson(codeConfig);
                    

                    boolean hasTriggeredData = inputData != null 
                        && inputData.get("currentNodeStatus") != null 
                        && inputData.get("currentNodeStatus").equals(ProcessStatusEnum.WAITING.getCode());

                    // 如果 config 中配置了 __WAIT_TRIGGER__ 且当前没有触发数据，说明是第一次进入，需要设置为等待
                    Boolean waitTrigger = (Boolean) config.get("__WAIT_TRIGGER__");
                    if (Boolean.TRUE.equals(waitTrigger) && !hasTriggeredData) {
                        // 第一次进入时，设置等待触发
                        outputData.put("__WAIT_TRIGGER__", true);
                        String triggerType = nodeData.getOrDefault("triggerType", "station_waiting").toString();
                        String triggerKey = nodeData.getOrDefault("triggerKey", node.getId()).toString();
                        return NodeExecutionResult.waiting(triggerType, triggerKey);
                    }
                    
                    // 触发继续执行或不需要等待触发时，执行完整的 code 逻辑
                    // ... existing code ...
                    if (config.containsKey("__HTTP_CALL__")) {
                        Map<String, Object> httpCall = (Map<String, Object>) config.get("__HTTP_CALL__");
                        // 替换参数中的占位符
                        httpCall = replaceParamsPlaceholders(httpCall, node, nodeData, inputData, config);
                        String method = (String) httpCall.get("method");
                        String url = (String) httpCall.get("url");
                        Map<String, Object> params = (Map<String, Object>) httpCall.get("params");
                        
                        if (method != null && url != null) {
                            Map<String, Object> httpResult = executeHttpCall(method, url, params);
                            outputData.putAll(httpResult);
                        }
                    }
                    
                    // 合并配置中的其他数据
                    if (config.containsKey("outputData")) {
                        Object output = config.get("outputData");
                        if (output instanceof Map) {
                            outputData.putAll((Map<String, Object>) output);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析 code 配置失败，继续返回默认数据：{}", e.getMessage());
                }
            }
            
            return NodeExecutionResult.success(outputData);
        } catch (Exception e) {
            log.error("站点节点执行失败", e);
            return NodeExecutionResult.error("站点节点执行失败: " + e.getMessage());
        }
    }
    
    private Map<String, Object> replaceParamsPlaceholders(Map<String, Object> httpCall, NodeData node, Map<String, Object> nodeData, Map<String, Object> inputData, Map<String, Object> parsedConfig) {
        // ... existing code ...
        Map<String, Object> result = new HashMap<>(httpCall);
        
        if (result.containsKey("params")) {
            Map<String, Object> params = (Map<String, Object>) result.get("params");
            if (params != null) {
                Map<String, Object> newParams = new HashMap<>(params);
                
                // 获取当前站点 ID
                String currentSiteId = null;
                if (nodeData != null && nodeData.containsKey("site")) {
                    Object site = nodeData.get("site");
                    if (site instanceof Map) {
                        currentSiteId = ((Map<String, Object>) site).get("siteCoordinateId").toString();
                    }
                }
                
                // 获取下一个站点 ID (从 inputData 中的 nextSite 或 nextSiteList)
                String nextSiteId = resolveNextSiteId(node, inputData, parsedConfig);
                log.info("Resolved nextSiteId: {}", nextSiteId);
                
                // 替换占位符
                for (Map.Entry<String, Object> entry : newParams.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if ("${currentSiteId}".equals(strValue) && currentSiteId != null) {
                            entry.setValue(currentSiteId);
                            log.info("Replace startLocation: {}", currentSiteId);
                        } else if ("${nextSiteId}".equals(strValue) && nextSiteId != null) {
                            entry.setValue(nextSiteId);
                            log.info("Replace endLocation: {}", nextSiteId);
                        }
                    }
                }
                
                result.put("params", newParams);
            }
        }
        
        return result;
    }
    
    /**
     * 根据条件判断和 nextSiteList 来解析下一个站点 ID
     * 如果配置了 __CONDITION__，则根据条件字段值从 nextSiteList 中选择
     * 条件配置示例：
     *   "__CONDITION__": {
     *     "checkField": "checkResult",
     *     "routes": [
     *       {"value": 1, "siteId": "1915633766512988168"},
     *       {"value": 0, "siteId": "1915633766512988169"}
     *     ]
     *   }
     */
    private String resolveNextSiteId(NodeData node, Map<String, Object> inputData, Map<String, Object> parsedConfig) {
        if (inputData == null) {
            return null;
        }
        
        // 首先检查是否有 nextSiteList（多个可能的下一站点）
        if (inputData.containsKey("nextSiteList")) {
            Object nextSiteListObj = inputData.get("nextSiteList");
            if (nextSiteListObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> nextSiteList = (java.util.List<Map<String, Object>>) nextSiteListObj;
                
                // 从 parsedConfig 中获取条件配置
                Map<String, Object> condition = (Map<String, Object>) parsedConfig.get("__CONDITION__");
                
                if (condition != null && condition.containsKey("checkField") && condition.containsKey("routes")) {
                    String checkField = condition.get("checkField").toString();
                    Object fieldValue = inputData.get(checkField);
                    log.info("Condition field '{}' value: {}", checkField, fieldValue);
                    
                    if (fieldValue != null) {
                        // 从 routes 中找匹配的路由
                        java.util.List<Map<String, Object>> routes = (java.util.List<Map<String, Object>>) condition.get("routes");
                        for (Map<String, Object> route : routes) {
                            Object routeValue = route.get("value");
                            if (routeValue != null && routeValue.toString().equals(fieldValue.toString())) {
                                String targetSiteId = route.get("siteCoordinateId").toString();
                                log.info("Condition matched: field '{}' = {}, selecting site {}", checkField, fieldValue, targetSiteId);
                                // 验证 targetSiteId 是否在 nextSiteList 中
                                for (Map<String, Object> site : nextSiteList) {
                                    if (targetSiteId.equals(site.get("siteCoordinateId").toString())) {
                                        return targetSiteId;
                                    }
                                }
                                log.warn("Target site {} not found in nextSiteList", targetSiteId);
                            }
                        }
                    }
                }
                
                // 如果没有配置条件或条件不匹配，返回第一个站点
                if (nextSiteList.size() > 0) {
                    return nextSiteList.get(0).get("siteCoordinateId").toString();
                }
            }
        }
        
        // 如果没有 nextSiteList，则使用 nextSite（普通情况）
        if (inputData.containsKey("nextSite")) {
            Object nextSite = inputData.get("nextSite");
            if (nextSite instanceof Map) {
                return ((Map<String, Object>) nextSite).get("siteCoordinateId").toString();
            }
        }
        
        return null;
    }
    
    private Map<String, Object> parseCodeStringToJson(String codeString) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 第一步：清理字符串 - 仅移除 var 关键字和换行符
            String jsonStr = codeString
                    .replaceAll("var\\s+", "")
                    .replaceAll("\\n", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            
            log.info("清理后的字符串：{}", jsonStr);
            log.info("字符串长度：{}", jsonStr.length());
            
            // 提取 __WAIT_TRIGGER__ 值
            int waitTriggerIdx = jsonStr.indexOf("__WAIT_TRIGGER__");
            if (waitTriggerIdx != -1) {
                // 找到 __WAIT_TRIGGER__ 后面的 =
                int equalsIdx = jsonStr.indexOf("=", waitTriggerIdx);
                log.info("waitTriggerIdx: {}, equalsIdx: {}", waitTriggerIdx, equalsIdx);
                
                if (equalsIdx != -1) {
                    int endIdx = -1;
                    // 找到最近的结束符号
                    int semicolonIdx = jsonStr.indexOf(";", equalsIdx);
                    int httpCallIdx = jsonStr.indexOf("httpCall", equalsIdx);
                    
                    // 找到最靠近的一个
                    if (semicolonIdx != -1) {
                        endIdx = semicolonIdx;
                    }
                    if (httpCallIdx != -1 && (endIdx == -1 || httpCallIdx < endIdx)) {
                        endIdx = httpCallIdx;
                    }
                    
                    log.info("semicolonIdx: {}, httpCallIdx: {}, endIdx: {}", semicolonIdx, httpCallIdx, endIdx);
                    
                    if (endIdx != -1) {
                        String valueStr = jsonStr.substring(equalsIdx + 1, endIdx).trim();
                        log.info("提取到的 __WAIT_TRIGGER__ 位置（原始）：{}", valueStr);
                        boolean boolValue = valueStr.equalsIgnoreCase("true");
                        log.info("提取到的 __WAIT_TRIGGER__ 值：{}", boolValue);
                        result.put("__WAIT_TRIGGER__", boolValue);
                    }
                }
            }
            
            // 提取 __HTTP_CALL__ 对象
            int httpCallIdx = jsonStr.indexOf("__HTTP_CALL__");
            log.info("httpCallIdx: {}", httpCallIdx);
            if (httpCallIdx != -1) {
                int equalsIdx = jsonStr.indexOf("=", httpCallIdx);
                
                int objectStartIdx = jsonStr.indexOf("{", equalsIdx);
                int objectEndIdx = findMatchingBrace(jsonStr, objectStartIdx);
                log.info("objectEndIdx: {}", objectEndIdx);
                
                if (objectEndIdx != -1) {
                    String httpCallJsonStr = jsonStr.substring(objectStartIdx, objectEndIdx + 1);
                    log.info("提取到的 httpCall 原始字符串：{}", httpCallJsonStr);
                    
                    // 转换为标准 JSON
                    httpCallJsonStr = httpCallJsonStr.replaceAll("([\\{,])\\s*(\\w+)\\s*:", "$1\"$2\":");
                    httpCallJsonStr = httpCallJsonStr.replaceAll(":\\s*'([^']*)'" , ": \"$1\"");
                    // 为没有引号的值添加引号（POST、GET 等）
                    httpCallJsonStr = httpCallJsonStr.replaceAll(":\\s*(POST|GET|PUT|DELETE|HEAD|OPTIONS)([,}])", ": \"$1\"$2");
                    
                    log.info("httpCall 转换后的 JSON：{}", httpCallJsonStr);
                    
                    try {
                        Map<String, Object> httpCall = com.alibaba.fastjson.JSON.parseObject(httpCallJsonStr, Map.class);
                        result.put("__HTTP_CALL__", httpCall);
                        log.info("httpCall 解析成功：{}", httpCall);
                    } catch (Exception e) {
                        log.error("解析 httpCall JSON 失败：{}", httpCallJsonStr, e);
                    }
                }
            }
            
            // 提取 __CONDITION__ 对象
            int conditionIdx = jsonStr.indexOf("__CONDITION__");
            log.info("conditionIdx: {}", conditionIdx);
            if (conditionIdx != -1) {
                int equalsIdx = jsonStr.indexOf("=", conditionIdx);
                
                int objectStartIdx = jsonStr.indexOf("{", equalsIdx);
                int objectEndIdx = findMatchingBrace(jsonStr, objectStartIdx);
                log.info("condition objectEndIdx: {}", objectEndIdx);
                
                if (objectEndIdx != -1) {
                    String conditionJsonStr = jsonStr.substring(objectStartIdx, objectEndIdx + 1);
                    log.info("提取到的 __CONDITION__ 原始字符串：{}", conditionJsonStr);
                    
                    // 转换为标准 JSON
                    conditionJsonStr = conditionJsonStr.replaceAll("([\\{,])\\s*(\\w+)\\s*:", "$1\"$2\":");
                    conditionJsonStr = conditionJsonStr.replaceAll(":\\s*'([^']*)'" , ": \"$1\"");
                    
                    log.info("condition 转换后的 JSON：{}", conditionJsonStr);
                    
                    try {
                        Map<String, Object> condition = com.alibaba.fastjson.JSON.parseObject(conditionJsonStr, Map.class);
                        result.put("__CONDITION__", condition);
                        log.info("__CONDITION__ 解析成功：{}", condition);
                    } catch (Exception e) {
                        log.error("解析 __CONDITION__ JSON 失败：{}", conditionJsonStr, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("转换代码字符串到 JSON 失败：{}", e.getMessage(), e);
            throw e;
        }
        
        return result;
    }
    
    private int findMatchingBrace(String str, int openIdx) {
        int count = 1;
        for (int i = openIdx + 1; i < str.length(); i++) {
            if (str.charAt(i) == '{') {
                count++;
            } else if (str.charAt(i) == '}') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private Map<String, Object> executeHttpCall(String method, String url, Map<String, Object> params) throws Exception {
        log.info("执行 HTTP 调用: {} {}", method, url);
        
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        // 添加 Authorization 请求头
        try {
            // token认证
            conn.setRequestProperty("Authorization", "Bearer " + SecurityUtils.getToken());
            log.info("已添加 Authorization 请求头");
        } catch (Exception e) {
            log.warn("获取 token 失败，将继续执行不带 Authorization 的请求: {}", e.getMessage());
        }
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            conn.setDoOutput(true);
            String json = com.alibaba.fastjson.JSON.toJSONString(params);
            log.info("HTTP 请求体: {}", json);
            conn.getOutputStream().write(json.getBytes("UTF-8"));
            conn.getOutputStream().flush();
        }
        
        int responseCode = conn.getResponseCode();
        log.info("HTTP 响应码: {}", responseCode);
        
        Map<String, Object> result = new HashMap<>();
        result.put("httpCode", responseCode);
        result.put("success", responseCode >= 200 && responseCode < 300);
        
        return result;
    }

    @Override
    public String getSupportedType() {
        return "station";
    }
}
