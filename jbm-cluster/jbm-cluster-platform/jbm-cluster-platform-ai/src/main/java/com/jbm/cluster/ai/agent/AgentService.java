package com.jbm.cluster.ai.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jbm.cluster.ai.agent.config.AgentProperties;
import com.jbm.cluster.ai.agent.dialogue.DialogueState;
import com.jbm.cluster.ai.agent.dialogue.DialogueStateManager;
import com.jbm.cluster.ai.agent.dialogue.ParameterCollector;
import com.jbm.cluster.ai.agent.dialogue.ParameterExtractor;
import com.jbm.cluster.ai.agent.execution.ResponseFormatter;
import com.jbm.cluster.ai.agent.model.*;
import com.jbm.cluster.ai.agent.nlu.IntentRecognizer;
import com.jbm.cluster.ai.agent.routing.IntentHandler;
import com.jbm.cluster.ai.agent.routing.IntentRouter;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 主控服务
 * 
 * 串联所有模块，支持流式输出
 * 
 * @author wesley
 */
@Slf4j
@Service
public class AgentService {
    
    @Autowired
    private IntentRecognizer intentRecognizer;
    
    @Autowired
    private IntentRouter intentRouter;
    
    @Autowired
    private ResponseFormatter responseFormatter;
    
    @Autowired
    private DialogueStateManager dialogueStateManager;
    
    @Autowired
    private ParameterCollector parameterCollector;
    
    @Autowired
    private ParameterExtractor parameterExtractor;
    
    @Autowired
    private AgentProperties agentProperties;
    
