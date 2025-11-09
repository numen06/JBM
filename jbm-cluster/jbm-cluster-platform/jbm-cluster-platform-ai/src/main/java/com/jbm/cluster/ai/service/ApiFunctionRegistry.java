package com.jbm.cluster.ai.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.ai.model.ApiMetadata;
import com.jbm.cluster.ai.model.ApiParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * API 函数注册器
 * 将收集到的 API 元数据注册为 AI 可调用的函数
 * @author wesley
 */
@Service
@Slf4j
public class ApiFunctionRegistry {

    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    @Autowired
    private ApiFunctionExecutor apiFunctionExecutor;
    
    /**
     * 注册的函数映射，key 为函数名
     */
    private final Map<String, FunctionDefinition> functionDefinitions = new ConcurrentHashMap<>();
    
    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;
    
    /**
     * 应用启动后注册所有函数
     */
    @PostConstruct
    public void initialize() {
        log.info("🚀 初始化 API 函数注册器...");
        // 后台异步注册，不阻塞启动
        new Thread(() -> {
            try {
                // 等待 API 元数据收集器加载数据（从缓存或收集）
                Thread.sleep(3000);
                registerAllFunctions();
                initialized = true;
            } catch (Exception e) {
                log.error("❌ 函数注册失败", e);
            }
        }, "function-registry-thread").start();
    }
    
    /**
     * 定时重新注册函数（每30分钟，与 API 收集同步）
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 1800000)
    public void scheduleRegister() {
        log.info("⏰ 定时重新注册函数...");
        registerAllFunctions();
    }
    
    /**
     * 注册所有函数
     */
    public synchronized void registerAllFunctions() {
        try {
            List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
            if (CollUtil.isEmpty(allApis)) {
                log.warn("⚠️ 没有可注册的 API，将在后续自动重试");
                return;
            }
            
            int oldSize = functionDefinitions.size();
            functionDefinitions.clear();
            
            int successCount = 0;
            for (ApiMetadata api : allApis) {
                try {
                    registerFunction(api);
                    successCount++;
                } catch (Exception e) {
                    log.warn("⚠️ 注册函数失败: {}", api.generateFunctionName(), e);
                }
            }
            
            log.info("✅ 函数注册完成: 成功 {} 个，总计 {} 个函数", 
                    successCount, functionDefinitions.size());
            
            if (oldSize > 0 && functionDefinitions.size() != oldSize) {
                log.info("🔄 函数数量变化: {} → {}", oldSize, functionDefinitions.size());
            }
            
            // 统计函数名长度
            long maxLength = functionDefinitions.values().stream()
                    .mapToInt(f -> f.getName().length())
                    .max()
                    .orElse(0);
            
            long avgLength = functionDefinitions.values().stream()
                    .mapToInt(f -> f.getName().length())
                    .average()
                    .orElse(0);
            
            log.info("📏 函数名长度统计: 最大 {} 字符, 平均 {} 字符", maxLength, (int) avgLength);
            
            // 检查是否有超过 64 字符的
            long overLimit = functionDefinitions.values().stream()
                    .filter(f -> f.getName().length() > 64)
                    .count();
            
            if (overLimit > 0) {
                log.error("⚠️ 警告: 有 {} 个函数名超过 64 字符限制！", overLimit);
                functionDefinitions.values().stream()
                        .filter(f -> f.getName().length() > 64)
                        .forEach(f -> log.error("   - {} ({} 字符)", f.getName(), f.getName().length()));
            } else {
                log.info("✅ 所有函数名都在 64 字符限制内");
            }
            
        } catch (Exception e) {
            log.error("❌ 注册函数失败", e);
        }
    }
    
    /**
     * 检查函数注册器是否已初始化
     */
    public boolean isInitialized() {
        return initialized && !functionDefinitions.isEmpty();
    }
    
