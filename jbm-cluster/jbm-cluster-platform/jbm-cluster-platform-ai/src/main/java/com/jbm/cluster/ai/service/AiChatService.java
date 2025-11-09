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
import com.alibaba.dashscope.common.ResultCallback;
import io.reactivex.Flowable;
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
            
            // 检查是否是询问能力的消息
            String userMessage = request.getMessage();
            boolean isAskingCapabilities = isAskingAboutCapabilities(userMessage);
            
            // 如果用户询问能力，注入可用 API 列表
            if (isAskingCapabilities && request.isEnableFunctions()) {
                String apiListContext = buildApiListContext();
                userMessage = userMessage + "\n\n" + apiListContext;
                log.info("🔍 检测到用户询问能力，已注入 API 列表上下文");
            }
            
            // 添加用户消息
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
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
     * 流式处理聊天请求
     * 使用 SSE 方式逐字返回，提升用户体验
     * 
     * @param request 聊天请求
     * @return Flowable 流式响应
     */
    public Flowable<String> chatStream(ChatRequest request) {
        return Flowable.create(emitter -> {
            try {
                if (StrUtil.isEmpty(dashScopeConfig.getApiKey()) || 
                    "sk-default".equals(dashScopeConfig.getApiKey())) {
                    emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                            "error", "AI 模型未配置",
                            "message", "请设置 DASHSCOPE_API_KEY"
                    )) + "\n\n");
                    emitter.onComplete();
                    return;
                }
                
                // 生成或使用已有的 sessionId
                String sessionId = StrUtil.isNotEmpty(request.getSessionId()) ? 
                        request.getSessionId() : IdUtil.simpleUUID();
                
                // 获取会话历史
                List<Message> messages = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
                
                // 添加系统消息（仅第一次）
                if (messages.isEmpty()) {
                    messages.add(Message.builder()
                            .role(Role.SYSTEM.getValue())
                            .content(SYSTEM_PROMPT)
                            .build());
                }
                
                // 检查是否询问能力
                String userMessage = request.getMessage();
                boolean isAskingCapabilities = isAskingAboutCapabilities(userMessage);
                
                if (isAskingCapabilities && request.isEnableFunctions()) {
                    String apiListContext = buildApiListContext();
                    userMessage = userMessage + "\n\n" + apiListContext;
                    log.info("🔍 [流式] 检测到用户询问能力，已注入 API 列表");
                }
                
                // 添加用户消息
                messages.add(Message.builder()
                        .role(Role.USER.getValue())
                        .content(userMessage)
                        .build());
                
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("💬 [流式对话] 用户消息: {}", request.getMessage());
                log.info("🔑 [流式对话] 会话ID: {}", sessionId);
                log.info("⚙️  [流式对话] Function Calling: {}", 
                        request.isEnableFunctions() ? "启用" : "禁用");
                if (isAskingCapabilities) {
                    log.info("🔍 [流式对话] 已注入 API 功能列表供 AI 参考");
                }
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                // 发送 sessionId
                emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                        "type", "sessionId",
                        "sessionId", sessionId
                )) + "\n\n");
                
                // 构建生成参数
                GenerationParam.GenerationParamBuilder paramBuilder = GenerationParam.builder()
                        .apiKey(dashScopeConfig.getApiKey())
                        .model(dashScopeConfig.getModel())
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .topP(0.8)
                        .temperature(dashScopeConfig.getTemperature().floatValue())
                        .maxTokens(dashScopeConfig.getMaxTokens())
                        .enableSearch(dashScopeConfig.getEnableSearch())
                        .incrementalOutput(true);  // 开启增量输出
                
                // 添加工具
                if (request.isEnableFunctions()) {
                    List<ToolFunction> tools = buildTools();
                    if (!tools.isEmpty()) {
                        paramBuilder.tools(tools);
                    }
                }
                
                GenerationParam param = paramBuilder.build();
                Generation gen = new Generation();
                
                // 用于收集完整消息
                StringBuilder fullMessage = new StringBuilder();
                List<ToolCallFunction> toolCalls = new ArrayList<>();
                
                log.info("🤖 [流式对话] AI 开始生成回复...");
                
                // 流式调用
                gen.streamCall(param, new ResultCallback<GenerationResult>() {
                    private boolean firstChunk = true;
                    
                    @Override
                    public void onEvent(GenerationResult result) {
                        if (result.getOutput() != null && 
                            result.getOutput().getChoices() != null &&
                            !result.getOutput().getChoices().isEmpty()) {
                            
                            Message msg = result.getOutput().getChoices().get(0).getMessage();
                            
                            // 处理文本内容
                            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                                if (firstChunk) {
                                    log.info("📝 [流式对话] AI 回复开始 ↓");
                                    System.out.print("🤖 AI: ");
                                    firstChunk = false;
                                }
                                
                                fullMessage.append(msg.getContent());
                                
                                // 实时打印到控制台
                                System.out.print(msg.getContent());
                                System.out.flush();
                                
                                // 发送流式文本
                                emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                                        "type", "text",
                                        "content", msg.getContent()
                                )) + "\n\n");
                            }
                            
                            // 收集函数调用
                            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                                for (var toolCall : msg.getToolCalls()) {
                                    if (toolCall instanceof ToolCallFunction) {
                                        toolCalls.add((ToolCallFunction) toolCall);
                                    }
                                }
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        System.out.println();  // 换行
                        log.error("❌ [流式对话] AI 调用失败: {}", e.getMessage());
                        emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                                "type", "error",
                                "message", e.getMessage()
                        )) + "\n\n");
                        emitter.onComplete();
                    }
                    
                    @Override
                    public void onComplete() {
                        try {
                            System.out.println();  // AI 回复结束换行
                            
                            // 如果有函数调用
                            if (!toolCalls.isEmpty()) {
                                log.info("");
                                log.info("🎯 [流式对话] AI 决定调用 {} 个函数", toolCalls.size());
                                
                                // 添加助手消息
                                Message assistantMsg = Message.builder()
                                        .role(Role.ASSISTANT.getValue())
                                        .content(fullMessage.toString())
                                        .toolCalls(new ArrayList<>(toolCalls))
                                        .build();
                                messages.add(assistantMsg);
                                
                                // 执行函数调用
                                for (ToolCallFunction funcCall : toolCalls) {
                                    String functionName = funcCall.getFunction().getName();
                                    String arguments = funcCall.getFunction().getArguments();
                                    
                                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                    log.info("📞 [流式对话] AI 选择调用函数: {}", functionName);
                                    log.info("📦 [流式对话] 原始参数: {}", arguments);
                                    
                                    try {
                                        // 验证 arguments 是否为空或 null
                                        if (arguments == null || arguments.trim().isEmpty()) {
                                            arguments = "{}";
                                            log.info("⚠️  [流式对话] 参数为空，使用默认空对象");
                                        }
                                        
                                        // 验证 JSON 格式
                                        cn.hutool.json.JSONObject argsJson;
                                        try {
                                            argsJson = JSONUtil.parseObj(arguments);
                                            log.info("✅ [流式对话] 参数解析成功: {}", argsJson);
                                        } catch (Exception parseEx) {
                                            log.error("❌ [流式对话] 参数 JSON 解析失败: {}", parseEx.getMessage());
                                            log.error("   原始参数: {}", arguments);
                                            // 使用空对象
                                            argsJson = new cn.hutool.json.JSONObject();
                                        }
                                        
                                        // 通知用户正在调用函数
                                        emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                                                "type", "functionCall",
                                                "functionName", functionName,
                                                "arguments", arguments
                                        )) + "\n\n");
                                        
                                        // 执行函数
                                        log.info("🔄 [流式对话] 正在执行函数调用...");
                                        String funcResult = apiFunctionRegistry.executeFunction(
                                                functionName, 
                                                argsJson);
                                        
                                        log.info("✅ [流式对话] 函数调用完成");
                                        log.info("📊 [流式对话] 返回数据: {}", 
                                                funcResult.length() > 200 ? 
                                                funcResult.substring(0, 200) + "..." : funcResult);
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        // 添加函数结果
                                        messages.add(Message.builder()
                                                .role(Role.TOOL.getValue())
                                                .content(funcResult)
                                                .name(functionName)
                                                .build());
                                        
                                    } catch (Exception funcEx) {
                                        log.error("❌ [流式对话] 函数调用异常: {}", funcEx.getMessage());
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        // 添加错误信息作为函数结果
                                        String errorResult = JSONUtil.toJsonStr(Map.of(
                                                "error", true,
                                                "message", "函数调用失败: " + funcEx.getMessage()
                                        ));
                                        
                                        messages.add(Message.builder()
                                                .role(Role.TOOL.getValue())
                                                .content(errorResult)
                                                .name(functionName)
                                                .build());
                                        
                                        // 通知用户错误
                                        emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                                                "type", "error",
                                                "message", "函数 " + functionName + " 调用失败: " + funcEx.getMessage()
                                        )) + "\n\n");
                                    }
                                }
                                
                                // 再次调用 AI 生成最终回复（流式）
                                log.info("");
                                log.info("🤖 [流式对话] AI 开始解析数据并生成最终回复...");
                                
                                GenerationParam finalParam = paramBuilder
                                        .messages(messages)
                                        .tools(null)
                                        .build();
                                
                                StringBuilder finalMsg = new StringBuilder();
                                
                                gen.streamCall(finalParam, new ResultCallback<GenerationResult>() {
                                    private boolean firstChunk = true;
                                    
                                    @Override
                                    public void onEvent(GenerationResult result) {
                                        if (result.getOutput() != null && 
                                            result.getOutput().getChoices() != null &&
                                            !result.getOutput().getChoices().isEmpty()) {
                                            
                                            String content = result.getOutput().getChoices().get(0)
                                                    .getMessage().getContent();
                                            if (content != null) {
                                                if (firstChunk) {
                                                    log.info("📝 [流式对话] AI 最终回复开始 ↓");
                                                    System.out.print("🤖 AI: ");
                                                    firstChunk = false;
                                                }
                                                
                                                finalMsg.append(content);
                                                
                                                // 实时打印
                                                System.out.print(content);
                                                System.out.flush();
                                                
                                                emitter.onNext("data: " + JSONUtil.toJsonStr(Map.of(
                                                        "type", "text",
                                                        "content", content
                                                )) + "\n\n");
                                            }
                                        }
                                    }
                                    
                                    @Override
                                    public void onError(Exception e) {
                                        System.out.println();  // 换行
                                        log.error("❌ [流式对话] 最终回复生成失败: {}", e.getMessage());
                                        emitter.onError(e);
                                    }
                                    
                                    @Override
                                    public void onComplete() {
                                        System.out.println();  // 换行
                                        
                                        // 添加最终消息到历史
                                        messages.add(Message.builder()
                                                .role(Role.ASSISTANT.getValue())
                                                .content(finalMsg.toString())
                                                .build());
                                        
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        log.info("✅ [流式对话] AI 回复完成");
                                        log.info("📝 [流式对话] 完整内容: {}", finalMsg.toString());
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        emitter.onNext("data: [DONE]\n\n");
                                        emitter.onComplete();
                                    }
                                });
                                
                            } else {
                                // 没有函数调用，直接完成
                                log.info("");
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                log.info("✅ [流式对话] AI 回复完成（无函数调用）");
                                log.info("📝 [流式对话] 完整内容: {}", fullMessage.toString());
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                
                                messages.add(Message.builder()
                                        .role(Role.ASSISTANT.getValue())
                                        .content(fullMessage.toString())
                                        .build());
                                
                                emitter.onNext("data: [DONE]\n\n");
                                emitter.onComplete();
                            }
                        } catch (Exception e) {
                            System.out.println();  // 换行
                            log.error("❌ [流式对话] 处理失败: {}", e.getMessage());
                            emitter.onError(e);
                        }
                    }
                });
                
            } catch (Exception e) {
                log.error("❌ [流式] 初始化失败", e);
                emitter.onError(e);
            }
        }, io.reactivex.BackpressureStrategy.BUFFER);
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
    
    /**
     * 判断用户是否在询问系统能力
     */
    private boolean isAskingAboutCapabilities(String message) {
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("能做什么") ||
               lowerMsg.contains("可以做什么") ||
               lowerMsg.contains("有什么功能") ||
               lowerMsg.contains("能帮我") ||
               lowerMsg.contains("可以帮我") ||
               lowerMsg.contains("能查询") ||
               lowerMsg.contains("可以查询") ||
               lowerMsg.contains("有哪些") ||
               lowerMsg.contains("what can you do") ||
               lowerMsg.contains("what can i");
    }
    
    /**
     * 构建 API 列表上下文
     */
    private String buildApiListContext() {
        Collection<ApiFunctionRegistry.FunctionDefinition> functions = 
                apiFunctionRegistry.getAllFunctions();
        
        if (functions.isEmpty()) {
            return "\n[系统提示：当前暂无可用的 API 函数]";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("\n\n[系统提示：以下是当前可用的 API 功能列表]\n");
        context.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        // 按服务分组
        Map<String, List<ApiFunctionRegistry.FunctionDefinition>> groupedByService = new HashMap<>();
        
        for (ApiFunctionRegistry.FunctionDefinition func : functions) {
            String desc = func.getDescription();
            // 从描述中提取服务名 [服务: xxx]
            String serviceName = "其他";
            int serviceStart = desc.indexOf("[服务: ");
            if (serviceStart >= 0) {
                int serviceEnd = desc.indexOf("]", serviceStart);
                if (serviceEnd > serviceStart) {
                    serviceName = desc.substring(serviceStart + 5, serviceEnd);
                }
            }
            
            groupedByService.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(func);
        }
        
        // 生成列表（最多显示前 30 个，避免上下文过长）
        int count = 0;
        int maxFunctions = 30;
        
        for (Map.Entry<String, List<ApiFunctionRegistry.FunctionDefinition>> entry : groupedByService.entrySet()) {
            if (count >= maxFunctions) {
                break;
            }
            
            context.append(String.format("\n【%s】\n", entry.getKey()));
            
            for (ApiFunctionRegistry.FunctionDefinition func : entry.getValue()) {
                if (count >= maxFunctions) {
                    break;
                }
                
                String cleanDesc = func.getDescription().replaceAll("\\[服务:.*?\\]", "").trim();
                context.append(String.format("  • %s\n", cleanDesc));
                count++;
            }
        }
        
        if (functions.size() > maxFunctions) {
            context.append(String.format("\n...还有 %d 个功能未列出\n", functions.size() - maxFunctions));
        }
        
        context.append("\n总计: ").append(functions.size()).append(" 个可用功能");
        context.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        context.append("\n请根据以上功能列表回答用户的问题。");
        
        return context.toString();
    }
}