    /**
     * 处理请求（流式）
     * 
     * @param request Agent 请求
     * @return 流式响应
     */
    public Flowable<String> askStream(AgentRequest request) {
        return Flowable.create(emitter -> {
            try {
                // 0. 生成 sessionId
                String sessionId = StrUtil.isNotEmpty(request.getSessionId()) ? 
                        request.getSessionId() : IdUtil.simpleUUID();
                
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("🤖 [Agent] 开始处理请求");
                log.info("   用户问题: {}", request.getMessage());
                log.info("   会话ID: {}", sessionId);
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                // 发送 sessionId
                emitter.onNext(toJson("sessionId", Map.of("sessionId", sessionId)) + "\n");
                
                // 0.5. 检查是否存在未完成的对话状态（对话模式）
                DialogueState existingState = dialogueStateManager.getState(sessionId);
                if (existingState != null && existingState.isWaitingForParameter() && 
                    agentProperties.getDialogue().getEnabled()) {
                    
                    log.info("💬 [Agent] 检测到未完成的对话，进入参数收集流程");
                    handleParameterCollection(emitter, existingState, request.getMessage());
                    return;
                }
                
                // 1. 初始化上下文（使用数组包装以便在 lambda 中修改）
                final AgentContext[] contextHolder = new AgentContext[1];
                contextHolder[0] = new AgentContext(sessionId, request.getMessage());
                
                // 2. NLU - 意图识别
                emitter.onNext(toJson("stage", Map.of(
                        "stage", "nlu", 
                        "message", "正在理解您的问题..."
                )) + "\n");
                
                Intent intent = intentRecognizer.recognize(request.getMessage());
                contextHolder[0].setIntent(intent);
                contextHolder[0].setStage(AgentContext.ProcessStage.NLU);
                
                // 发送意图信息
                Map<String, Object> intentInfo = new HashMap<>();
                intentInfo.put("name", intent.getName());
                intentInfo.put("type", intent.getType());
                intentInfo.put("confidence", intent.getConfidence());
                intentInfo.put("params", intent.getParams());
                emitter.onNext(toJson("intent", intentInfo) + "\n");
                
                log.info("✅ [Agent] NLU 完成: intent={}, confidence={}", 
                        intent.getName(), intent.getConfidence());
                
                // 3. Intent Routing
                emitter.onNext(toJson("stage", Map.of(
                        "stage", "routing", 
                        "message", "正在选择处理策略..."
                )) + "\n");
                
                IntentHandler handler = intentRouter.route(intent);
                
                if (handler == null) {
                    String error = "未找到合适的处理器";
                    log.error("❌ [Agent] {}", error);
                    emitter.onNext(toJson("error", Map.of("message", error)) + "\n");
                    emitter.onComplete();
                    return;
                }
                
                log.info("✅ [Agent] Routing 完成: handler={}", 
                        handler.getClass().getSimpleName());
                
                // 4. API Selection
                emitter.onNext(toJson("stage", Map.of(
                        "stage", "selection", 
                        "message", "正在选择合适的API..."
                )) + "\n");
                
                // 5. 执行处理（包含 API Selection、参数绑定、API 调用）
                contextHolder[0] = handler.handle(contextHolder[0]);
                
                // 5.5. 检查是否需要参数收集（对话模式）
                if (contextHolder[0].isNeedsParameterCollection() && 
                    agentProperties.getDialogue().getEnabled()) {
                    
                    log.info("💬 [Agent] 需要参数收集，进入对话模式");
                    
                    DialogueState dialogueState = contextHolder[0].getDialogueState();
                    if (dialogueState != null) {
                        // 保存对话状态
                        dialogueStateManager.createState(dialogueState);
                        
                        // 生成提问
                        String question = generateQuestion(dialogueState);
                        
                        // 发送参数需求信息
                        emitter.onNext(toJson("parameterNeeded", Map.of(
                                "parameter", dialogueState.getCurrentAskingParameter(),
                                "question", question,
                                "missingCount", dialogueState.getMissingRequiredParameters().size(),
                                "round", dialogueState.getRoundCount() + 1
                        )) + "\n");
                        
                        // 发送问题文本
                        emitter.onNext(toJson("text", Map.of("content", question)) + "\n");
                        
                        log.info("✅ [Agent] 已发送参数收集问题");
                        emitter.onNext("[DONE]\n");
                        emitter.onComplete();
                        return;
                    }
                }
                
                if (!contextHolder[0].isSuccess()) {
                    String error = contextHolder[0].getErrorMessage();
                    log.error("❌ [Agent] 处理失败: {}", error);
                    emitter.onNext(toJson("error", Map.of("message", error)) + "\n");
                    emitter.onComplete();
                    return;
                }
                
                // 发送选中的 API
                if (contextHolder[0].getSelectedApi() != null) {
                    Map<String, Object> apiInfo = new HashMap<>();
                    apiInfo.put("method", contextHolder[0].getSelectedApi().getMethod());
                    apiInfo.put("path", contextHolder[0].getSelectedApi().getPath());
                    apiInfo.put("service", contextHolder[0].getSelectedApi().getServiceName());
                    apiInfo.put("matchScore", contextHolder[0].getSelectedApi().getMatchScore());
                    emitter.onNext(toJson("apiSelected", apiInfo) + "\n");
                }
                
                // 发送 API 调用信息
                if (contextHolder[0].getBoundUrl() != null) {
                    emitter.onNext(toJson("apiCalling", Map.of(
                            "url", contextHolder[0].getBoundUrl(),
                            "method", contextHolder[0].getRequestMethod()
                    )) + "\n");
                }
                
                // 发送 API 响应信息
                if (contextHolder[0].getApiResponse() != null) {
                    Map<String, Object> resultInfo = new HashMap<>();
                    resultInfo.put("statusCode", contextHolder[0].getResponseStatusCode());
                    resultInfo.put("dataLength", contextHolder[0].getApiResponse().length());
                    emitter.onNext(toJson("apiResult", resultInfo) + "\n");
                }
                
                log.info("✅ [Agent] API 调用完成，耗时: {}ms", contextHolder[0].getDurationMs());
                
                // 6. 格式化响应（流式输出）
                contextHolder[0].setStage(AgentContext.ProcessStage.RESPONSE_FORMATTING);
                emitter.onNext(toJson("stage", Map.of(
                        "stage", "formatting", 
                        "message", "正在生成回复..."
                )) + "\n");
                
                Flowable<String> textStream = responseFormatter.formatStream(contextHolder[0]);
                
                textStream.subscribe(
                    // onNext: 逐字输出文本
                    text -> {
                        emitter.onNext(toJson("text", Map.of("content", text)) + "\n");
                    },
                    // onError
                    error -> {
                        log.error("❌ [Agent] 格式化失败: {}", error.getMessage());
                        emitter.onNext(toJson("error", Map.of(
                                "message", "响应格式化失败: " + error.getMessage()
                        )) + "\n");
                        emitter.onComplete();
                    },
                    // onComplete
                    () -> {
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        log.info("✅ [Agent] 请求处理完成，总耗时: {}ms", contextHolder[0].getDurationMs());
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        
                        emitter.onNext("[DONE]\n");
                        emitter.onComplete();
                    }
                );
                
            } catch (Exception e) {
                log.error("❌ [Agent] 处理异常: {}", e.getMessage(), e);
                emitter.onNext(toJson("error", Map.of(
                        "message", "处理失败: " + e.getMessage()
                )) + "\n");
                emitter.onComplete();
            }
        }, BackpressureStrategy.BUFFER);
    }
    
