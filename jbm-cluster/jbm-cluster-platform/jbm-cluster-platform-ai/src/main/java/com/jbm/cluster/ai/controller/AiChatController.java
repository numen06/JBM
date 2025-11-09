package com.jbm.cluster.ai.controller;

import com.jbm.cluster.ai.model.ChatRequest;
import com.jbm.cluster.ai.model.ChatResponse;
import com.jbm.cluster.ai.service.AiChatService;
import com.jbm.cluster.ai.service.ApiMetadataCollector;
import io.reactivex.Flowable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
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
    private AiChatService aiChatService;
    
    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    /**
     * 聊天接口（普通模式）
     */
    @ApiOperation("发送消息并获取 AI 回复（普通模式，等待完整响应）")
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("📨 收到聊天请求: {}", request.getMessage());
        return aiChatService.chat(request);
    }
    
    /**
     * 聊天接口（流式模式）
     * 使用 Server-Sent Events (SSE) 实现流式响应
     * AI 逐字生成，用户实时看到回复，体验更好
     */
    @ApiOperation("发送消息并获取 AI 流式回复（推荐使用，响应更快）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("📨 [流式] 收到聊天请求: {}", request.getMessage());
        
        // 将 RxJava Flowable 转换为 Reactor Flux
        Flowable<String> flowable = aiChatService.chatStream(request);
        return Flux.from(flowable);
    }
    
    /**
     * 清除会话
     */
    @ApiOperation("清除指定会话的历史记录")
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        aiChatService.clearSession(sessionId);
        return Map.of("success", true, "message", "会话已清除");
    }
    
    /**
     * 获取可用函数列表
     */
    @ApiOperation("获取所有可用的 API 函数列表")
    @GetMapping("/functions")
    public List<Map<String, Object>> listFunctions() {
        return aiChatService.listAvailableFunctions();
    }
    
    /**
     * 健康检查
     */
    @ApiOperation("健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        int apiCount = apiMetadataCollector.getAllApis().size();
        return Map.of(
                "status", "ok",
                "service", "jbm-ai",
                "message", "AI 服务运行正常",
                "apiCount", apiCount
        );
    }
    
    /**
     * 强制刷新 API 元数据（清除缓存并重新收集）
     */
    @ApiOperation("强制刷新 API 元数据缓存")
    @PostMapping("/refresh")
    public Map<String, Object> refreshApiMetadata() {
        log.info("📡 收到刷新 API 元数据请求");
        apiMetadataCollector.forceRefresh();
        return Map.of(
                "success", true,
                "message", "API 元数据刷新任务已启动（后台执行）"
        );
    }
    
    /**
     * 清除 API 元数据缓存
     */
    @ApiOperation("清除 API 元数据缓存文件")
    @DeleteMapping("/cache")
    public Map<String, Object> clearCache() {
        log.info("🗑️ 收到清除缓存请求");
        apiMetadataCollector.clearCache();
        return Map.of(
                "success", true,
                "message", "缓存已清除"
        );
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
        
        return Map.of(
                "totalApis", totalApis,
                "serviceCount", (int) serviceCount,
                "functionsCount", aiChatService.listAvailableFunctions().size(),
                "cacheEnabled", true
        );
    }
}

