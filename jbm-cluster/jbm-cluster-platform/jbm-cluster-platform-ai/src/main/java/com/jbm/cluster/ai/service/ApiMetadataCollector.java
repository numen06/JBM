package com.jbm.cluster.ai.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jbm.cluster.ai.model.ApiMetadata;
import com.jbm.cluster.ai.model.ApiParameter;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * API 元数据收集器
 * 从 Nacos 注册中心和 Swagger 文档收集接口元数据
 * 支持文件缓存和后台异步收集
 * @author wesley
 */
@Service
@Slf4j
public class ApiMetadataCollector {

    @Autowired
    private DiscoveryClient discoveryClient;
    
    /**
     * API 元数据缓存文件路径
     */
    @Value("${dashscope.api-cache-file:data/api-metadata-cache.json}")
    private String cacheFilePath;
    
    /**
     * 是否启用文件缓存
     */
    @Value("${dashscope.api-cache-enabled:true}")
    private boolean cacheEnabled;
    
    /**
     * 缓存的 API 元数据，key 为 serviceName
     */
    private final Map<String, List<ApiMetadata>> apiMetadataCache = new ConcurrentHashMap<>();
    
    /**
     * 所有 API 元数据的扁平列表
     */
    private final List<ApiMetadata> allApis = new ArrayList<>();
    
    /**
     * 多线程执行器
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * 是否正在收集中
     */
    private volatile boolean collecting = false;
    
    /**
     * 应用启动后初始化
     */
    @PostConstruct
    public void initialize() {
        log.info("🚀 初始化 API 元数据收集器...");
        
        // 尝试从缓存文件加载
        if (cacheEnabled && loadFromCache()) {
            log.info("✅ 从缓存文件加载 {} 个 API", allApis.size());
            // 后台异步刷新
            asyncCollectAllApiMetadata();
        } else {
            log.info("📋 缓存未命中，开始后台收集 API 元数据...");
            // 后台异步收集
            asyncCollectAllApiMetadata();
        }
    }
    
    /**
     * 异步收集所有 API 元数据
     */
    @Async
    public void asyncCollectAllApiMetadata() {
        if (collecting) {
            log.debug("⚠️ API 收集正在进行中，跳过本次请求");
            return;
        }
        
        new Thread(() -> {
            try {
                collectAllApiMetadata();
            } catch (Exception e) {
                log.error("❌ 后台收集 API 失败", e);
            }
        }, "api-collector-thread").start();
    }
    
    /**
     * 定时刷新 API 元数据（每30分钟）
     */
    @Scheduled(fixedRate = 1800000)
    public void scheduleRefresh() {
        log.info("⏰ 定时刷新 API 元数据...");
        asyncCollectAllApiMetadata();
    }
    
