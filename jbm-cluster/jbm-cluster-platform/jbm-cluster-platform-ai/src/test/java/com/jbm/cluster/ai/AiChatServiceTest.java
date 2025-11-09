package com.jbm.cluster.ai;

import com.jbm.cluster.ai.model.ChatRequest;
import com.jbm.cluster.ai.model.ChatResponse;
import com.jbm.cluster.ai.service.AiChatService;
import com.jbm.cluster.ai.service.ApiMetadataCollector;
import com.jbm.cluster.ai.service.ApiFunctionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * AI 聊天服务集成测试
 * @author wesley
 */
@SpringBootTest
@Slf4j
public class AiChatServiceTest {

    @Autowired(required = false)
    private AiChatService aiChatService;
    
    @Autowired(required = false)
    private ApiMetadataCollector apiMetadataCollector;
    
    @Autowired(required = false)
    private ApiFunctionRegistry apiFunctionRegistry;
    
    /**
     * 测试 API 元数据收集
     */
    @Test
    public void testApiMetadataCollection() {
        if (apiMetadataCollector == null) {
            log.warn("⚠️ ApiMetadataCollector 未注入，跳过测试");
            return;
        }
        
        log.info("📋 开始测试 API 元数据收集...");
        
        // 触发收集
        apiMetadataCollector.collectAllApiMetadata();
        
        // 获取所有 API
        var allApis = apiMetadataCollector.getAllApis();
        log.info("✅ 收集到 {} 个 API", allApis.size());
        
        // 打印前 5 个 API
        allApis.stream()
                .limit(5)
                .forEach(api -> log.info("  - {} {} [{}]", 
                        api.getMethod(), api.getPath(), api.getServiceName()));
    }
    
    /**
     * 测试函数注册
     */
    @Test
    public void testFunctionRegistry() {
        if (apiFunctionRegistry == null) {
            log.warn("⚠️ ApiFunctionRegistry 未注入，跳过测试");
            return;
        }
        
        log.info("📋 开始测试函数注册...");
        
        // 等待函数注册完成
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // 获取所有函数
        var allFunctions = apiFunctionRegistry.getAllFunctions();
        log.info("✅ 注册了 {} 个函数", allFunctions.size());
        
        // 打印前 5 个函数
        allFunctions.stream()
                .limit(5)
                .forEach(func -> log.info("  - {} : {}", 
                        func.getName(), func.getDescription()));
    }
    
    /**
     * 测试简单对话
     */
    @Test
    public void testSimpleChat() {
        if (aiChatService == null) {
            log.warn("⚠️ AiChatService 未注入，跳过测试");
            return;
        }
        
        log.info("💬 开始测试简单对话...");
        
        ChatRequest request = new ChatRequest();
        request.setMessage("你好，你能做什么？");
        request.setEnableFunctions(false);
        
        ChatResponse response = aiChatService.chat(request);
        
        log.info("✅ AI 回复: {}", response.getMessage());
        if (response.getError() != null) {
            log.error("❌ 错误: {}", response.getError());
        }
    }
    
    /**
     * 测试 Function Calling
     */
    @Test
    public void testFunctionCalling() {
        if (aiChatService == null) {
            log.warn("⚠️ AiChatService 未注入，跳过测试");
            return;
        }
        
        log.info("🎯 开始测试 Function Calling...");
        
        // 等待函数注册完成
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        ChatRequest request = new ChatRequest();
        request.setMessage("帮我查询一下当前系统中有哪些在线用户");
        request.setEnableFunctions(true);
        
        ChatResponse response = aiChatService.chat(request);
        
        log.info("✅ AI 回复: {}", response.getMessage());
        if (response.isFunctionCalled()) {
            log.info("📞 调用了函数: {}", response.getFunctionName());
            log.info("📊 函数结果: {}", response.getFunctionResult());
        }
        if (response.getError() != null) {
            log.error("❌ 错误: {}", response.getError());
        }
    }
    
    /**
     * 测试可用函数列表
     */
    @Test
    public void testListAvailableFunctions() {
        if (aiChatService == null) {
            log.warn("⚠️ AiChatService 未注入，跳过测试");
            return;
        }
        
        log.info("📋 开始测试可用函数列表...");
        
        // 等待函数注册完成
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        List<Map<String, Object>> functions = aiChatService.listAvailableFunctions();
        log.info("✅ 共有 {} 个可用函数", functions.size());
        
        // 打印前 10 个函数
        functions.stream()
                .limit(10)
                .forEach(func -> log.info("  - {} : {}", 
                        func.get("name"), func.get("description")));
    }
}

