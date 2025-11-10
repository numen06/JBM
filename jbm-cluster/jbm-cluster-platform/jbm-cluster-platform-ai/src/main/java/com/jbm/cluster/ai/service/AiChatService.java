package com.jbm.cluster.ai.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
 * AI 聊天服务（旧架构 - 已废弃）
 * 
 * ⚠️ 已废弃：请使用新的 Agent 模块化架构 (AgentService)
 * 
 * 旧架构问题：
 * 1. Function Calling 流式输出时可能出现 toolCalls 数据不完整
 * 2. 函数名可能为 null，导致调用失败
 * 3. AI 可能输出函数调用的文本形式而不是真正执行
 * 4. 单一服务，难以扩展和维护
 * 
 * 新架构优势：
 * 1. 模块化设计，职责单一
 * 2. 不依赖 Function Calling 的流式问题
 * 3. 清晰的阶段进度反馈
 * 4. 更稳定可靠
 * 5. 易于测试和调试
 * 
 * 迁移指南：
 * - 使用 AgentService.askStream() 替代 chatStream()
 * - 使用 AgentService.ask() 替代 chat()
 * - 前端切换到 /ai/agent/stream 端点
 * 
 * @author wesley
 * @deprecated 使用 {@link com.jbm.cluster.ai.agent.AgentService} 替代
 */
@Deprecated
@Service
@Slf4j
public class AiChatService {

    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    @Autowired
    private ApiFunctionRegistry apiFunctionRegistry;
    
    @Autowired
    private AgentFunctionService agentFunctionService;
    
    /**
     * 会话历史，key 为 sessionId
     */
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();
    
