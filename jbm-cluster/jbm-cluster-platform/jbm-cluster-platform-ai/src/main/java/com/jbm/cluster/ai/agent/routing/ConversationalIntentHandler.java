package com.jbm.cluster.ai.agent.routing;

import cn.hutool.core.util.StrUtil;
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
 * 对话型意图处理器
 * 
 * 处理不需要调用 API 的对话型请求，如：问候、自我介绍、闲聊等
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ConversationalIntentHandler implements IntentHandler {
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    /**
     * 对话型意图列表
     */
    private static final List<String> CONVERSATIONAL_INTENTS = Arrays.asList(
            "introduce_yourself",  // 自我介绍
            "greeting",            // 问候
            "goodbye",             // 告别
            "thank",               // 感谢
            "help",                // 求助
            "general_chat",        // 闲聊
            "unknown",             // 未知
            "general_query"        // 通用查询（置信度低时）
    );
    
    /**
     * 对话系统提示词
     */
    private static final String CONVERSATIONAL_PROMPT = """
            你是 JBM 系统的 AI 智能助手。
            
            你的能力：
            1. 通过调用系统 API 查询和操作真实的业务数据（用户、订单、库存、设备等）
            2. 回答关于系统功能和使用方法的问题
            3. 进行友好的对话交流
            
            回答原则：
            - 简洁、专业、友好
            - 突出你的数据查询能力
            - 鼓励用户尝试提问
            
            示例：
            用户："你好"
            回复："你好！我是 JBM 系统的 AI 智能助手。我可以帮您查询系统中的各种数据，比如用户信息、订单状态、库存情况等。有什么我可以帮您的吗？"
            
            用户："请介绍一下你自己"
            回复："我是 JBM 系统的 AI 智能助手。我可以通过调用系统接口来帮您：
            - 查询用户信息和在线状态
            - 查看订单详情和统计
            - 检查库存和物料情况
            - 获取设备运行数据
            - 以及更多系统功能
            
            您可以直接用自然语言问我问题，我会帮您找到并调用相关接口获取数据。试试问我'查询在线用户'或'系统有多少个接口'？"
            """;
    
    @Override
    public boolean canHandle(Intent intent) {
        if (intent == null || StrUtil.isEmpty(intent.getName())) {
            return false;
        }
        
        // 检查是否是对话型意图
        boolean isConversational = CONVERSATIONAL_INTENTS.stream()
                .anyMatch(ci -> intent.getName().toLowerCase().contains(ci));
        
        // 或者置信度低于 0.5 的通用查询
        if (!isConversational && intent.getConfidence() < 0.5) {
            isConversational = true;
        }
        
        return isConversational;
    }
    
    @Override
    public AgentContext handle(AgentContext context) {
        try {
            Intent intent = context.getIntent();
            
            log.info("💬 [Conversational Handler] 处理对话型意图: {}", intent.getName());
            
            // 直接使用 AI 生成对话回复（不调用 API）
            context.setStage(AgentContext.ProcessStage.RESPONSE_FORMATTING);
            
            String response = generateConversationalResponse(context.getUserQuery());
            
            // 将响应设置为 API 响应，这样 ResponseFormatter 可以处理
            // 但实际上 ResponseFormatter 会检测到不是 JSON，直接返回文本
            context.setApiResponse(response);
            context.setResponseStatusCode(200);
            context.markCompleted(true);
            
            log.info("✅ [Conversational Handler] 对话回复生成完成");
            
            return context;
            
        } catch (Exception e) {
            log.error("❌ [Conversational Handler] 处理失败: {}", e.getMessage(), e);
            context.setErrorMessage("对话处理失败: " + e.getMessage());
            context.markCompleted(false);
            return context;
        }
    }
    
    @Override
    public int getPriority() {
        return 10; // 高于默认处理器（0）
    }
    
    /**
     * 生成对话回复
     */
    private String generateConversationalResponse(String userQuery) {
        try {
            // 构建消息
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(CONVERSATIONAL_PROMPT)
                    .build());
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
                    .temperature(0.7f)  // 对话可以稍高温度
                    .maxTokens(500)
                    .build();
            
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            if (result != null && result.getOutput() != null && 
                result.getOutput().getChoices() != null && 
                !result.getOutput().getChoices().isEmpty()) {
                
                String response = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.info("🤖 [Conversational] AI 回复: {}", response);
                return response;
            }
            
            return "你好！我是 JBM 系统的 AI 智能助手，可以帮您查询系统数据。";
            
        } catch (Exception e) {
            log.error("❌ [Conversational] 生成回复失败: {}", e.getMessage(), e);
            return "你好！我是 JBM 系统的 AI 智能助手。";
        }
    }
}

