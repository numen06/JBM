package com.jbm.cluster.ai.controller;

import com.jbm.cluster.ai.agent.AgentService;
import com.jbm.cluster.ai.agent.model.AgentRequest;
import com.jbm.cluster.ai.agent.model.AgentResponse;
import com.jbm.cluster.ai.model.ChatRequest;
import com.jbm.cluster.ai.service.ApiMetadataCollector;
import io.reactivex.Flowable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天控制器
 * @author wesley
 */
@Api(tags = "AI 聊天")
@RestController
@RequestMapping("/ai")
@Slf4j
public class AiChatController {

    @Autowired
    private AgentService agentService;
    
    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    /**
     * 聊天接口（普通模式）- 使用新 Agent 架构
     */
    @ApiOperation("发送消息并获取 AI 回复（使用 Agent 架构）")
    @PostMapping("/chat")
    public AgentResponse chat(@RequestBody ChatRequest request) {
        log.info("📨 收到聊天请求（转发到 Agent）: {}", request.getMessage());
        
        // 转换为 AgentRequest
        AgentRequest agentRequest = AgentRequest.builder()
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .enableAgent(request.isEnableFunctions())
                .build();
        
        return agentService.ask(agentRequest);
    }
    
    /**
     * 聊天接口（流式模式）- 使用新 Agent 架构
     * 使用 Server-Sent Events (SSE) 实现流式响应
     * AI 逐字生成，用户实时看到回复，体验更好
     */
    @ApiOperation("发送消息并获取 AI 流式回复（使用 Agent 架构，推荐）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("📨 [流式] 收到聊天请求（转发到 Agent）: {}", request.getMessage());
        
        // 转换为 AgentRequest
        AgentRequest agentRequest = AgentRequest.builder()
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .enableAgent(request.isEnableFunctions())
                .build();
        
        // 将 RxJava Flowable 转换为 Reactor Flux
        Flowable<String> flowable = agentService.askStream(agentRequest);
        return Flux.from(flowable);
    }
    
    /**
     * 清除会话
     */
    @ApiOperation("清除指定会话的历史记录")
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        // 新的 Agent 架构是无状态的，不需要清除会话
        // 保留接口以向后兼容
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Agent 架构无需清除会话（无状态设计）");
        return result;
    }
    
    /**
     * 获取可用 API 列表
     */
    @ApiOperation("获取所有可用的 API 列表")
    @GetMapping("/functions")
    public Map<String, Object> listFunctions() {
        int totalApis = apiMetadataCollector.getAllApis().size();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", totalApis);
        result.put("apis", apiMetadataCollector.getAllApis());
        return result;
    }
    
    /**
     * 聊天页面
     */
    @ApiOperation("访问 AI 聊天界面")
    @GetMapping(value = "/chat-ui", produces = MediaType.TEXT_HTML_VALUE)
    public String chatUI() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/chat.html");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 健康检查
     */
    @ApiOperation("健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        int apiCount = apiMetadataCollector.getAllApis().size();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "jbm-ai");
        result.put("message", "AI 服务运行正常");
        result.put("apiCount", apiCount);
        return result;
    }
    
    /**
     * 强制刷新 API 元数据（清除缓存并重新收集）
     */
    @ApiOperation("强制刷新 API 元数据缓存")
    @PostMapping("/refresh")
    public Map<String, Object> refreshApiMetadata() {
        log.info("📡 收到刷新 API 元数据请求");
        apiMetadataCollector.forceRefresh();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "API 元数据刷新任务已启动（后台执行）");
        return result;
    }
    
    /**
     * 清除 API 元数据缓存
     */
    @ApiOperation("清除 API 元数据缓存文件")
    @DeleteMapping("/cache")
    public Map<String, Object> clearCache() {
        log.info("🗑️ 收到清除缓存请求");
        apiMetadataCollector.clearCache();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "缓存已清除");
        return result;
    }
    
    /**
     * 获取 API 元数据统计信息
     */
    @ApiOperation("获取 API 元数据统计信息")
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        int totalApis = apiMetadataCollector.getAllApis().size();
        long serviceCount = apiMetadataCollector.getAllApis().stream()
                .map(api -> api.getServiceName())
                .distinct()
                .count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalApis", totalApis);
        result.put("serviceCount", (int) serviceCount);
        result.put("architecture", "Agent Modular");
        result.put("cacheEnabled", true);
        return result;
    }
    
    // ==================== Agent 模式端点 ====================
    
    /**
     * Agent 流式端点（推荐）
     * 
     * 使用模块化 Agent 架构处理请求，支持流式输出
     */
    @ApiOperation("Agent 流式端点：使用模块化架构处理请求（推荐）")
    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentStream(@RequestBody AgentRequest request) {
        log.info("🤖 [Agent] 收到流式请求: {}", request.getMessage());
        
        // 将 RxJava Flowable 转换为 Reactor Flux
        Flowable<String> flowable = agentService.askStream(request);
        return Flux.from(flowable);
    }
    
    /**
     * Agent 同步端点
     * 
     * 使用模块化 Agent 架构处理请求，等待完整响应
     */
    @ApiOperation("Agent 同步端点：使用模块化架构处理请求")
    @PostMapping("/agent/ask")
    public AgentResponse agentAsk(@RequestBody AgentRequest request) {
        log.info("🤖 [Agent] 收到同步请求: {}", request.getMessage());
        return agentService.ask(request);
    }
}

