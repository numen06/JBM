package com.jbm.cluster.ai.agent.routing;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.jbm.cluster.ai.agent.model.AgentContext;
import com.jbm.cluster.ai.agent.model.Intent;
import com.jbm.cluster.ai.config.DashScopeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 直接回答意图处理器
 * 
 * 处理不需要调用 API 的意图，如：
 * - 问候（你好、hello）
 * - 自我介绍（介绍自己）
 * - 帮助（你能做什么）
 * - 闲聊
 * 
 * 优先级高于 DefaultIntentHandler
 * 
 * @author wesley
 */
@Slf4j
@Component
public class DirectAnswerIntentHandler implements IntentHandler {
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    /**
     * 不需要 API 的意图列表
     */
    private static final List<String> DIRECT_ANSWER_INTENTS = Arrays.asList(
            "introduce_yourself",
            "greeting",
            "hello",
            "help",
            "what_can_you_do",
            "thank",
            "goodbye",
            "chat",
            "general_query"
    );
    
    @Override
    public boolean canHandle(Intent intent) {
        if (intent == null || intent.getName() == null) {
            return false;
        }
        
        // 检查是否是直接回答类型的意图
        String intentName = intent.getName().toLowerCase();
        for (String pattern : DIRECT_ANSWER_INTENTS) {
            if (intentName.contains(pattern) || pattern.contains(intentName)) {
                return true;
            }
        }
        
        // 置信度低于 0.6 也可能需要直接回答
        if (intent.getConfidence() < 0.6) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public AgentContext handle(AgentContext context) {
        try {
            Intent intent = context.getIntent();
            String userQuery = context.getUserQuery();
            
            log.info("💬 [Direct Answer] 处理直接回答意图: {}", intent.getName());
            log.info("   用户问题: {}", userQuery);
            
            // 使用 AI 直接生成回复（不调用 API）
            String answer = generateDirectAnswer(userQuery);
            
            // 将回复存储到 context 的 apiResponse 中
            // 这样 ResponseFormatter 可以直接使用
            context.setApiResponse("{\"answer\": \"" + escapeJson(answer) + "\"}");
            context.setResponseStatusCode(200);
            context.markCompleted(true);
            
            log.info("✅ [Direct Answer] 生成回复完成");
            
            return context;
            
        } catch (Exception e) {
            log.error("❌ [Direct Answer] 处理失败: {}", e.getMessage(), e);
            context.setErrorMessage("处理失败: " + e.getMessage());
            context.markCompleted(false);
            return context;
        }
    }
    
    /**
     * 生成直接回答
     */
    private String generateDirectAnswer(String userQuery) {
        try {
            List<Message> messages = new ArrayList<>();
            
            // System Prompt
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("""
                            你是 JBM 系统的 AI 智能助手。
                            
                            当用户问候、要求自我介绍或询问帮助时，请友好、专业地回答。
                            
                            自我介绍要点：
                            - 你是 JBM 系统的 AI 助手
                            - 可以帮助用户查询和管理系统数据
                            - 支持用户信息、订单、库存等业务查询
                            - 使用自然语言交互，简单易用
                            
                            回答要求：
                            - 简洁、友好、专业
                            - 中文回复
                            - 不要过于冗长
                            """)
                    .build());
            
            // 用户问题
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userQuery)
                    .build());
            
            // 调用 AI
            GenerationParam param = GenerationParam.builder()
                    .apiKey(dashScopeConfig.getApiKey())
                    .model(dashScopeConfig.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(0.7f)  // 稍高温度，回复更自然
                    .maxTokens(500)
                    .build();
            
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            if (result != null && result.getOutput() != null && 
                result.getOutput().getChoices() != null && 
                !result.getOutput().getChoices().isEmpty()) {
                
                return result.getOutput().getChoices().get(0).getMessage().getContent();
            }
            
            return "你好！我是 JBM AI 智能助手，很高兴为您服务。";
            
        } catch (Exception e) {
            log.error("❌ [Direct Answer] 生成回复失败: {}", e.getMessage(), e);
            return "你好！我是 JBM AI 智能助手，很高兴为您服务。";
        }
    }
    
    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    @Override
    public int getPriority() {
        return 100; // 高优先级，优先于 DefaultIntentHandler
    }
}

