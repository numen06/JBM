package com.jbm.cluster.ai.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.ai.model.ApiMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 元函数服务
 * 
 * 提供少量高级函数，让 AI 能够搜索、发现和调用实际的业务 API
 * 
 * 优势：
 * 1. 减少 Token 消耗 - 只发送 4 个元函数定义
 * 2. 提高准确性 - AI 先搜索再执行，更精准
 * 3. 加快响应 - 减少推理时间
 * 4. 降低成本 - 大幅减少 token 使用
 * 
 * @author wesley
 */
@Service
@Slf4j
public class AgentFunctionService {
    
    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    @Autowired
    private ApiFunctionRegistry apiFunctionRegistry;
    
    /**
     * 测试函数：获取当前时间
     * 
     * 用于验证 Function Calling 机制是否正常工作
     * 
     * @param params 参数（可为空）
     * @return 当前时间信息
     */
    public String getCurrentTime(JSONObject params) {
        log.info("⏰ [测试函数] 获取当前时间");
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentTime", now.format(formatter));
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "这是一个测试函数，用于验证 Function Calling 是否正常工作");
        
        return JSONUtil.toJsonStr(result);
    }
    
    /**
     * 元函数 1: 搜索 API
     * 
     * 根据关键词或描述搜索相关的 API
     * 
     * @param params 参数: {query: "关键词", limit: 10}
     * @return 匹配的 API 列表
     */
    public String searchApis(JSONObject params) {
        log.info("🔍 [Agent] 搜索 API: {}", params);
        
        String query = params.getStr("query", "");
        int limit = params.getInt("limit", 10);
        
        if (query.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "请提供搜索关键词");
            errorResult.put("apis", Collections.emptyList());
            return JSONUtil.toJsonStr(errorResult);
        }
        
        // 获取所有 API
        List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
        
        // 模糊搜索：匹配 API 名称、路径、描述
        List<Map<String, Object>> results = allApis.stream()
                .filter(api -> {
                    String searchText = (api.getSummary() + " " + 
                                       api.getPath() + " " +
                                       api.getServiceName()).toLowerCase();
                    return searchText.contains(query.toLowerCase());
                })
                .limit(limit)
                .map(api -> {
                    Map<String, Object> apiMap = new HashMap<>();
                    apiMap.put("apiId", api.getServiceName() + "_" + api.getPath().hashCode());
                    apiMap.put("name", api.getSummary());
                    apiMap.put("path", api.getPath());
                    apiMap.put("method", api.getMethod());
                    apiMap.put("service", api.getServiceName());
                    apiMap.put("description", api.getSummary());
                    return apiMap;
                })
                .collect(Collectors.toList());
        
        log.info("✅ [Agent] 找到 {} 个匹配的 API", results.size());
        
        Map<String, Object> successResult = new HashMap<>();
        successResult.put("success", true);
        successResult.put("count", results.size());
        successResult.put("apis", results);
        return JSONUtil.toJsonStr(successResult);
    }
    
    /**
     * 元函数 2: 列出 API 分类
     * 
     * 按服务分组展示所有 API
     * 
     * @param params 参数: {}
     * @return API 分类列表
     */
    public String listApiCategories(JSONObject params) {
        log.info("📋 [Agent] 列出 API 分类");
        
        List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
        
        // 按服务分组
        Map<String, List<Map<String, Object>>> categories = allApis.stream()
                .collect(Collectors.groupingBy(
                        ApiMetadata::getServiceName,
                        Collectors.mapping(api -> {
                            Map<String, Object> apiMap = new HashMap<>();
                            apiMap.put("apiId", api.getServiceName() + "_" + api.getPath().hashCode());
                            apiMap.put("name", api.getSummary());
                            apiMap.put("path", api.getPath());
                            apiMap.put("method", api.getMethod());
                            return apiMap;
                        }, Collectors.toList())
                ));
        
        // 转换为列表格式
        List<Map<String, Object>> result = categories.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("service", entry.getKey());
                    categoryMap.put("count", entry.getValue().size());
                    categoryMap.put("apis", entry.getValue());
                    return categoryMap;
                })
                .collect(Collectors.toList());
        
        log.info("✅ [Agent] 共 {} 个服务分类", result.size());
        
        Map<String, Object> categoryResult = new HashMap<>();
        categoryResult.put("success", true);
        categoryResult.put("categories", result);
        return JSONUtil.toJsonStr(categoryResult);
    }
    
    /**
     * 元函数 3: 获取 API 详情
     * 
     * 获取指定 API 的详细信息（参数、返回值等）
     * 
     * @param params 参数: {apiId: "xxx"}
     * @return API 详细信息
     */
    public String getApiDetail(JSONObject params) {
        String apiId = params.getStr("apiId");
        
        log.info("📄 [Agent] 获取 API 详情: {}", apiId);
        
        if (apiId == null || apiId.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "请提供 apiId");
            return JSONUtil.toJsonStr(errorResult);
        }
        
        // 查找 API
        List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
        Optional<ApiMetadata> apiOpt = allApis.stream()
                .filter(api -> {
                    String id = api.getServiceName() + "_" + api.getPath().hashCode();
                    return id.equals(apiId);
                })
                .findFirst();
        
        if (apiOpt.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "未找到指定的 API");
            return JSONUtil.toJsonStr(errorResult);
        }
        
        ApiMetadata api = apiOpt.get();
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("apiId", apiId);
        detail.put("name", api.getSummary());
        detail.put("path", api.getPath());
        detail.put("method", api.getMethod());
        detail.put("service", api.getServiceName());
        detail.put("description", api.getDescription() != null ? api.getDescription() : api.getSummary());
        detail.put("parameters", api.getParameters());
        detail.put("tags", api.getTags());
        
        log.info("✅ [Agent] 返回 API 详情: {}", api.getSummary());
        
        Map<String, Object> detailResult = new HashMap<>();
        detailResult.put("success", true);
        detailResult.put("api", detail);
        return JSONUtil.toJsonStr(detailResult);
    }
    
    /**
     * 元函数 4: 执行 API
     * 
     * 执行指定的 API 并返回结果
     * 
     * @param params 参数: {apiId: "xxx", parameters: {...}}
     * @return API 执行结果
     */
    public String executeApi(JSONObject params) {
        String apiId = params.getStr("apiId");
        JSONObject parameters = params.getJSONObject("parameters");
        
        log.info("🚀 [Agent] 执行 API: {}, 参数: {}", apiId, parameters);
        
        if (apiId == null || apiId.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "请提供 apiId");
            return JSONUtil.toJsonStr(errorResult);
        }
        
        // 查找 API
        List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
        Optional<ApiMetadata> apiOpt = allApis.stream()
                .filter(api -> {
                    String id = api.getServiceName() + "_" + api.getPath().hashCode();
                    return id.equals(apiId);
                })
                .findFirst();
        
        if (apiOpt.isEmpty()) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "未找到指定的 API");
            return JSONUtil.toJsonStr(errorResult);
        }
        
        ApiMetadata api = apiOpt.get();
        
        try {
            // 构造函数名（与 ApiFunctionRegistry 一致）
            String functionName = api.getServiceName() + "_" + 
                                api.getMethod().toLowerCase() + "_" + 
                                api.getPath().replaceAll("[^a-zA-Z0-9]", "_");
            
            // 执行函数
            String result = apiFunctionRegistry.executeFunction(
                    functionName, 
                    parameters != null ? parameters : new JSONObject()
            );
            
            log.info("✅ [Agent] API 执行成功");
            
            Map<String, Object> successResult = new HashMap<>();
            successResult.put("success", true);
            successResult.put("result", JSONUtil.parseObj(result));
            return JSONUtil.toJsonStr(successResult);
            
        } catch (Exception e) {
            log.error("❌ [Agent] API 执行失败: {}", e.getMessage());
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "API 执行失败: " + e.getMessage());
            return JSONUtil.toJsonStr(errorResult);
        }
    }
}

