package com.jbm.cluster.ai.agent.execution;

import cn.hutool.json.JSONUtil;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * API 执行器
 * 
 * 负责执行 API 调用并返回结果
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ApiExecutor {
    
    @Autowired
    private JbmRequestTemplate jbmRequestTemplate;
    
    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private String response;
        private int statusCode;
        private boolean success;
        private String errorMessage;
        private long duration;
        
        public ExecutionResult(String response, int statusCode, long duration) {
            this.response = response;
            this.statusCode = statusCode;
            this.success = statusCode >= 200 && statusCode < 300;
            this.duration = duration;
        }
        
        public ExecutionResult(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            this.statusCode = 500;
        }
        
        public String getResponse() { return response; }
        public int getStatusCode() { return statusCode; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getDuration() { return duration; }
    }
    
    /**
     * 执行 API 调用
     * 
     * @param url 完整 URL（支持 feign:// 协议）
     * @param method HTTP 方法
     * @param requestBody 请求体（可选）
     * @return 执行结果
     */
    public ExecutionResult execute(String url, String method, String requestBody) {
        log.info("🚀 [API Execution] 开始执行 API");
        log.info("   URL: {}", url);
        log.info("   Method: {}", method);
        if (requestBody != null) {
            log.info("   Body: {}", requestBody);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用 JbmRequestTemplate 执行内部调用
            Response response = jbmRequestTemplate.request(url, method, requestBody);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                
                log.info("✅ [API Execution] API 执行成功，耗时: {}ms", duration);
                log.info("   响应长度: {} 字符", responseBody.length());
                
                // 验证响应是否为有效 JSON
                if (!responseBody.isEmpty()) {
                    try {
                        JSONUtil.parse(responseBody);
                    } catch (Exception e) {
                        log.warn("⚠️  [API Execution] 响应不是有效的 JSON: {}", 
                                responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody);
                    }
                }
                
                return new ExecutionResult(responseBody, response.code(), duration);
            } else {
                long errorDuration = System.currentTimeMillis() - startTime;
                String errorMsg = "API 调用失败: HTTP " + response.code();
                log.error("❌ [API Execution] {}, 耗时: {}ms", errorMsg, errorDuration);
                
                return new ExecutionResult(errorMsg);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [API Execution] API 执行失败，耗时: {}ms", duration);
            log.error("   错误: {}", e.getMessage(), e);
            
            return new ExecutionResult("API 执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行 API 调用（无请求体）
     */
    public ExecutionResult execute(String url, String method) {
        return execute(url, method, null);
    }
}