    /**
     * 处理请求（同步）
     * 
     * @param request Agent 请求
     * @return 响应
     */
    @SuppressWarnings("unchecked")
    public AgentResponse ask(AgentRequest request) {
        String sessionId = StrUtil.isNotEmpty(request.getSessionId()) ? 
                request.getSessionId() : IdUtil.simpleUUID();
        
        long startTime = System.currentTimeMillis();
        StringBuilder messageBuilder = new StringBuilder();
        
        // 使用数组包装以便在 lambda 中修改
        final Intent[] intentResultHolder = new Intent[1];
        final String[] apiCalledHolder = new String[1];
        
        try {
            askStream(request).blockingForEach(event -> {
                // 解析事件
                try {
                    if (event.equals("[DONE]\n")) {
                        return;
                    }
                    
                    Map<String, Object> eventData = JSONUtil.parseObj(event);
                    String type = (String) eventData.get("type");
                    
                    if ("text".equals(type)) {
                        Map<String, Object> content = (Map<String, Object>) eventData.get("content");
                        if (content != null) {
                            messageBuilder.append(content.get("content"));
                        }
                    } else if ("intent".equals(type)) {
                        // 保存意图信息
                        Intent intent = new Intent();
                        intent.setName((String) eventData.get("name"));
                        intent.setConfidence(((Number) eventData.get("confidence")).doubleValue());
                        intentResultHolder[0] = intent;
                    } else if ("apiSelected".equals(type)) {
                        apiCalledHolder[0] = String.format("%s %s", 
                                eventData.get("method"), 
                                eventData.get("path"));
                    }
                } catch (Exception e) {
                    log.warn("解析事件失败: {}", event, e);
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            
            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .message(messageBuilder.toString())
                    .success(true)
                    .durationMs(duration)
                    .intent(intentResultHolder[0])
                    .apiCalled(apiCalledHolder[0])
                    .build();
            
        } catch (Exception e) {
            log.error("❌ [Agent] 同步处理失败: {}", e.getMessage(), e);
            return AgentResponse.error(sessionId, e.getMessage());
        }
    }
    
    /**
     * 处理参数收集（对话模式）
     */
    private void handleParameterCollection(io.reactivex.FlowableEmitter<String> emitter, 
                                            DialogueState dialogueState, 
                                            String userReply) {
        try {
            log.info("💬 [参数收集] 开始处理用户回复");
            log.info("   当前询问参数: {}", dialogueState.getCurrentAskingParameter());
            log.info("   用户回复: {}", userReply);
            
            // 1. 检查轮次限制
            int maxRounds = agentProperties.getDialogue().getMaxRounds();
            if (dialogueState.getRoundCount() >= maxRounds) {
                log.warn("⚠️  [参数收集] 超过最大轮次限制: {}", maxRounds);
                emitter.onNext(toJson("error", Map.of(
                        "message", "对话轮次超过限制，请稍后重试"
                )) + "\n");
                dialogueStateManager.removeState(dialogueState.getSessionId());
                emitter.onComplete();
                return;
            }
            
            // 2. 检查取消指令
            if (isCancelCommand(userReply)) {
                log.info("🚫 [参数收集] 用户取消对话");
                emitter.onNext(toJson("text", Map.of("content", "已取消操作")) + "\n");
                dialogueStateManager.removeState(dialogueState.getSessionId());
                emitter.onNext("[DONE]\n");
                emitter.onComplete();
                return;
            }
            
            // 3. 提取参数
            String currentParam = dialogueState.getCurrentAskingParameter();
            ParameterExtractor.ExtractionResult extractionResult = 
                    parameterExtractor.extractParameter(
                            dialogueState.getSelectedApi(),
                            currentParam,
                            userReply
                    );
            
            // 4. 记录对话轮次
            String question = parameterCollector.generateQuestion(
                    dialogueState.getSelectedApi(), currentParam);
            dialogueState.recordRound(question, userReply);
            
            // 5. 处理提取结果
            if (extractionResult.isSuccess() && 
                extractionResult.getConfidence() >= agentProperties.getDialogue().getExtractionConfidenceThreshold()) {
                
                // 参数提取成功
                dialogueState.addCollectedParameter(currentParam, extractionResult.getValue());
                log.info("✅ [参数收集] 成功提取参数: {} = {}", currentParam, extractionResult.getValue());
                
                // 发送参数收集成功信息
                emitter.onNext(toJson("parameterCollected", Map.of(
                        "parameter", currentParam,
                        "value", extractionResult.getValue()
                )) + "\n");
                
            } else {
                // 提取失败，重新询问
                log.warn("⚠️  [参数收集] 参数提取失败: {}", extractionResult.getReason());
                
                String retryQuestion = String.format(
                        "抱歉，我没有理解您的回复。%s",
                        parameterCollector.generateQuestion(dialogueState.getSelectedApi(), currentParam)
                );
                
                emitter.onNext(toJson("text", Map.of("content", retryQuestion)) + "\n");
                dialogueStateManager.updateState(dialogueState);
                emitter.onNext("[DONE]\n");
                emitter.onComplete();
                return;
            }
            
            // 6. 检查是否还有缺失参数
            if (dialogueState.isParametersComplete()) {
                // 所有参数已收集完毕，执行 API
                log.info("✅ [参数收集] 所有参数已收集完毕，准备执行 API");
                
                emitter.onNext(toJson("apiReady", Map.of(
                        "message", "参数已齐全，开始执行..."
                )) + "\n");
                
                // 执行完整的 Agent 流程
                executeWithCollectedParameters(emitter, dialogueState);
                
                // 清理对话状态
                dialogueStateManager.removeState(dialogueState.getSessionId());
                
            } else {
                // 还有参数缺失，继续询问
                String nextParam = dialogueState.getNextParameterToAsk();
                dialogueState.setCurrentAskingParameter(nextParam);
                
                String nextQuestion = generateQuestion(dialogueState);
                
                log.info("❓ [参数收集] 继续询问下一个参数: {}", nextParam);
                
                // 发送下一个问题
                emitter.onNext(toJson("parameterNeeded", Map.of(
                        "parameter", nextParam,
                        "question", nextQuestion,
                        "missingCount", dialogueState.getMissingRequiredParameters().size(),
                        "round", dialogueState.getRoundCount() + 1
                )) + "\n");
                
                emitter.onNext(toJson("text", Map.of("content", nextQuestion)) + "\n");
                
                // 更新状态
                dialogueStateManager.updateState(dialogueState);
                
                emitter.onNext("[DONE]\n");
                emitter.onComplete();
            }
            
        } catch (Exception e) {
            log.error("❌ [参数收集] 处理失败: {}", e.getMessage(), e);
            emitter.onNext(toJson("error", Map.of("message", "参数收集失败: " + e.getMessage())) + "\n");
            dialogueStateManager.removeState(dialogueState.getSessionId());
            emitter.onComplete();
        }
    }
    
    /**
     * 使用已收集的参数执行 API
     */
    private void executeWithCollectedParameters(io.reactivex.FlowableEmitter<String> emitter, 
                                                 DialogueState dialogueState) {
        try {
            // 创建新的 context
            AgentContext context = new AgentContext(
                    dialogueState.getSessionId(),
                    dialogueState.getOriginalQuery()
            );
            context.setIntent(dialogueState.getIntent());
            context.setSelectedApi(dialogueState.getSelectedApi());
            context.setCollectedParameters(dialogueState.getCollectedParameters());
            context.setDialogueState(dialogueState);
            
            // 路由并处理（会执行 API）
            IntentHandler handler = intentRouter.route(context.getIntent());
            if (handler != null) {
                context = handler.handle(context);
            }
            
            if (!context.isSuccess()) {
                emitter.onNext(toJson("error", Map.of("message", context.getErrorMessage())) + "\n");
                emitter.onComplete();
                return;
            }
            
            // 发送 API 执行信息
            if (context.getBoundUrl() != null) {
                emitter.onNext(toJson("apiCalling", Map.of(
                        "url", context.getBoundUrl(),
                        "method", context.getRequestMethod()
                )) + "\n");
            }
            
            if (context.getApiResponse() != null) {
                emitter.onNext(toJson("apiResult", Map.of(
                        "statusCode", context.getResponseStatusCode(),
                        "dataLength", context.getApiResponse().length()
                )) + "\n");
            }
            
            // 格式化响应
            context.setStage(AgentContext.ProcessStage.RESPONSE_FORMATTING);
            emitter.onNext(toJson("stage", Map.of(
                    "stage", "formatting",
                    "message", "正在生成回复..."
            )) + "\n");
            
            Flowable<String> textStream = responseFormatter.formatStream(context);
            
            textStream.subscribe(
                    text -> emitter.onNext(toJson("text", Map.of("content", text)) + "\n"),
                    error -> {
                        log.error("❌ [参数收集] 格式化失败: {}", error.getMessage());
                        emitter.onNext(toJson("error", Map.of("message", "响应格式化失败")) + "\n");
                        emitter.onComplete();
                    },
                    () -> {
                        log.info("✅ [参数收集] 完整流程执行完成");
                        emitter.onNext("[DONE]\n");
                        emitter.onComplete();
                    }
            );
            
        } catch (Exception e) {
            log.error("❌ [参数收集] 执行失败: {}", e.getMessage(), e);
            emitter.onNext(toJson("error", Map.of("message", "执行失败: " + e.getMessage())) + "\n");
            emitter.onComplete();
        }
    }
    
    /**
     * 生成提问
     */
    private String generateQuestion(DialogueState dialogueState) {
        String paramName = dialogueState.getCurrentAskingParameter();
        String question = parameterCollector.generateQuestion(
                dialogueState.getSelectedApi(), paramName);
        
        // 添加进度提示
        int total = dialogueState.getCollectedParameters().size() + 
                    dialogueState.getMissingRequiredParameters().size();
        int collected = dialogueState.getCollectedParameters().size();
        
        return String.format("[%d/%d] %s", collected + 1, total, question);
    }
    
    /**
     * 检查是否为取消指令
     */
    private boolean isCancelCommand(String text) {
        if (StrUtil.isEmpty(text)) {
            return false;
        }
        
        String lower = text.toLowerCase().trim();
        return lower.equals("取消") || 
               lower.equals("退出") || 
               lower.equals("算了") ||
               lower.equals("cancel") || 
               lower.equals("quit") || 
               lower.equals("exit");
    }
    
    /**
     * 构建 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    private String toJson(String type, Object data) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.putAll((Map<String, Object>) data);
        return JSONUtil.toJsonStr(event);
    }
}