    /**
     * 注册单个函数
     */
    private void registerFunction(ApiMetadata api) {
        String functionName = api.generateFunctionName();
        
        // 验证函数名长度（通义千问限制 64 字符）
        if (functionName.length() > 64) {
            log.error("❌ 函数名超过 64 字符限制: {} ({} 字符)", functionName, functionName.length());
            log.error("   路径: {} {}", api.getMethod(), api.getPath());
            log.error("   服务: {}", api.getServiceName());
            // 跳过这个函数，不注册
            return;
        }
        
        // 创建函数定义
        FunctionDefinition functionDef = new FunctionDefinition();
        functionDef.setName(functionName);
        functionDef.setDescription(buildFunctionDescription(api));
        functionDef.setParameters(buildFunctionParameters(api));
        
        // 创建执行函数
        Function<Map<String, Object>, String> executor = args -> {
            return apiFunctionExecutor.executeFunction(functionName, args);
        };
        functionDef.setExecutor(executor);
        
        functionDefinitions.put(functionName, functionDef);
        log.debug("📝 注册函数: {} ({}字符)", functionName, functionName.length());
    }
    
    /**
     * 构建函数描述
     */
    private String buildFunctionDescription(ApiMetadata api) {
        StringBuilder desc = new StringBuilder();
        
        // 优先使用 summary，其次使用 description
        String mainDesc = StrUtil.isNotEmpty(api.getSummary()) ? 
                api.getSummary() : api.getDescription();
        
        if (StrUtil.isNotEmpty(mainDesc)) {
            desc.append(mainDesc);
        } else {
            desc.append(api.getMethod()).append(" ").append(api.getPath());
        }
        
        // 添加服务信息
        desc.append(" [服务: ").append(api.getServiceName()).append("]");
        
        return desc.toString();
    }
    
    /**
     * 构建函数参数定义（JSON Schema 格式）
     */
    private Map<String, Object> buildFunctionParameters(ApiMetadata api) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        if (CollUtil.isNotEmpty(api.getParameters())) {
            for (ApiParameter param : api.getParameters()) {
                Map<String, Object> paramSchema = new HashMap<>();
                paramSchema.put("type", convertType(param.getType()));
                
                if (StrUtil.isNotEmpty(param.getDescription())) {
                    paramSchema.put("description", param.getDescription());
                }
                
                if (param.getExample() != null) {
                    paramSchema.put("example", param.getExample());
                }
                
                properties.put(param.getName(), paramSchema);
                
                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }
        }
        
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        
        return schema;
    }
    
    /**
     * 转换参数类型到 JSON Schema 类型
     */
    private String convertType(String swaggerType) {
        if (StrUtil.isEmpty(swaggerType)) {
            return "string";
        }
        
        return switch (swaggerType.toLowerCase()) {
            case "integer", "int", "long" -> "integer";
            case "number", "float", "double" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array", "list" -> "array";
            case "object" -> "object";
            default -> "string";
        };
    }
    
    /**
     * 获取所有函数定义
     */
    public Collection<FunctionDefinition> getAllFunctions() {
        return new ArrayList<>(functionDefinitions.values());
    }
    
    /**
     * 获取函数定义
     */
    public FunctionDefinition getFunction(String functionName) {
        return functionDefinitions.get(functionName);
    }
    
    /**
     * 执行函数
     */
    public String executeFunction(String functionName, Map<String, Object> arguments) {
        FunctionDefinition functionDef = functionDefinitions.get(functionName);
        if (functionDef == null) {
            log.warn("⚠️ 函数不存在: {}", functionName);
            return "{\"error\": \"Function not found: " + functionName + "\"}";
        }
        
        return functionDef.getExecutor().apply(arguments);
    }
    
    /**
     * 函数定义类
     */
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
        private Function<Map<String, Object>, String> executor;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
        
        public Function<Map<String, Object>, String> getExecutor() {
            return executor;
        }
        
        public void setExecutor(Function<Map<String, Object>, String> executor) {
            this.executor = executor;
        }
    }
}

