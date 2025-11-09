package com.jbm.cluster.ai.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
import com.jbm.cluster.ai.config.DashScopeConfig;
import com.jbm.cluster.ai.model.ChatRequest;
import com.jbm.cluster.ai.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 聊天服务
 * 
 * 核心架构：
 * 1. AI 负责理解用户意图
 * 2. AI 通过 Function Calling 决定调用哪个接口
 * 3. AI 提供函数参数
 * 4. 系统通过 JbmRequestTemplate 安全地执行内部调用
 * 5. AI 解析返回的 JSON 数据
 * 6. AI 生成用户友好的回复
 * 
 * 安全机制：
 * - AI 不直接访问系统，所有调用通过 JbmRequestTemplate
 * - 使用 Feign 内部协议，自动携带认证信息
 * - 符合 JBM 安全体系和权限控制
 * 
 * @author wesley
 */
@Service
@Slf4j
public class AiChatService {

    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    @Autowired
    private ApiFunctionRegistry apiFunctionRegistry;
    
    /**
     * 会话历史，key 为 sessionId
     */
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();
    
    /**
     * 系统提示词
     * 
     * 明确 AI 的职责：
     * - 理解意图：分析用户真正想要什么
     * - 选择接口：从可用函数中选择最合适的
     * - 提取参数：从用户消息中提取必要的参数
     * - 解析数据：理解函数返回的 JSON 数据
     * - 生成回复：将数据转化为自然语言
     */
    private static final String SYSTEM_PROMPT = """
            你是 JBM 智能助手，专门帮助用户查询和操作 JBM 系统中的数据。
            
            你的能力和职责：
            1. 理解用户意图 - 分析用户真正想要什么
            2. 选择合适的函数 - 从可用的 API 函数中选择最匹配的
            3. 提取参数 - 从用户消息中提取函数需要的参数
            4. 调用函数获取数据 - 使用 Function Calling 机制
            5. 解析返回的 JSON 数据 - 理解数据含义
            6. 生成友好的回复 - 将数据转化为用户容易理解的语言
            
            重要说明：
            - 函数调用通过系统内部的 JbmRequestTemplate 安全执行，你无法直接访问系统
            - 你只能调用已注册的函数，不要尝试其他操作
            - 函数会返回原始的 JSON 数据，你需要自己解析并提取关键信息
            - 如果没有合适的函数，请礼貌地告知用户当前无法完成该操作
            - 保持回复简洁、准确、友好
            """;
    
