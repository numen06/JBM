package com.jbm.cluster.ai.agent.routing;

import com.jbm.cluster.ai.agent.binding.ParameterBinder;
import com.jbm.cluster.ai.agent.dialogue.DialogueState;
import com.jbm.cluster.ai.agent.dialogue.ParameterCollector;
import com.jbm.cluster.ai.agent.execution.ApiExecutor;
import com.jbm.cluster.ai.agent.model.AgentContext;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.agent.model.Intent;
import com.jbm.cluster.ai.agent.selection.ApiSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话式意图处理器
 * 
 * 处理需要 API 调用的意图，支持参数收集对话
 * 
 * @author wesley
 */
@Slf4j
@Component
public class DialogueIntentHandler implements IntentHandler {
    
    @Autowired
    private ApiSelector apiSelector;
    
    @Autowired
    private ParameterBinder parameterBinder;
    
    @Autowired
    private ParameterCollector parameterCollector;
    
    @Autowired
    private ApiExecutor apiExecutor;
    
    @Override
    public boolean canHandle(Intent intent) {
        // 处理 QUERY, CREATE, UPDATE, DELETE 类型的意图
        if (intent == null || intent.getType() == null) {
            return false;
        }
        
        Intent.IntentType type = intent.getType();
        return type == Intent.IntentType.QUERY || 
               type == Intent.IntentType.CREATE || 
               type == Intent.IntentType.UPDATE || 
               type == Intent.IntentType.DELETE;
    }
    