    /**
     * 收集所有服务的 API 元数据（多线程）
     */
    public void collectAllApiMetadata() {
        if (collecting) {
            log.debug("⚠️ API 收集正在进行中，跳过重复收集");
            return;
        }
        
        collecting = true;
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> services = discoveryClient.getServices();
            log.info("📋 发现 {} 个注册服务，开始多线程收集...", services.size());
            
            // 使用多线程并发收集
            Map<String, List<ApiMetadata>> newCache = new ConcurrentHashMap<>();
            List<Thread> threads = new ArrayList<>();
            
            for (String serviceName : services) {
                Thread thread = new Thread(() -> {
                    try {
                        List<ApiMetadata> apis = collectServiceApiMetadata(serviceName);
                        if (CollUtil.isNotEmpty(apis)) {
                            newCache.put(serviceName, apis);
                            log.info("✅ 成功收集服务 {} 的 {} 个 API", serviceName, apis.size());
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ 收集服务 {} 的 API 元数据失败: {}", serviceName, e.getMessage());
                    }
                }, "api-collector-" + serviceName);
                
                thread.start();
                threads.add(thread);
            }
            
            // 等待所有线程完成（最多30秒）
            for (Thread thread : threads) {
                try {
                    thread.join(30000);
                } catch (InterruptedException e) {
                    log.warn("⚠️ 等待收集线程超时");
                }
            }
            
            // 更新缓存
            apiMetadataCache.clear();
            apiMetadataCache.putAll(newCache);
            
            // 更新扁平列表
            synchronized (allApis) {
                allApis.clear();
                apiMetadataCache.values().forEach(allApis::addAll);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("📊 API 元数据收集完成: 成功 {} 个服务, 共 {} 个 API，耗时 {}ms", 
                    newCache.size(), allApis.size(), duration);
            
            // 保存到缓存文件
            if (cacheEnabled && !allApis.isEmpty()) {
                saveToCache();
            }
            
        } catch (Exception e) {
            log.error("❌ 收集 API 元数据失败", e);
        } finally {
            collecting = false;
        }
    }
    
    /**
     * 收集指定服务的 API 元数据
     */
    public List<ApiMetadata> collectServiceApiMetadata(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (CollUtil.isEmpty(instances)) {
            log.debug("服务 {} 无可用实例", serviceName);
            return Collections.emptyList();
        }
        
        ServiceInstance instance = instances.get(0);
        String baseUrl = instance.getUri().toString();
        
        // 尝试获取 Swagger 文档
        List<ApiMetadata> apis = new ArrayList<>();
        
        // 尝试 Swagger 2.0 路径
        try {
            String swaggerUrl = baseUrl + "/v2/api-docs";
            apis = parseSwagger2(serviceName, swaggerUrl);
            if (CollUtil.isNotEmpty(apis)) {
                return apis;
            }
        } catch (Exception e) {
            log.debug("服务 {} 没有 Swagger 2.0 文档", serviceName);
        }
        
        // 尝试 OpenAPI 3.0 路径
        try {
            String openApiUrl = baseUrl + "/v3/api-docs";
            apis = parseOpenApi3(serviceName, openApiUrl);
            if (CollUtil.isNotEmpty(apis)) {
                return apis;
            }
        } catch (Exception e) {
            log.debug("服务 {} 没有 OpenAPI 3.0 文档", serviceName);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 解析 Swagger 2.0 文档
     */
    private List<ApiMetadata> parseSwagger2(String serviceName, String swaggerUrl) {
        List<ApiMetadata> apis = new ArrayList<>();
        
        try {
            // 获取 Swagger JSON
            String jsonStr = HttpUtil.get(swaggerUrl, 5000);
            if (StrUtil.isEmpty(jsonStr)) {
                return apis;
            }
            
            // 使用 Swagger Parser 解析
            Swagger swagger = new SwaggerParser().parse(jsonStr);
            if (swagger == null || swagger.getPaths() == null) {
                return apis;
            }
            
            // 遍历所有路径
            Map<String, Path> paths = swagger.getPaths();
            for (Map.Entry<String, Path> pathEntry : paths.entrySet()) {
                String pathStr = pathEntry.getKey();
                Path path = pathEntry.getValue();
                
                // 解析每个 HTTP 方法
                parseOperation(serviceName, pathStr, "GET", path.getGet(), apis);
                parseOperation(serviceName, pathStr, "POST", path.getPost(), apis);
                parseOperation(serviceName, pathStr, "PUT", path.getPut(), apis);
                parseOperation(serviceName, pathStr, "DELETE", path.getDelete(), apis);
                parseOperation(serviceName, pathStr, "PATCH", path.getPatch(), apis);
            }
            
        } catch (Exception e) {
            log.warn("解析 Swagger 2.0 文档失败: {}", e.getMessage());
        }
        
        return apis;
    }
    
    /**
     * 解析单个 Operation
     */
    private void parseOperation(String serviceName, String path, String method, 
                                 Operation operation, List<ApiMetadata> apis) {
        if (operation == null) {
            return;
        }
        
        ApiMetadata api = new ApiMetadata();
        api.setServiceName(serviceName);
        api.setPath(path);
        api.setMethod(method);
        api.setSummary(operation.getSummary());
        api.setDescription(operation.getDescription());
        api.setTags(operation.getTags());
        
        // 解析参数
        if (CollUtil.isNotEmpty(operation.getParameters())) {
            List<ApiParameter> params = operation.getParameters().stream()
                    .map(this::parseParameter)
                    .collect(Collectors.toList());
            api.setParameters(params);
        }
        
        // 判断响应类型
        if (operation.getResponses() != null && operation.getResponses().get("200") != null) {
            Response response = operation.getResponses().get("200");
            if (response.getSchema() != null) {
                // Swagger 2.0 的 Property 类型处理
                api.setResponseType(response.getSchema().getType());
            }
        }
        
        apis.add(api);
    }
    
    /**
     * 解析参数
     */
    private ApiParameter parseParameter(Parameter parameter) {
        ApiParameter apiParam = new ApiParameter();
        apiParam.setName(parameter.getName());
        apiParam.setIn(parameter.getIn());
        apiParam.setDescription(parameter.getDescription());
        apiParam.setRequired(parameter.getRequired());
        
        // 类型信息
        if (parameter instanceof io.swagger.models.parameters.QueryParameter) {
            io.swagger.models.parameters.QueryParameter qp = (io.swagger.models.parameters.QueryParameter) parameter;
            apiParam.setType(qp.getType());
        } else if (parameter instanceof io.swagger.models.parameters.PathParameter) {
            io.swagger.models.parameters.PathParameter pp = (io.swagger.models.parameters.PathParameter) parameter;
            apiParam.setType(pp.getType());
        } else if (parameter instanceof io.swagger.models.parameters.BodyParameter) {
            apiParam.setType("object");
        }
        
        return apiParam;
    }
    
    /**
     * 解析 OpenAPI 3.0 文档（简化版，主要使用 Swagger 2.0）
     */
    private List<ApiMetadata> parseOpenApi3(String serviceName, String openApiUrl) {
        // 简化实现，可以后续扩展
        return Collections.emptyList();
    }
    
    /**
     * 获取所有 API 元数据
     */
    public List<ApiMetadata> getAllApis() {
        synchronized (allApis) {
            return new ArrayList<>(allApis);
        }
    }
    
    /**
     * 根据服务名获取 API 元数据
     */
    public List<ApiMetadata> getApisByService(String serviceName) {
        return apiMetadataCache.getOrDefault(serviceName, Collections.emptyList());
    }
    
    /**
     * 搜索 API（根据描述、路径等）
     */
    public List<ApiMetadata> searchApis(String keyword) {
        if (StrUtil.isEmpty(keyword)) {
            return getAllApis();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return allApis.stream()
                .filter(api -> {
                    String desc = StrUtil.emptyToDefault(api.getDescription(), "").toLowerCase();
                    String path = StrUtil.emptyToDefault(api.getPath(), "").toLowerCase();
                    String summary = StrUtil.emptyToDefault(api.getSummary(), "").toLowerCase();
                    return desc.contains(lowerKeyword) || 
                           path.contains(lowerKeyword) || 
                           summary.contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 从缓存文件加载 API 元数据
     */
    private boolean loadFromCache() {
        try {
            File cacheFile = new File(cacheFilePath);
            if (!cacheFile.exists()) {
                log.debug("📁 缓存文件不存在: {}", cacheFilePath);
                return false;
            }
            
            // 检查文件是否过期（超过24小时）
            long fileAge = System.currentTimeMillis() - cacheFile.lastModified();
            if (fileAge > 24 * 60 * 60 * 1000) {
                log.info("⏰ 缓存文件已过期（{}小时），将重新收集", fileAge / (60 * 60 * 1000));
                return false;
            }
            
            log.info("📂 从缓存文件加载 API 元数据: {}", cacheFilePath);
            String jsonContent = FileUtil.readString(cacheFile, StandardCharsets.UTF_8);
            
            // 使用 FastJSON 解析
            Map<String, List<ApiMetadata>> loadedCache = JSON.parseObject(
                    jsonContent, 
                    new TypeReference<Map<String, List<ApiMetadata>>>() {}
            );
            
            if (loadedCache != null && !loadedCache.isEmpty()) {
                apiMetadataCache.clear();
                apiMetadataCache.putAll(loadedCache);
                
                // 更新扁平列表
                synchronized (allApis) {
                    allApis.clear();
                    apiMetadataCache.values().forEach(allApis::addAll);
                }
                
                log.info("✅ 成功从缓存加载 {} 个服务的 {} 个 API", 
                        loadedCache.size(), allApis.size());
                return true;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 从缓存文件加载失败: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 保存 API 元数据到缓存文件
     */
    private void saveToCache() {
        try {
            // 确保目录存在
            File cacheFile = new File(cacheFilePath);
            File parentDir = cacheFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                log.debug("📁 创建缓存目录: {}", parentDir.getAbsolutePath());
            }
            
            // 使用 FastJSON 序列化
            String jsonContent = JSON.toJSONString(apiMetadataCache, true);
            
            // 写入文件
            FileUtil.writeString(jsonContent, cacheFile, StandardCharsets.UTF_8);
            
            log.info("💾 API 元数据已保存到缓存文件: {} ({} bytes)", 
                    cacheFilePath, cacheFile.length());
            
        } catch (Exception e) {
            log.error("❌ 保存缓存文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清除缓存文件
     */
    public void clearCache() {
        try {
            File cacheFile = new File(cacheFilePath);
            if (cacheFile.exists()) {
                cacheFile.delete();
                log.info("🗑️ 缓存文件已删除");
            }
            
            apiMetadataCache.clear();
            synchronized (allApis) {
                allApis.clear();
            }
            
        } catch (Exception e) {
            log.error("❌ 清除缓存失败", e);
        }
    }
    
    /**
     * 强制刷新（清除缓存并重新收集）
     */
    public void forceRefresh() {
        log.info("🔄 强制刷新 API 元数据...");
        clearCache();
        asyncCollectAllApiMetadata();
    }
}

