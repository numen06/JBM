package com.jbm.cluster.ai.agent.execution;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.jbm.cluster.ai.agent.model.AgentContext;
import com.jbm.cluster.ai.config.DashScopeConfig;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 响应格式化器
 * 
 * 使用 AI 将 API 返回的 JSON 数据转换为自然语言
 * 支持流式输出
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ResponseFormatter {
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    /**
     * 格式化 System Prompt
     */
    private static final String FORMAT_SYSTEM_PROMPT = """
            你是一个专业的数据分析助手，负责将 API 返回的 JSON 数据转换为用户友好的自然语言。
            
            要求：
            1. 分析 JSON 数据，提取关键信息
            2. 用简洁、准确、易懂的中文回复
            3. 如果数据是列表，合理组织展示
            4. 如果数据包含错误信息，友好地告知用户
            5. 保持专业和礼貌
            6. 不要输出原始 JSON
            
            示例：
            
            输入：{"userId": "123", "name": "张三", "email": "zhangsan@example.com", "status": "active"}
            输出：用户ID 123 的信息如下：
            - 姓名：张三
            - 邮箱：zhangsan@example.com
            - 状态：正常
            
            输入：{"users": [{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}], "total": 2}
            输出：找到 2 个用户：
            1. Alice (ID: 1)
            2. Bob (ID: 2)
            
            输入：{"error": "User not found"}
            输出：抱歉，未找到该用户。
            """;
    
    /**
     * 格式化响应（流式）
     * 
     * @param context Agent 上下文
     * @return 流式文本输出
     */
    public Flowable<String> formatStream(AgentContext context) {
        return Flowable.create(emitter -> {
            try {
                String apiResponse = context.getApiResponse();
                String userQuery = context.getUserQuery();
                
                if (StrUtil.isEmpty(apiResponse)) {
                    emitter.onNext("抱歉，没有获取到数据。");
                    emitter.onComplete();
                    return;
                }
                
                // 检查是否是对话型回复（非 JSON 格式）
                if (!isJson(apiResponse)) {
                    log.info("💬 [Response Formatter] 检测到对话型回复，直接流式输出");
                    
                    // 逐字输出（模拟流式效果）
                    for (int i = 0; i < apiResponse.length(); i++) {
                        emitter.onNext(String.valueOf(apiResponse.charAt(i)));
                    }
                    
                    emitter.onComplete();
                    return;
                }
                
                // 检查是否是直接回答（不需要格式化）
                if (apiResponse.startsWith("{\"answer\":")) {
                    // 提取 answer 字段并流式输出
                    try {
                        cn.hutool.json.JSONObject json = JSONUtil.parseObj(apiResponse);
                        String answer = json.getStr("answer");
                        
                        if (answer != null) {
                            log.info("📝 [Response Formatter] 直接回答模式，流式输出");
                            
                            // 逐字输出（模拟流式效果）
                            for (int i = 0; i < answer.length(); i++) {
                                emitter.onNext(String.valueOf(answer.charAt(i)));
                                
                                // 可选：添加微小延迟以模拟打字效果
                                // Thread.sleep(10);
                            }
                            
                            emitter.onComplete();
                            return;
                        }
                    } catch (Exception e) {
                        log.warn("解析直接回答失败，使用 AI 格式化: {}", e.getMessage());
                    }
                }
                
                log.info("📝 [Response Formatter] 开始格式化响应（流式）");
                
                // 构建消息
                List<Message> messages = new ArrayList<>();
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(FORMAT_SYSTEM_PROMPT)
                        .build());
                
                // 构建用户消息：包含原始问题和 API 数据
                String userMessage = String.format("""
                        用户问题：%s
                        
                        API 返回的数据：
                        %s
                        
                        请将上述数据转换为友好的自然语言回复。
                        """, userQuery, apiResponse);
                
                messages.add(Message.builder()
                        .role(Role.USER.getValue())
                        .content(userMessage)
                        .build());
                
                // 构建生成参数
                GenerationParam param = GenerationParam.builder()
                        .apiKey(dashScopeConfig.getApiKey())
                        .model(dashScopeConfig.getModel())
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .temperature(0.3f)  // 稍高温度，使回复更自然
                        .maxTokens(1000)
                        .incrementalOutput(true)  // 关键：启用增量输出
                        .build();
                
                // 流式调用 AI
                Generation gen = new Generation();
                Flowable<GenerationResult> resultFlowable = gen.streamCall(param);
                
                resultFlowable.subscribe(
                    // onNext: 处理每个响应片段
                    result -> {
                        if (result.getOutput() != null && 
                            result.getOutput().getChoices() != null &&
                            !result.getOutput().getChoices().isEmpty()) {
                            
                            String content = result.getOutput().getChoices().get(0)
                                    .getMessage().getContent();
                            
                            if (content != null && !content.isEmpty()) {
                                emitter.onNext(content);
                            }
                        }
                    },
                    // onError: 处理错误
                    error -> {
                        log.error("❌ [Response Formatter] 格式化失败: {}", error.getMessage());
                        emitter.onError(error);
                    },
                    // onComplete: 完成
                    () -> {
                        log.info("✅ [Response Formatter] 格式化完成");
                        emitter.onComplete();
                    }
                );
                
            } catch (Exception e) {
                log.error("❌ [Response Formatter] 格式化异常: {}", e.getMessage(), e);
                emitter.onNext("抱歉，处理响应时出现错误：" + e.getMessage());
                emitter.onComplete();
            }
        }, BackpressureStrategy.BUFFER);
    }
    
    /**
     * 格式化响应（同步）
     * 
     * @param context Agent 上下文
     * @return 格式化后的文本
     */
    public String format(AgentContext context) {
        StringBuilder result = new StringBuilder();
        
        try {
            formatStream(context)
                    .blockingForEach(result::append);
        } catch (Exception e) {
            log.error("❌ [Response Formatter] 同步格式化失败: {}", e.getMessage(), e);
            return "抱歉，处理响应时出现错误：" + e.getMessage();
        }
        
        return result.toString();
    }
    
    /**
     * 判断字符串是否是 JSON 格式
     */
    private boolean isJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = text.trim();
        
        // JSON 对象或数组
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                JSONUtil.parse(trimmed);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }
}