    @Override
    public AgentContext handle(AgentContext context) {
        if (context == null || context.getIntent() == null) {
            context.setSuccess(false);
            context.setErrorMessage("上下文或意图为空");
            return context;
        }
        
        log.info("🎯 [对话处理] 开始处理意图: {}", context.getIntent().getName());
        
        try {
            // 1. 选择 API
            context.setStage(AgentContext.ProcessStage.API_SELECTION);
            ApiDefinition selectedApi = selectApi(context);
            
            if (selectedApi == null) {
                // 未找到匹配的 API，返回友好提示
                String friendlyMessage = generateNoApiFoundMessage(context.getIntent(), context.getUserQuery());
                context.setApiResponse("{\"message\": \"" + escapeJson(friendlyMessage) + "\"}");
                context.setResponseStatusCode(404);
                context.setSuccess(true);  // 设置为成功，让系统正常返回消息
                log.warn("⚠️  [对话处理] 未找到匹配的 API，返回友好提示");
                return context;
            }
            
            context.setSelectedApi(selectedApi);
            log.info("✅ [对话处理] 已选择 API: {} {}", 
                    selectedApi.getMethod(), selectedApi.getPath());
            
            // 2. 合并参数（意图参数 + 已收集参数）
            Map<String, Object> allParams = mergeParameters(context);
            
            // 3. 尝试参数绑定（部分绑定模式）
            context.setStage(AgentContext.ProcessStage.PARAMETER_BINDING);
            ParameterBinder.BindingResult bindingResult = 
                    parameterBinder.bind(selectedApi, allParams, true);
            
            // 4. 检查是否缺少必填参数
            if (bindingResult.isPartialBinding() && 
                bindingResult.getMissingRequiredParameters() != null && 
                !bindingResult.getMissingRequiredParameters().isEmpty()) {
                
                // 需要参数收集
                log.info("⚠️  [对话处理] 缺少必填参数，需要进入对话模式");
                context.setNeedsParameterCollection(true);
                
                // 创建或更新对话状态
                initializeDialogueState(context, selectedApi, bindingResult.getMissingRequiredParameters());
                
                // 不执行 API，等待参数收集
                return context;
            }
            
            // 5. 参数齐全，执行 API
            if (bindingResult.isSuccess()) {
                context.setStage(AgentContext.ProcessStage.API_CALLING);
                context.setBoundUrl(bindingResult.getUrl());
                context.setRequestMethod(bindingResult.getMethod());
                context.setRequestBody(bindingResult.getRequestBody());
                
                log.info("🚀 [对话处理] 参数齐全，执行 API");
                ApiExecutor.ExecutionResult executionResult = apiExecutor.execute(
                        bindingResult.getUrl(),
                        bindingResult.getMethod(),
                        bindingResult.getRequestBody()
                );
                
                context.setApiResponse(executionResult.getResponse());
                context.setResponseStatusCode(executionResult.getStatusCode());
                context.setSuccess(executionResult.isSuccess());
                
                if (!executionResult.isSuccess()) {
                    context.setErrorMessage(executionResult.getErrorMessage());
                }
                
                log.info("✅ [对话处理] API 执行完成: success={}", executionResult.isSuccess());
                
            } else {
                context.setSuccess(false);
                context.setErrorMessage(bindingResult.getErrorMessage());
                log.error("❌ [对话处理] 参数绑定失败: {}", bindingResult.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ [对话处理] 处理失败: {}", e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("处理失败: " + e.getMessage());
        }
        
        return context;
    }
    
    /**
     * 选择 API
     */
    private ApiDefinition selectApi(AgentContext context) {
        List<ApiDefinition> allApis = apiSelector.getAllApis();
        
        if (allApis == null || allApis.isEmpty()) {
            log.warn("⚠️  [对话处理] 没有可用的 API");
            return null;
        }
        
        return apiSelector.selectBestApi(context.getIntent(), allApis);
    }
    
    /**
     * 合并参数
     * 
     * 优先级：已收集参数 > 意图参数
     */
    private Map<String, Object> mergeParameters(AgentContext context) {
        Map<String, Object> merged = new HashMap<>();
        
        // 1. 添加意图参数
        if (context.getIntent().getParams() != null) {
            merged.putAll(context.getIntent().getParams());
        }
        
        // 2. 添加已收集参数（覆盖意图参数）
        if (context.getCollectedParameters() != null) {
            merged.putAll(context.getCollectedParameters());
        }
        
        // 3. 从对话状态添加参数
        if (context.getDialogueState() != null && 
            context.getDialogueState().getCollectedParameters() != null) {
            merged.putAll(context.getDialogueState().getCollectedParameters());
        }
        
        log.debug("🔀 [对话处理] 合并参数: {}", merged);
        
        return merged;
    }
    
    /**
     * 初始化对话状态
     */
    private void initializeDialogueState(AgentContext context, 
                                          ApiDefinition selectedApi, 
                                          List<String> missingParams) {
        DialogueState dialogueState = context.getDialogueState();
        
        if (dialogueState == null) {
            // 创建新的对话状态
            dialogueState = new DialogueState(
                    context.getSessionId(),
                    context.getUserQuery(),
                    context.getIntent(),
                    selectedApi
            );
            context.setDialogueState(dialogueState);
            log.info("📝 [对话处理] 创建新的对话状态");
        }
        
        // 设置缺失参数
        dialogueState.setMissingRequiredParameters(missingParams);
        
        // 设置当前询问的参数（第一个缺失的参数）
        if (!missingParams.isEmpty()) {
            String nextParam = missingParams.get(0);
            dialogueState.setCurrentAskingParameter(nextParam);
            log.info("❓ [对话处理] 准备询问参数: {}", nextParam);
        }
    }
    
    /**
     * 生成未找到 API 的友好提示
     */
    private String generateNoApiFoundMessage(Intent intent, String userQuery) {
        StringBuilder message = new StringBuilder();
        message.append("抱歉，我在系统中没有找到与\"").append(userQuery).append("\"相关的功能接口。\n\n");
        message.append("可能的原因：\n");
        message.append("1. 该功能暂未在系统中实现\n");
        message.append("2. 您可以换一种表达方式试试\n");
        message.append("3. 请联系管理员确认是否有相关API\n\n");
        message.append("💡 提示：您可以询问\"你可以帮我做什么？\"来了解我的功能。");
        
        return message.toString();
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
        // 中等优先级
        return 50;
    }
}