    /**
     * 系统提示词（Agent 模式 - 强调真实执行）
     * 
     * 使用两阶段 Agent 架构：先搜索 API，再执行
     */
    private static final String SYSTEM_PROMPT = """
            你是 JBM 系统的 AI 智能助手，能够查询和操作真实的业务数据。
            
            【核心原则】
            1. 当用户询问数据时，必须使用函数调用（Function Calling）获取真实数据
            2. 绝对禁止编造、猜测或返回示例数据
            3. 永远不要在回复中输出 JSON、函数名或参数 - 直接使用工具调用机制
            
            【标准流程】
            1. 静默调用函数获取真实数据（用户看不到这个过程）
            2. 分析返回的数据
            3. 用自然、简洁的中文直接回答
            
            【正确示例】
            用户："查询用户ID为123的信息"
            正确回复："用户ID 123 的信息如下：
            - 姓名：张三
            - 邮箱：zhangsan@example.com
            - 状态：正常"
            
            【严格禁止】
            ❌ 不要说过程："我将查询..."、"让我来..."、"正在调用..."
            ❌ 不要输出函数调用："searchApis"、"executeApi"、"[内部调用...]"
            ❌ 不要输出 JSON 格式：{"function": ...} 或 ```json ...```
            ❌ 不要编造数据："用户A、用户B、用户C"
            
            关键：静默使用工具，直接给结果。像人类助手一样回答，不要暴露技术细节。
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
                            
                            // 执行函数（支持 Agent 模式）
                            String functionResult = executeFunction(functionName, JSONUtil.parseObj(arguments));
                            
                            log.info("✅ [系统返回] 数据已返回给 AI: {}", 
                                    functionResult.length() > 200 ? 
                                    functionResult.substring(0, 200) + "..." : functionResult);
                            log.info("   → AI 将解析这些数据并生成回复");
                            
                            // 添加函数结果消息（必须包含 tool_call_id）
                            Message toolResultMessage = Message.builder()
                                    .role(Role.TOOL.getValue())
                                    .content(functionResult)
                                    .name(functionName)
                                    .build();
                            
                            // 设置 tool_call_id（关键！）
                            if (funcCall.getId() != null) {
                                toolResultMessage.setToolCallId(funcCall.getId());
                                log.info("🔑 设置 tool_call_id: {}", funcCall.getId());
                            }
                            
                            messages.add(toolResultMessage);
                            
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
                    emitter.onNext(JSONUtil.toJsonStr(Map.of(
                            "error", "AI 模型未配置",
                            "message", "请设置 DASHSCOPE_API_KEY"
                    )) + "\n");
                    emitter.onComplete();
                    return;
                }
                
                // 生成或使用已有的 sessionId
                String sessionId = StrUtil.isNotEmpty(request.getSessionId()) ? 
                        request.getSessionId() : IdUtil.simpleUUID();
                
                // 获取会话历史
                List<Message> messages = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
                
                // 验证消息历史的完整性，防止未完成的函数调用
                validateAndCleanMessageHistory(messages, sessionId);
                
                // 限制消息历史长度，防止超过 API 限制
                limitMessageHistory(messages, sessionId);
                
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
                
                // 发送 sessionId（Spring 会自动添加 "data: " 前缀）
                emitter.onNext(JSONUtil.toJsonStr(Map.of(
                        "type", "sessionId",
                        "sessionId", sessionId
                )) + "\n");
                
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
                
                // 添加工具（根据配置选择模式）
                if (request.isEnableFunctions()) {
                    List<ToolFunction> tools;
                    if (dashScopeConfig.getAgentMode()) {
                        // Agent 模式：只使用 4 个元函数
                        tools = buildAgentTools();
                        log.info("🤖 [Agent模式] 已注入 {} 个元函数（推荐）", tools.size());
                        // 打印函数名称
                        tools.forEach(tool -> {
                            if (tool.getFunction() != null) {
                                log.info("   - 函数: {}", tool.getFunction().getName());
                            }
                        });
                    } else {
                        // 传统模式：注册所有 API
                        tools = buildTools();
                        log.info("🔧 [传统模式] 已注册 {} 个 API 函数", tools.size());
                    }
                    
                    if (!tools.isEmpty()) {
                        paramBuilder.tools(tools);
                        log.info("✅ [流式对话] Tools 参数已设置，共 {} 个函数", tools.size());
                    } else {
                        log.warn("⚠️  [流式对话] Tools 列表为空，AI 将无法调用函数");
                    }
                } else {
                    log.info("❌ [流式对话] Function Calling 已禁用");
                }
                
                GenerationParam param = paramBuilder.build();
                Generation gen = new Generation();
                
                // 调试：打印请求参数
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("📤 [请求参数] 模型: {}", param.getModel());
                log.info("📤 [请求参数] 温度: {}", param.getTemperature());
                log.info("📤 [请求参数] 消息数量: {}", param.getMessages().size());
                log.info("📤 [请求参数] Tools 数量: {}", 
                        param.getTools() != null ? param.getTools().size() : 0);
                if (param.getTools() != null && !param.getTools().isEmpty()) {
                    log.info("📤 [请求参数] Tools 已正确设置:");
                    param.getTools().forEach(tool -> {
                        // ToolFunction 是 ToolBase 的子类
                        if (tool instanceof ToolFunction) {
                            ToolFunction toolFunc = (ToolFunction) tool;
                            if (toolFunc.getFunction() != null) {
                                log.info("   → {}: {}", 
                                        toolFunc.getFunction().getName(),
                                        toolFunc.getFunction().getDescription());
                            }
                        }
                    });
                } else {
                    log.warn("⚠️  [请求参数] Tools 为空或未设置！AI 将无法调用函数！");
                }
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                // 用于收集完整消息
                StringBuilder fullMessage = new StringBuilder();
                final List<ToolCallFunction>[] finalToolCalls = new List[]{new ArrayList<>()};
                
                log.info("🤖 [流式对话] AI 开始生成回复...");
                
                // 使用官方推荐的 Flowable 方式进行流式调用
                Flowable<GenerationResult> resultFlowable = gen.streamCall(param);
                
                final boolean[] firstChunk = {true};
                final Message[] lastMessage = {null};
                
                resultFlowable.subscribe(
                    // onNext: 处理每个响应片段
                    result -> {
                        if (result.getOutput() != null && 
                            result.getOutput().getChoices() != null &&
                            !result.getOutput().getChoices().isEmpty()) {
                            
                            Message msg = result.getOutput().getChoices().get(0).getMessage();
                            
                            // 保存最后一个消息（包含完整的 toolCalls）
                            lastMessage[0] = msg;
                            
                            // 🔍 调试：打印每个 chunk 的 toolCalls 状态
                            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                                log.debug("🔍 [流式调试] 收到 toolCalls，数量: {}", msg.getToolCalls().size());
                                for (int i = 0; i < msg.getToolCalls().size(); i++) {
                                    var tc = msg.getToolCalls().get(i);
                                    if (tc instanceof ToolCallFunction) {
                                        ToolCallFunction tcf = (ToolCallFunction) tc;
                                        log.debug("    [{}] id={}, type={}, function.name={}, function.arguments={}", 
                                                i,
                                                tcf.getId(),
                                                tcf.getType(),
                                                tcf.getFunction() != null ? tcf.getFunction().getName() : "NULL",
                                                tcf.getFunction() != null && tcf.getFunction().getArguments() != null ? 
                                                    tcf.getFunction().getArguments().substring(0, Math.min(50, tcf.getFunction().getArguments().length())) : "NULL");
                                    }
                                }
                            }
                            
                            // 处理文本内容 - AI 真实的流式输出
                            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                                if (firstChunk[0]) {
                                    log.info("📝 [流式对话] AI 回复开始 ↓");
                                    // 检查是否有 tool_calls
                                    if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                                        log.info("🎯 [流式对话] 检测到 toolCalls，AI 正在准备函数调用");
                                    } else {
                                        log.warn("⚠️  [流式对话] 未检测到 toolCalls，AI 直接输出文本");
                                        log.warn("   这可能意味着 AI 没有使用 function calling 功能");
                                    }
                                    System.out.print("🤖 AI: ");
                                    firstChunk[0] = false;
                                }
                                
                                String content = msg.getContent();
                                fullMessage.append(content);
                                
                                // 直接发送 AI 生成的文本
                                emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                        "type", "text",
                                        "content", content
                                )) + "\n");
                                
                                // 实时打印到控制台
                                System.out.print(content);
                                System.out.flush();
                            }
                        }
                    },
                    // onError: 处理错误
                    error -> {
                        System.out.println();
                        log.error("❌ [流式对话] AI 调用失败: {}", error.getMessage());
                        emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                "type", "error",
                                "message", error.getMessage()
                        )) + "\n");
                        emitter.onComplete();
                    },
                    // onComplete: 流式输出完成
                    () -> {
                        try {
                            System.out.println();  // 换行
                            
                            // 从最后一个消息中提取完整的函数调用
                            List<ToolCallFunction> toolCalls = new ArrayList<>();
                            if (lastMessage[0] != null && lastMessage[0].getToolCalls() != null) {
                                for (var toolCall : lastMessage[0].getToolCalls()) {
                                    if (toolCall instanceof ToolCallFunction) {
                                        toolCalls.add((ToolCallFunction) toolCall);
                                    }
                                }
                            }
                            
                            // 🔍 调试：打印 lastMessage 的详细信息
                            if (lastMessage[0] != null) {
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                log.info("🔍 [流式调试] 最后一条消息的详细信息:");
                                log.info("   role: {}", lastMessage[0].getRole());
                                log.info("   content: {}", lastMessage[0].getContent());
                                log.info("   toolCalls: {}", lastMessage[0].getToolCalls());
                                if (lastMessage[0].getToolCalls() != null) {
                                    log.info("   toolCalls.size(): {}", lastMessage[0].getToolCalls().size());
                                }
                            }
                            
                            // 如果有函数调用
                            if (!toolCalls.isEmpty()) {
                                log.info("");
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                log.info("🎯 [流式对话] AI 决定调用 {} 个函数", toolCalls.size());
                                
                                // 调试：打印所有 toolCalls
                                for (int i = 0; i < toolCalls.size(); i++) {
                                    ToolCallFunction tc = toolCalls.get(i);
                                    log.info("  [{}] 函数名: {}, 参数长度: {}", 
                                            i, 
                                            tc.getFunction() != null ? tc.getFunction().getName() : "NULL",
                                            tc.getFunction() != null && tc.getFunction().getArguments() != null ? 
                                                tc.getFunction().getArguments().length() : 0);
                                    if (tc.getFunction() != null && tc.getFunction().getArguments() != null) {
                                        log.info("      完整参数: {}", tc.getFunction().getArguments());
                                    }
                                    
                                    // 🔍 详细调试：打印 tc 对象的字符串表示
                                    log.info("      完整对象: {}", tc);
                                }
                                
                                // 添加助手消息
                                Message assistantMsg = Message.builder()
                                        .role(Role.ASSISTANT.getValue())
                                        .content(fullMessage.toString())
                                        .toolCalls(new ArrayList<>(toolCalls))
                                        .build();
                                messages.add(assistantMsg);
                                
                                // 执行函数调用
                                for (ToolCallFunction funcCall : toolCalls) {
                                    // 空值检查
                                    if (funcCall == null || funcCall.getFunction() == null) {
                                        log.warn("⚠️  [流式对话] 跳过无效的函数调用");
                                        continue;
                                    }
                                    
                                    String functionName = funcCall.getFunction().getName();
                                    String arguments = funcCall.getFunction().getArguments();
                                    
                                    if (functionName == null || functionName.isEmpty()) {
                                        log.warn("⚠️  [流式对话] 跳过空函数名的调用: funcCall={}", funcCall);
                                        continue;
                                    }
                                    
                                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                    log.info("📞 [流式对话] AI 选择调用函数: {}", functionName);
                                    log.info("📦 [流式对话] 原始参数: {}", arguments);
                                    log.info("📦 [流式对话] 参数长度: {}", arguments != null ? arguments.length() : 0);
                                    
                                    try {
                                        // 验证 arguments 是否为空或 null
                                        String finalArguments = arguments;
                                        if (arguments == null || arguments.trim().isEmpty()) {
                                            finalArguments = "{}";
                                            log.info("⚠️  [流式对话] 参数为空，使用默认空对象");
                                        }
                                        
                                        // 验证 JSON 格式
                                        cn.hutool.json.JSONObject argsJson;
                                        try {
                                            argsJson = JSONUtil.parseObj(finalArguments);
                                            log.info("✅ [流式对话] 参数解析成功: {}", argsJson);
                                        } catch (Exception parseEx) {
                                            log.error("❌ [流式对话] 参数 JSON 解析失败: {}", parseEx.getMessage());
                                            log.error("   原始参数: {}", finalArguments);
                                            log.error("   参数长度: {}", finalArguments.length());
                                            // 使用空对象
                                            argsJson = new cn.hutool.json.JSONObject();
                                        }
                                        
                                        // 通知用户正在调用函数（显示完整参数）
                                        emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                                "type", "functionCall",
                                                "functionName", functionName,
                                                "arguments", finalArguments
                                        )) + "\n");
                                        
                                        // 执行函数（支持 Agent 元函数）
                                        log.info("🔄 [流式对话] 正在执行函数调用...");
                                        String funcResult = executeFunction(functionName, argsJson);
                                        
                                        log.info("✅ [流式对话] 函数调用完成");
                                        log.info("📊 [流式对话] 返回数据长度: {} 字符", funcResult.length());
                                        log.info("📊 [流式对话] 返回数据预览: {}", 
                                                funcResult.length() > 300 ? 
                                                funcResult.substring(0, 300) + "..." : funcResult);
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        // 通知前端函数执行完成，并传递结果数据
                                        Map<String, Object> resultMap = new HashMap<>();
                                        resultMap.put("type", "functionResult");
                                        resultMap.put("functionName", functionName);
                                        resultMap.put("success", true);
                                        resultMap.put("result", funcResult);
                                        emitter.onNext(JSONUtil.toJsonStr(resultMap) + "\n");
                                        
                                        // 添加函数结果（必须包含 tool_call_id）
                                        Message toolMessage = Message.builder()
                                                .role(Role.TOOL.getValue())
                                                .content(funcResult)
                                                .name(functionName)
                                                .build();
                                        
                                        // 设置 tool_call_id（关键！）
                                        if (funcCall.getId() != null) {
                                            toolMessage.setToolCallId(funcCall.getId());
                                            log.info("🔑 [流式对话] 设置 tool_call_id: {}", funcCall.getId());
                                        }
                                        
                                        messages.add(toolMessage);
                                        
                                    } catch (Exception funcEx) {
                                        log.error("❌ [流式对话] 函数调用异常: {}", funcEx.getMessage());
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        // 添加错误信息作为函数结果
                                        Map<String, Object> errorMap = new HashMap<>();
                                        errorMap.put("error", true);
                                        errorMap.put("message", "函数调用失败: " + funcEx.getMessage());
                                        String errorResult = JSONUtil.toJsonStr(errorMap);
                                        
                                        Message errorToolMessage = Message.builder()
                                                .role(Role.TOOL.getValue())
                                                .content(errorResult)
                                                .name(functionName)
                                                .build();
                                        
                                        // 设置 tool_call_id
                                        if (funcCall.getId() != null) {
                                            errorToolMessage.setToolCallId(funcCall.getId());
                                        }
                                        
                                        messages.add(errorToolMessage);
                                        
                                        // 通知用户错误
                                        emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                                "type", "error",
                                                "message", "函数 " + functionName + " 调用失败: " + funcEx.getMessage()
                                        )) + "\n");
                                    }
                                }
                                
                                // 再次调用 AI 生成最终回复（流式）
                                log.info("");
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                log.info("🧠 [流式对话] AI 正在分析数据并生成最终回复...");
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                
                                // 通知前端开始生成回复
                                emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                        "type", "analyzing",
                                        "message", "🧠 AI 正在分析数据..."
                                )) + "\n");
                                
                                GenerationParam finalParam = paramBuilder
                                        .messages(messages)
                                        .tools(null)
                                        .build();
                                
                                StringBuilder finalMsg = new StringBuilder();
                                
                                // 使用 Flowable 方式进行第二次流式调用
                                Flowable<GenerationResult> finalFlowable = gen.streamCall(finalParam);
                                final boolean[] finalFirstChunk = {true};
                                
                                finalFlowable.subscribe(
                                    // onNext
                                    result -> {
                                        if (result.getOutput() != null && 
                                            result.getOutput().getChoices() != null &&
                                            !result.getOutput().getChoices().isEmpty()) {
                                            
                                            String content = result.getOutput().getChoices().get(0)
                                                    .getMessage().getContent();
                                            if (content != null) {
                                                if (finalFirstChunk[0]) {
                                                    log.info("📝 [流式对话] AI 最终回复开始 ↓");
                                                    System.out.print("🤖 AI: ");
                                                    finalFirstChunk[0] = false;
                                                }
                                                
                                                finalMsg.append(content);
                                                
                                                // 实时打印
                                                System.out.print(content);
                                                System.out.flush();
                                                
                                                // 发送到前端
                                                String sseData = JSONUtil.toJsonStr(Map.of(
                                                        "type", "text",
                                                        "content", content
                                                )) + "\n";
                                                log.debug("📤 [SSE] 发送: {}", sseData.trim());
                                                emitter.onNext(sseData);
                                            }
                                        }
                                    },
                                    // onError
                                    error -> {
                                        System.out.println();
                                        log.error("❌ [流式对话] 最终回复生成失败: {}", error.getMessage());
                                        emitter.onError(error);
                                    },
                                    // onComplete
                                    () -> {
                                        System.out.println();
                                        
                                        // 添加最终消息到历史
                                        messages.add(Message.builder()
                                                .role(Role.ASSISTANT.getValue())
                                                .content(finalMsg.toString())
                                                .build());
                                        
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        log.info("✅ [流式对话] AI 回复完成");
                                        log.info("📝 [流式对话] 完整内容: {}", finalMsg.toString());
                                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                        
                                        emitter.onNext("[DONE]\n");
                                        emitter.onComplete();
                                    }
                                );
                                
                            } else {
                                // 没有函数调用，直接完成
                                log.info("");
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                log.warn("⚠️  [流式对话] AI 回复完成，但没有调用任何函数");
                                log.warn("   这可能意味着：");
                                log.warn("   1. tools 参数没有正确传递给 AI");
                                log.warn("   2. AI 模型选择输出文本而不是调用函数");
                                log.warn("   3. Function Calling 功能未启用");
                                
                                // 检查是否包含函数名称的文本（说明AI误解了如何调用函数）
                                String content = fullMessage.toString();
                                boolean aiOutputFunctionText = content.contains("/searchApis") || 
                                                               content.contains("/executeApi") ||
                                                               content.contains("searchApis(") || 
                                                               content.contains("executeApi(") ||
                                                               content.contains("让我调用") ||
                                                               content.contains("让我为您查找");
                                
                                if (aiOutputFunctionText) {
                                    log.error("❌ [流式对话] 检测到 AI 输出了函数调用的文本形式！");
                                    log.error("   AI 应该使用 Function Calling 机制，而不是输出文本");
                                    log.error("   这说明 AI 没有理解如何正确使用 tools");
                                    log.error("");
                                    log.error("💡 解决方案：");
                                    log.error("   1. 确认模型支持 Function Calling (推荐 qwen-max)");
                                    log.error("   2. 降低 temperature 到 0.1 或更低");
                                    log.error("   3. 检查 tools 参数是否正确传递（见上方日志）");
                                    log.error("   4. 尝试清除会话重新开始");
                                    
                                    // 通知前端
                                    emitter.onNext(JSONUtil.toJsonStr(Map.of(
                                            "type", "error",
                                            "message", "⚠️ AI 未正确使用 Function Calling，请检查配置或清除会话重试"
                                    )) + "\n");
                                }
                                
                                log.info("📝 [流式对话] 完整内容: {}", fullMessage.toString());
                                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                
                                messages.add(Message.builder()
                                        .role(Role.ASSISTANT.getValue())
                                        .content(fullMessage.toString())
                                        .build());
                                
                                emitter.onNext("[DONE]\n");
                                emitter.onComplete();
                            }
                        } catch (Exception e) {
                            System.out.println();  // 换行
                            log.error("❌ [流式对话] 处理失败: {}", e.getMessage());
                            emitter.onError(e);
                        }
                    }
                );  // subscribe 结束
                
            } catch (Exception e) {
                log.error("❌ [流式] 初始化失败", e);
                emitter.onError(e);
            }
        }, io.reactivex.BackpressureStrategy.BUFFER);
    }
    
    /**
     * 构建 Agent 元函数列表（推荐）
     * 
     * 只提供 4 个高级函数，让 AI 能够搜索和执行实际 API
     * 大幅减少 Token 消耗，提高响应速度
     */
    private List<ToolFunction> buildAgentTools() {
        List<ToolFunction> tools = new ArrayList<>();
        Gson gson = new Gson();
        
        // 0. 测试函数：获取当前时间（用于验证 Function Calling）
        tools.add(ToolFunction.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name("getCurrentTime")
                        .description("获取当前系统时间。当用户询问'现在几点'、'当前时间'、'what time is it'时调用此函数。")
                        .parameters(gson.fromJson("{\"type\": \"object\", \"properties\": {}, \"required\": []}", JsonObject.class))
                        .build())
                .build());
        
        // 1. 搜索 API（最常用）
        tools.add(ToolFunction.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name("searchApis")
                        .description("搜索系统中可用的API接口。当用户询问任何数据（用户、订单、库存等）时，必须先调用此函数找到相关API。返回匹配的API列表，包含apiId供executeApi使用。")
                        .parameters(gson.fromJson("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "query": {
                                            "type": "string",
                                            "description": "搜索关键词，从用户问题中提取，例如：'用户'、'订单'、'库存'、'物料'、'设备'等"
                                        },
                                        "limit": {
                                            "type": "integer",
                                            "description": "返回结果数量限制",
                                            "default": 5
                                        }
                                    },
                                    "required": ["query"]
                                }
                                """, JsonObject.class))
                        .build())
                .build());
        
        // 2. 执行 API（第二常用）
        tools.add(ToolFunction.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name("executeApi")
                        .description("执行指定的API获取真实数据。从searchApis结果中获取apiId后调用此函数。执行后返回真实的业务数据，你需要解析并用自然语言呈现给用户。")
                        .parameters(gson.fromJson("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "apiId": {
                                            "type": "string",
                                            "description": "从searchApis返回结果中获取的API标识符"
                                        },
                                        "parameters": {
                                            "type": "object",
                                            "description": "API所需的参数，如userId、pageSize等。如果API不需要参数则传空对象{}",
                                            "default": {}
                                        }
                                    },
                                    "required": ["apiId"]
                                }
                                """, JsonObject.class))
                        .build())
                .build());
        
        // 3. 列出 API 分类
        tools.add(ToolFunction.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name("listApiCategories")
                        .description("列出所有API分类和服务。当用户询问'你能做什么'、'有什么功能'、'系统有哪些接口'时调用此函数。返回按服务分组的API列表。")
                        .parameters(gson.fromJson("{\"type\": \"object\", \"properties\": {}, \"required\": []}", JsonObject.class))
                        .build())
                .build());
        
        // 4. 获取 API 详情
        tools.add(ToolFunction.builder()
                .type("function")
                .function(FunctionDefinition.builder()
                        .name("getApiDetail")
                        .description("获取指定API的详细信息，包括参数、用法等。当需要了解API需要什么参数才能执行时调用此函数。")
                        .parameters(gson.fromJson("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "apiId": {
                                            "type": "string",
                                            "description": "从searchApis或listApiCategories获取的API标识符"
                                        }
                                    },
                                    "required": ["apiId"]
                                }
                                """, JsonObject.class))
                        .build())
                .build());
        
        log.info("✅ 构建了 {} 个 Agent 元函数", tools.size());
        log.info("   - getCurrentTime (测试函数)");
        log.info("   - searchApis (搜索 API)");
        log.info("   - executeApi (执行 API)");
        log.info("   - listApiCategories (列出分类)");
        log.info("   - getApiDetail (API 详情)");
        return tools;
    }
    
    /**
     * 构建工具列表（函数）- 传统模式
     * 
     * 注册所有 API 为函数（不推荐，Token 消耗大）
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
     * 限制消息历史长度
     * 
     * 防止消息总长度超过 API 限制 (30720 字符)
     * 保留最近的对话，删除最旧的（但保留系统消息）
     */
    private void limitMessageHistory(List<Message> messages, String sessionId) {
        if (messages.size() <= 1) {
            return; // 只有系统消息或空，不需要限制
        }
        
        // 计算总长度
        int totalLength = messages.stream()
                .mapToInt(msg -> msg.getContent() != null ? msg.getContent().length() : 0)
                .sum();
        
        // 限制：保留最近 20 条消息，或总长度不超过 20000 字符
        final int MAX_MESSAGES = 20;
        final int MAX_LENGTH = 20000;
        
        boolean needClean = messages.size() > MAX_MESSAGES || totalLength > MAX_LENGTH;
        
        if (needClean) {
            log.warn("⚠️  [会话限制] 消息历史过长，进行清理");
            log.warn("   会话ID: {}", sessionId);
            log.warn("   当前消息数: {}, 总长度: {}", messages.size(), totalLength);
            
            // 保留系统消息（第一条）
            Message systemMsg = messages.get(0);
            List<Message> recentMessages = new ArrayList<>();
            recentMessages.add(systemMsg);
            
            // 保留最近的消息
            int startIndex = Math.max(1, messages.size() - MAX_MESSAGES + 1);
            for (int i = startIndex; i < messages.size(); i++) {
                recentMessages.add(messages.get(i));
            }
            
            // 替换消息列表
            messages.clear();
            messages.addAll(recentMessages);
            
            // 重新计算长度
            int newLength = messages.stream()
                    .mapToInt(msg -> msg.getContent() != null ? msg.getContent().length() : 0)
                    .sum();
            
            log.info("✅ [会话限制] 清理完成");
            log.info("   清理后消息数: {}, 总长度: {}", messages.size(), newLength);
        }
    }
    
    /**
     * 验证并清理消息历史
     * 
     * 防止未完成的函数调用导致 API 错误
     * 规则：assistant message 带 tool_calls 后，必须跟 tool role 的响应
     */
    private void validateAndCleanMessageHistory(List<Message> messages, String sessionId) {
        if (messages.isEmpty()) {
            return;
        }
        
        // 检查最后一条消息
        Message lastMsg = messages.get(messages.size() - 1);
        
        // 如果最后一条是 assistant 消息且有 tool_calls，说明上次函数调用未完成
        if (Role.ASSISTANT.getValue().equals(lastMsg.getRole()) && 
            lastMsg.getToolCalls() != null && 
            !lastMsg.getToolCalls().isEmpty()) {
            
            log.warn("⚠️  [会话清理] 检测到未完成的函数调用，清理会话历史");
            log.warn("   会话ID: {}", sessionId);
            log.warn("   问题消息索引: {}", messages.size() - 1);
            
            // 移除未完成的 assistant 消息
            messages.remove(messages.size() - 1);
            log.info("✅ [会话清理] 已移除未完成的函数调用消息");
        }
        
        // 再检查一次，确保没有孤立的 tool_calls
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (Role.ASSISTANT.getValue().equals(msg.getRole()) && 
                msg.getToolCalls() != null && 
                !msg.getToolCalls().isEmpty()) {
                
                // 检查后面是否有对应的 tool 响应
                boolean hasToolResponse = false;
                if (i + 1 < messages.size()) {
                    Message nextMsg = messages.get(i + 1);
                    if (Role.TOOL.getValue().equals(nextMsg.getRole())) {
                        hasToolResponse = true;
                    }
                }
                
                if (!hasToolResponse) {
                    log.warn("⚠️  [会话清理] 发现第 {} 条消息有未响应的函数调用，清理从此开始的所有消息", i);
                    // 移除从这条消息开始的所有后续消息
                    while (messages.size() > i) {
                        messages.remove(messages.size() - 1);
                    }
                    break;
                }
            }
        }
    }
    
    /**
     * 执行函数（支持 Agent 元函数和普通函数）
     * 
     * @param functionName 函数名
     * @param params 参数
     * @return 执行结果
     */
    private String executeFunction(String functionName, cn.hutool.json.JSONObject params) {
        // 判断是否是 Agent 元函数
        switch (functionName) {
            case "getCurrentTime":
                log.info("⏰ [测试] 执行测试函数: getCurrentTime");
                return agentFunctionService.getCurrentTime(params);
                
            case "searchApis":
                log.info("🔍 [Agent] 执行元函数: searchApis");
                return agentFunctionService.searchApis(params);
                
            case "listApiCategories":
                log.info("📋 [Agent] 执行元函数: listApiCategories");
                return agentFunctionService.listApiCategories(params);
                
            case "getApiDetail":
                log.info("📄 [Agent] 执行元函数: getApiDetail");
                return agentFunctionService.getApiDetail(params);
                
            case "executeApi":
                log.info("🚀 [Agent] 执行元函数: executeApi");
                return agentFunctionService.executeApi(params);
                
            default:
                // 传统模式：直接执行业务 API
                log.info("🔧 [传统模式] 执行业务函数: {}", functionName);
                return apiFunctionRegistry.executeFunction(functionName, params);
        }
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
