package com.jbm.cluster.ai.agent;

import com.jbm.cluster.ai.agent.model.AgentRequest;
import com.jbm.cluster.ai.agent.model.AgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 服务集成测试
 * 
 * 验证端到端流程
 * 
 * @author wesley
 */
@Slf4j
@SpringBootTest
@Disabled("需要真实的 AI API Key 才能运行")
public class AgentServiceIntegrationTest {
    
    @Autowired(required = false)
    private AgentService agentService;
    
    /**
     * 测试简单查询
     */
    @Test
    public void testSimpleQuery() {
        if (agentService == null) {
            log.warn("AgentService 未注入，跳过测试");
            return;
        }
        
        AgentRequest request = AgentRequest.builder()
                .message("现在几点？")
                .build();
        
        AgentResponse response = agentService.ask(request);
        
        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertNotNull(response.getMessage());
        assertTrue(response.isSuccess());
        
        log.info("响应: {}", response.getMessage());
        log.info("耗时: {}ms", response.getDurationMs());
    }
    
    /**
     * 测试流式输出
     */
    @Test
    public void testStreamQuery() {
        if (agentService == null) {
            log.warn("AgentService 未注入，跳过测试");
            return;
        }
        
        AgentRequest request = AgentRequest.builder()
                .message("系统有多少个接口？")
                .build();
        
        StringBuilder result = new StringBuilder();
        
        agentService.askStream(request)
                .blockingForEach(event -> {
                    log.info("收到事件: {}", event);
                    result.append(event);
                });
        
        assertFalse(result.toString().isEmpty());
        log.info("完整响应长度: {} 字符", result.length());
    }
    
    /**
     * 测试带参数的查询
     */
    @Test
    public void testParameterizedQuery() {
        if (agentService == null) {
            log.warn("AgentService 未注入，跳过测试");
            return;
        }
        
        AgentRequest request = AgentRequest.builder()
                .message("查询用户ID为123的信息")
                .build();
        
        AgentResponse response = agentService.ask(request);
        
        assertNotNull(response);
        assertNotNull(response.getMessage());
        
        // 验证意图识别
        if (response.getIntent() != null) {
            log.info("识别的意图: {}", response.getIntent().getName());
            log.info("置信度: {}", response.getIntent().getConfidence());
        }
        
        // 验证 API 调用
        if (response.getApiCalled() != null) {
            log.info("调用的 API: {}", response.getApiCalled());
        }
        
        log.info("响应: {}", response.getMessage());
    }
}

