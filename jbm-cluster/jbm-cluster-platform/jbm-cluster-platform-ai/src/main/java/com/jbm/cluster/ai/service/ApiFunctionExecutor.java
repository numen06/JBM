package com.jbm.cluster.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.ai.model.ApiMetadata;
import com.jbm.cluster.ai.model.ApiParameter;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * API 函数执行器
 * 
 * 架构设计：
 * 1. AI 通过 Function Calling 决定调用哪个接口和传递什么参数
 * 2. 本执行器接收 AI 的函数调用请求
 * 3. 通过 JbmRequestTemplate 执行实际的系统内部调用（不从外部访问）
 * 4. 返回 JSON 数据给 AI
 * 5. AI 负责解析数据并生成用户友好的回复
 * 
 * 关键点：
 * - AI 不直接访问系统，所有调用都通过 JbmRequestTemplate
 * - 使用 Feign 协议进行内部服务调用：feign://service-name/path
 * - 自动携带认证信息，符合 JBM 安全体系
 * 
 * @author wesley
 */
@Service
@Slf4j
public class ApiFunctionExecutor {

    @Autowired
    private JbmRequestTemplate jbmRequestTemplate;
    
    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    /**
     * 执行函数调用
     * 
     * 工作流程：
     * 1. AI 决定调用哪个函数（functionName）
     * 2. AI 提供调用参数（arguments）
     * 3. 本方法通过 JbmRequestTemplate 执行内部调用
     * 4. 返回原始 JSON 数据给 AI
     * 5. AI 自己解析并理解数据，生成回复
     * 
     * @param functionName 函数名（AI 决定）
     * @param arguments 参数（AI 提供）
     * @return 接口返回的原始 JSON 数据（供 AI 解析）
     */
    public String executeFunction(String functionName, Object arguments) {
        try {
            log.info("🎯 [AI请求] 执行函数: {}, 参数: {}", functionName, arguments);
            
            // 查找对应的 API 元数据
            ApiMetadata apiMetadata = findApiByFunctionName(functionName);
            if (apiMetadata == null) {
                log.warn("⚠️ 未找到函数 {} 对应的 API", functionName);
                return "{\"error\": \"Function not found: " + functionName + "\"}";
            }
            
            // 构建 Feign URL（内部调用协议）
            String url = apiMetadata.generateFeignUrl();  // feign://service-name/path
            String method = apiMetadata.getMethod();
            
            // 解析 AI 提供的参数
            Map<String, Object> params = parseArguments(arguments);
            
            // 构建完整 URL（包含路径参数和查询参数）
            url = buildUrl(url, apiMetadata, params);
            
            // 构建请求体（对于 POST/PUT）
            String requestBody = buildRequestBody(method, apiMetadata, params);
            
            log.info("📡 [内部调用] 通过 JbmRequestTemplate: {} {}", method, url);
            if (requestBody != null) {
                log.info("📦 [请求体]: {}", requestBody);
            }
            
            // 关键：通过 JbmRequestTemplate 执行内部调用（不从外部访问）
            Response response = jbmRequestTemplate.request(url, method, requestBody);
            
            // 解析响应 - 返回原始 JSON 给 AI 自己解析
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                log.info("✅ [内部调用成功] 返回数据给 AI 解析: {}", 
                        responseBody.length() > 200 ? 
                        responseBody.substring(0, 200) + "..." : responseBody);
                
                // 关键：返回原始 JSON 数据给 AI
                // AI 会自己解析这些数据并生成用户友好的回复
                return responseBody;
            } else {
                String errorMsg = "接口调用失败: HTTP " + response.code();
                log.error("❌ [内部调用失败] {}", errorMsg);
                return "{\"error\": \"" + errorMsg + "\"}";
            }
            
        } catch (Exception e) {
            log.error("❌ 执行函数失败: {}", functionName, e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 根据函数名查找 API 元数据
     */
    private ApiMetadata findApiByFunctionName(String functionName) {
        return apiMetadataCollector.getAllApis().stream()
                .filter(api -> api.generateFunctionName().equals(functionName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 解析参数
     */
    private Map<String, Object> parseArguments(Object arguments) {
        if (arguments == null) {
            return Map.of();
        }
        
        if (arguments instanceof Map) {
            return (Map<String, Object>) arguments;
        }
        
        if (arguments instanceof String) {
            String argStr = (String) arguments;
            if (StrUtil.isEmpty(argStr) || "{}".equals(argStr)) {
                return Map.of();
            }
            return JSONUtil.toBean(argStr, Map.class);
        }
        
        return Map.of();
    }
    
    /**
     * 构建完整 URL（包含路径参数和查询参数）
     */
    private String buildUrl(String baseUrl, ApiMetadata apiMetadata, Map<String, Object> params) {
        String url = baseUrl;
        StringBuilder queryParams = new StringBuilder();
        
        if (apiMetadata.getParameters() != null) {
            for (ApiParameter param : apiMetadata.getParameters()) {
                Object value = params.get(param.getName());
                if (value == null) {
                    continue;
                }
                
                // 处理路径参数
                if ("path".equals(param.getIn())) {
                    url = url.replace("{" + param.getName() + "}", String.valueOf(value));
                }
                
                // 处理查询参数
                if ("query".equals(param.getIn())) {
                    if (queryParams.length() > 0) {
                        queryParams.append("&");
                    }
                    queryParams.append(param.getName()).append("=").append(value);
                }
            }
        }
        
        // 添加查询参数
        if (queryParams.length() > 0) {
            url += (url.contains("?") ? "&" : "?") + queryParams.toString();
        }
        
        return url;
    }
    
    /**
     * 构建请求体（对于 POST/PUT/PATCH）
     */
    private String buildRequestBody(String method, ApiMetadata apiMetadata, Map<String, Object> params) {
        if (!"POST".equalsIgnoreCase(method) && 
            !"PUT".equalsIgnoreCase(method) && 
            !"PATCH".equalsIgnoreCase(method)) {
            return null;
        }
        
        // 查找 body 参数
        if (apiMetadata.getParameters() != null) {
            for (ApiParameter param : apiMetadata.getParameters()) {
                if ("body".equals(param.getIn())) {
                    Object bodyValue = params.get(param.getName());
                    if (bodyValue != null) {
                        if (bodyValue instanceof String) {
                            return (String) bodyValue;
                        }
                        return JSONUtil.toJsonStr(bodyValue);
                    }
                }
            }
        }
        
        // 如果没有明确的 body 参数，将所有非 path/query 参数作为 body
        Map<String, Object> bodyParams = params.entrySet().stream()
                .filter(entry -> {
                    if (apiMetadata.getParameters() == null) {
                        return true;
                    }
                    return apiMetadata.getParameters().stream()
                            .noneMatch(p -> p.getName().equals(entry.getKey()) && 
                                          ("path".equals(p.getIn()) || "query".equals(p.getIn())));
                })
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue
                ));
        
        return bodyParams.isEmpty() ? null : JSONUtil.toJsonStr(bodyParams);
    }
}