    /**
     * 处理聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        
        try {
            if (StrUtil.isEmpty(dashScopeConfig.getApiKey()) || 
                "sk-default".equals(dashScopeConfig.getApiKey())) {
                response.setError("AI 模型未配置，请设置 DASHSCOPE_API_KEY 环境变量");
                response.setMessage("抱歉，AI 服务暂时不可用。请联系管理员配置 API Key。");
                return response;
            }
            
            // 生成或使用已有的 sessionId
            String sessionId = StrUtil.isNotEmpty(request.getSessionId()) ? 
                    request.getSessionId() : IdUtil.simpleUUID();
            response.setSessionId(sessionId);
            
            // 获取会话历史
            List<Message> messages = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
            
            // 添加系统消息（仅第一次）
            if (messages.isEmpty()) {
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(SYSTEM_PROMPT)
                        .build());
            }
            
            // 添加用户消息
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(request.getMessage())
                    .build());
            
            log.info("💬 用户消息: {}", request.getMessage());
            
            // 构建生成参数
            GenerationParam.GenerationParamBuilder paramBuilder = GenerationParam.builder()
                    .apiKey(dashScopeConfig.getApiKey())
                    .model(dashScopeConfig.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .temperature(dashScopeConfig.getTemperature().floatValue())
                    .maxTokens(dashScopeConfig.getMaxTokens())
                    .enableSearch(dashScopeConfig.getEnableSearch());
            
            // 添加工具（函数）
            if (request.isEnableFunctions()) {
                List<ToolFunction> tools = buildTools();
                if (!tools.isEmpty()) {
                    paramBuilder.tools(tools);
                    log.debug("📋 注册了 {} 个函数供 AI 调用", tools.size());
                }
            }
            
            GenerationParam param = paramBuilder.build();
            
            // 调用 AI 模型
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 处理响应
            if (result != null && result.getOutput() != null && 
                result.getOutput().getChoices() != null && 
                !result.getOutput().getChoices().isEmpty()) {
                
                Message assistantMessage = result.getOutput().getChoices().get(0).getMessage();
                
                // 检查是否有函数调用
                if (assistantMessage.getToolCalls() != null && 
                    !assistantMessage.getToolCalls().isEmpty()) {
                    
                    log.info("🎯 [AI 决策] AI 分析后决定调用函数");
                    
                    // 添加助手消息（包含函数调用请求）
                    messages.add(assistantMessage);
                    
                    // 执行所有函数调用
                    for (ToolCallBase toolCall : assistantMessage.getToolCalls()) {
                        if (toolCall instanceof ToolCallFunction) {
                            ToolCallFunction funcCall = (ToolCallFunction) toolCall;
                            String functionName = funcCall.getFunction().getName();
                            String arguments = funcCall.getFunction().getArguments();
                            
                            log.info("📞 [AI 选择] 函数: {}, 参数: {}", functionName, arguments);
                            log.info("   → AI 已决定调用哪个接口和传递什么参数");
                            log.info("   → 系统将通过 JbmRequestTemplate 安全执行");
                            
                            // 执行函数（内部通过 JbmRequestTemplate 调用）
                            String functionResult = apiFunctionRegistry.executeFunction(
                                    functionName, 
                                    JSONUtil.parseObj(arguments));
                            
                            log.info("✅ [系统返回] 数据已返回给 AI: {}", 
                                    functionResult.length() > 200 ? 
                                    functionResult.substring(0, 200) + "..." : functionResult);
                            log.info("   → AI 将解析这些数据并生成回复");
                            
                            // 添加函数结果消息
                            messages.add(Message.builder()
                                    .role(Role.TOOL.getValue())
                                    .content(functionResult)
                                    .name(functionName)
                                    .build());
                            
                            response.setFunctionCalled(true);
                            response.setFunctionName(functionName);
                            response.setFunctionResult(functionResult);
                        }
                    }
                    
                    // 再次调用 AI 让它解析数据并生成最终回复
                    log.info("🤖 [AI 处理] 让 AI 解析函数返回的数据...");
                    param = paramBuilder.messages(messages).tools(null).build();
                    result = gen.call(param);
                    
                    if (result != null && result.getOutput() != null && 
                        result.getOutput().getChoices() != null && 
                        !result.getOutput().getChoices().isEmpty()) {
                        
                        Message finalMessage = result.getOutput().getChoices().get(0).getMessage();
                        String aiMessage = finalMessage.getContent();
                        response.setMessage(aiMessage);
                        
                        // 添加最终助手消息到历史
                        messages.add(finalMessage);
                        
                        log.info("🤖 [AI 回复] AI 已解析数据并生成回复: {}", aiMessage);
                        log.info("   → AI 将原始 JSON 转化为了用户友好的自然语言");
                    }
                } else {
                    // 没有函数调用，直接返回回复
                    String aiMessage = assistantMessage.getContent();
                    response.setMessage(aiMessage);
                    
                    // 添加助手消息到历史
                    messages.add(assistantMessage);
                    
                    log.info("🤖 AI 回复: {}", aiMessage);
                }
            } else {
                response.setMessage("抱歉，我现在无法回答您的问题。");
            }
            
        } catch (Exception e) {
            log.error("❌ 聊天处理失败", e);
            response.setError(e.getMessage());
            response.setMessage("抱歉，处理您的请求时出现了错误：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 构建工具列表（函数）
     */
    private List<ToolFunction> buildTools() {
        Collection<ApiFunctionRegistry.FunctionDefinition> functions = 
                apiFunctionRegistry.getAllFunctions();
        
        return functions.stream()
                .map(this::convertToToolFunction)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换函数定义为 ToolFunction
     */
    private ToolFunction convertToToolFunction(ApiFunctionRegistry.FunctionDefinition functionDef) {
        // 将 Map 转换为 Gson JsonObject
        Gson gson = new Gson();
        String jsonStr = JSONUtil.toJsonStr(functionDef.getParameters());
        com.google.gson.JsonObject jsonObject = gson.fromJson(jsonStr, com.google.gson.JsonObject.class);
        
        FunctionDefinition funcDef = FunctionDefinition.builder()
                .name(functionDef.getName())
                .description(functionDef.getDescription())
                .parameters(jsonObject)
                .build();
        
        return ToolFunction.builder()
                .type("function")
                .function(funcDef)
                .build();
    }
    
    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        if (StrUtil.isNotEmpty(sessionId)) {
            sessionHistory.remove(sessionId);
            log.info("🗑️ 清除会话: {}", sessionId);
        }
    }
    
    /**
     * 获取所有可用的函数列表
     */
    public List<Map<String, Object>> listAvailableFunctions() {
        return apiFunctionRegistry.getAllFunctions().stream()
                .map(func -> {
                    Map<String, Object> funcInfo = new HashMap<>();
                    funcInfo.put("name", func.getName());
                    funcInfo.put("description", func.getDescription());
                    funcInfo.put("parameters", func.getParameters());
                    return funcInfo;
                })
                .collect(Collectors.toList());
    }
}
