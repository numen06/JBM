package com.jbm.cluster.ai.agent.binding;

import cn.hutool.json.JSONUtil;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.model.ApiParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数绑定器
 * 
 * 根据 API 定义和意图参数构造完整的 URL 和请求体
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ParameterBinder {
    
    /**
     * 路径参数模式：{id}, {userId} 等
     */
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    /**
     * 绑定结果
     */
    public static class BindingResult {
        private String url;
        private String method;
        private String requestBody;
        private boolean success;
        private String errorMessage;
        private java.util.List<String> missingRequiredParameters;
        private boolean partialBinding;
        
        public BindingResult(String url, String method, String requestBody) {
            this.url = url;
            this.method = method;
            this.requestBody = requestBody;
            this.success = true;
            this.partialBinding = false;
        }
        
        public BindingResult(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            this.partialBinding = false;
        }
        
        /**
         * 部分绑定结果（用于对话模式）
         */
        public BindingResult(String url, String method, String requestBody, 
                            java.util.List<String> missingRequiredParameters) {
            this.url = url;
            this.method = method;
            this.requestBody = requestBody;
            this.success = false;
            this.partialBinding = true;
            this.missingRequiredParameters = missingRequiredParameters;
            this.errorMessage = "缺少必填参数: " + String.join(", ", missingRequiredParameters);
        }
        
        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public String getRequestBody() { return requestBody; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public java.util.List<String> getMissingRequiredParameters() { return missingRequiredParameters; }
        public boolean isPartialBinding() { return partialBinding; }
    }
    
    /**
     * 绑定参数（完整模式，要求所有必填参数）
     * 
     * @param api API 定义
     * @param params 参数 Map
     * @return 绑定结果
     */
    public BindingResult bind(ApiDefinition api, Map<String, Object> params) {
        return bind(api, params, false);
    }
    
    /**
     * 绑定参数（支持部分绑定）
     * 
     * @param api API 定义
     * @param params 参数 Map
     * @param allowPartial 是否允许部分绑定（用于对话模式）
     * @return 绑定结果
     */
    public BindingResult bind(ApiDefinition api, Map<String, Object> params, boolean allowPartial) {
        if (api == null) {
            return new BindingResult("API 定义不能为空");
        }
        
        log.info("🔗 [Parameter Binding] 开始参数绑定");
        log.info("   API: {} {}", api.getMethod(), api.getPath());
        log.info("   参数: {}", params);
        log.info("   部分绑定模式: {}", allowPartial);
        
        try {
            // 1. 检查必填参数
            java.util.List<String> missingParams = checkMissingRequiredParams(api, params);
            
            if (!missingParams.isEmpty()) {
                log.warn("⚠️  [Parameter Binding] 缺少必填参数: {}", missingParams);
                
                if (!allowPartial) {
                    // 完整模式：缺少参数直接失败
                    return new BindingResult("缺少必填参数: " + String.join(", ", missingParams));
                }
                
                // 部分绑定模式：继续绑定已有参数，返回缺失参数列表
                log.info("   使用部分绑定模式，继续绑定已有参数");
            }
            
            // 2. 处理路径参数
            String path = bindPathParams(api.getPath(), params);
            
            // 3. 处理查询参数和请求体
            Map<String, Object> unboundParams = new HashMap<>(params != null ? params : Map.of());
            
            // 移除已绑定到路径的参数
            if (params != null) {
                Matcher matcher = PATH_PARAM_PATTERN.matcher(api.getPath());
                while (matcher.find()) {
                    String paramName = matcher.group(1);
                    unboundParams.remove(paramName);
                }
            }
            
            String requestBody = null;
            
            // 4. 根据 HTTP 方法处理剩余参数
            if ("GET".equalsIgnoreCase(api.getMethod()) || "DELETE".equalsIgnoreCase(api.getMethod())) {
                // GET/DELETE: 使用查询参数
                if (!unboundParams.isEmpty()) {
                    path = addQueryParams(path, unboundParams);
                }
            } else if ("POST".equalsIgnoreCase(api.getMethod()) || 
                      "PUT".equalsIgnoreCase(api.getMethod()) || 
                      "PATCH".equalsIgnoreCase(api.getMethod())) {
                // POST/PUT/PATCH: 使用请求体
                if (!unboundParams.isEmpty()) {
                    requestBody = JSONUtil.toJsonStr(unboundParams);
                }
            }
            
            // 5. 构建完整 URL（使用 Feign 协议）
            String fullUrl = buildFeignUrl(api.getServiceName(), path);
            
            log.info("✅ [Parameter Binding] 参数绑定完成");
            log.info("   URL: {}", fullUrl);
            if (requestBody != null) {
                log.info("   Body: {}", requestBody);
            }
            
            // 如果有缺失参数且允许部分绑定，返回部分绑定结果
            if (!missingParams.isEmpty() && allowPartial) {
                log.info("   部分绑定: 缺失 {} 个必填参数", missingParams.size());
                return new BindingResult(fullUrl, api.getMethod(), requestBody, missingParams);
            }
            
            return new BindingResult(fullUrl, api.getMethod(), requestBody);
            
        } catch (Exception e) {
            log.error("❌ [Parameter Binding] 参数绑定失败: {}", e.getMessage(), e);
            return new BindingResult("参数绑定失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查缺失的必填参数（新方法，返回列表）
     */
    private java.util.List<String> checkMissingRequiredParams(ApiDefinition api, Map<String, Object> params) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        
        if (api.getParameters() == null || api.getParameters().isEmpty()) {
            return missing; // 没有参数要求
        }
        
        for (ApiParameter param : api.getParameters()) {
            if (param.isRequired()) {
                Object value = params != null ? params.get(param.getName()) : null;
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    missing.add(param.getName());
                }
            }
        }
        
        return missing;
    }
    
    /**
     * 验证必填参数（旧方法，保持兼容）
     */
    @Deprecated
    private String validateRequiredParams(ApiDefinition api, Map<String, Object> params) {
        java.util.List<String> missing = checkMissingRequiredParams(api, params);
        if (missing.isEmpty()) {
            return null;
        }
        return "缺少必填参数: " + String.join(", ", missing);
    }
    
    /**
     * 绑定路径参数
     * 
     * 将 /users/{id} 转换为 /users/123
     */
    private String bindPathParams(String path, Map<String, Object> params) {
        if (path == null || params == null) {
            return path;
        }
        
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = params.get(paramName);
            
            if (paramValue != null) {
                matcher.appendReplacement(result, paramValue.toString());
                log.debug("   路径参数: {} = {}", paramName, paramValue);
            } else {
                log.warn("⚠️  路径参数 {} 未找到对应值", paramName);
                matcher.appendReplacement(result, "{" + paramName + "}");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 添加查询参数
     * 
     * 将参数添加到 URL 中：/users?name=xxx&age=20
     */
    private String addQueryParams(String path, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return path;
        }
        
        StringBuilder query = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                if (first) {
                    query.append("?");
                    first = false;
                } else {
                    query.append("&");
                }
                query.append(entry.getKey())
                     .append("=")
                     .append(entry.getValue().toString());
                
                log.debug("   查询参数: {} = {}", entry.getKey(), entry.getValue());
            }
        }
        
        return path + query.toString();
    }
    
    /**
     * 构建 Feign URL
     * 
     * 格式：feign://service-name/path
     */
    private String buildFeignUrl(String serviceName, String path) {
        // 确保 path 以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return "feign://" + serviceName + path;
    }
}


