package com.jbm.cluster.ai.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * API 元数据
 * @author wesley
 */
@Data
public class ApiMetadata {
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * API 路径
     */
    private String path;
    
    /**
     * HTTP 方法 (GET, POST, PUT, DELETE 等)
     */
    private String method;
    
    /**
     * API 摘要（简短描述）
     */
    private String summary;
    
    /**
     * API 描述（详细描述）
     */
    private String description;
    
    /**
     * API 标签/分类
     */
    private List<String> tags;
    
    /**
     * 请求参数列表
     */
    private List<ApiParameter> parameters;
    
    /**
     * 响应类型
     */
    private String responseType;
    
    /**
     * 是否需要认证
     */
    private boolean requiresAuth = true;
    
    /**
     * 生成函数名（用于 AI Function Calling）
     * 
     * 限制：通义千问要求函数名不超过 64 个字符
     * 
     * 优化策略：
     * 1. 缩短 HTTP 方法：GET→g, POST→p, PUT→u, DELETE→d, PATCH→pa
     * 2. 移除常见前缀：/api/, /v1/, /v2/
     * 3. 智能缩写：users→u, list→l, info→i
     * 4. 确保长度 ≤ 64
     */
    public String generateFunctionName() {
        // 1. 缩短 HTTP 方法名
        String methodPrefix = switch (method.toUpperCase()) {
            case "GET" -> "g";
            case "POST" -> "p";
            case "PUT" -> "u";
            case "DELETE" -> "d";
            case "PATCH" -> "pa";
            default -> method.toLowerCase().substring(0, 1);
        };
        
        // 2. 处理路径：移除常见前缀
        String processedPath = path;
        processedPath = processedPath.replaceFirst("^/api/", "/");
        processedPath = processedPath.replaceFirst("^/v\\d+/", "/");
        
        // 3. 转换为函数名格式
        String funcName = methodPrefix + processedPath.replace("/", "_").replace("-", "_");
        
        // 4. 移除连续的下划线
        funcName = funcName.replaceAll("_+", "_");
        
        // 5. 移除开头和结尾的下划线
        funcName = funcName.replaceAll("^_|_$", "");
        
        // 6. 如果超过 64 字符，进行智能缩短
        if (funcName.length() > 64) {
            funcName = shortenFunctionName(funcName);
        }
        
        return funcName;
    }
    
    /**
     * 缩短函数名到 64 字符以内
     */
    private String shortenFunctionName(String funcName) {
        // 策略1：智能缩写常见单词
        funcName = funcName.replace("users", "u")
                          .replace("user", "u")
                          .replace("list", "l")
                          .replace("info", "i")
                          .replace("detail", "d")
                          .replace("query", "q")
                          .replace("search", "s")
                          .replace("create", "c")
                          .replace("update", "upd")
                          .replace("delete", "del")
                          .replace("management", "mgmt")
                          .replace("system", "sys")
                          .replace("admin", "adm")
                          .replace("config", "cfg");
        
        // 策略2：如果还是太长，截断并添加哈希
        if (funcName.length() > 64) {
            // 保留前 58 个字符，添加 6 位哈希
            String hash = String.format("%06x", funcName.hashCode() & 0xFFFFFF);
            funcName = funcName.substring(0, Math.min(58, funcName.length())) + "_" + hash;
        }
        
        return funcName;
    }
    
    /**
     * 生成完整的 URL（用于 Feign 调用）
     */
    public String generateFeignUrl() {
        return "feign://" + serviceName + path;
    }
}

