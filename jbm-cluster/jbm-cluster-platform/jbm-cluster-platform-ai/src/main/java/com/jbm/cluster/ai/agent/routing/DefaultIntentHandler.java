package com.jbm.cluster.ai.agent.routing;

import com.jbm.cluster.ai.agent.binding.ParameterBinder;
import com.jbm.cluster.ai.agent.execution.ApiExecutor;
import com.jbm.cluster.ai.agent.model.AgentContext;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.agent.model.Intent;
import com.jbm.cluster.ai.agent.selection.ApiSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认意图处理器
 * 
 * 适用于所有意图的通用处理器
 * 执行标准流程：API Selection → 参数绑定 → API 调用
 * 
 * @author wesley
 */
@Slf4j
@Component
public class DefaultIntentHandler implements IntentHandler {
    
    @Autowired
    private ApiSelector apiSelector;
    
    @Autowired
    private ParameterBinder parameterBinder;
    
    @Autowired
    private ApiExecutor apiExecutor;
    
    @Override
    public boolean canHandle(Intent intent) {
        // 默认处理器可以处理所有意图
        return true;
    }
    
    @Override
    public AgentContext handle(AgentContext context) {
        try {
            Intent intent = context.getIntent();
            
            if (intent == null) {
                context.setErrorMessage("意图为空");
                context.markCompleted(false);
                return context;
            }
            
            log.info("🎯 [Intent Handler] 使用默认处理器处理意图: {}", intent.getName());
            
            // 1. API Selection
            context.setStage(AgentContext.ProcessStage.API_SELECTION);
            log.info("📋 [Intent Handler] 阶段 1: API 选择");
            
            ApiDefinition selectedApi = apiSelector.selectBestApi(intent, apiSelector.getAllApis());
            
            if (selectedApi == null) {
                log.warn("⚠️  [Intent Handler] 未找到匹配的 API，返回提示信息");
                
                // 生成友好的提示信息
                String message = String.format(
                    "抱歉，系统中暂时没有关于「%s」的接口。\n\n" +
                    "您可以：\n" +
                    "1. 尝试用其他方式描述您的需求\n" +
                    "2. 询问「你能做什么」查看系统支持的功能\n" +
                    "3. 联系管理员添加相关接口",
                    intent.getRawQuery()
                );
                
                context.setApiResponse(message);
                context.setResponseStatusCode(200);
                context.markCompleted(true);
                return context;
            }
            
            context.setSelectedApi(selectedApi);
            log.info("✅ [Intent Handler] API 选择完成: {} {}", 
                    selectedApi.getMethod(), selectedApi.getPath());
            
            // 2. 参数绑定
            context.setStage(AgentContext.ProcessStage.PARAMETER_BINDING);
            log.info("🔗 [Intent Handler] 阶段 2: 参数绑定");
            
            ParameterBinder.BindingResult bindingResult = 
                    parameterBinder.bind(selectedApi, intent.getParams());
            
            if (!bindingResult.isSuccess()) {
                context.setErrorMessage(bindingResult.getErrorMessage());
                context.markCompleted(false);
                log.warn("⚠️  [Intent Handler] 参数绑定失败: {}", bindingResult.getErrorMessage());
                return context;
            }
            
            context.setBoundUrl(bindingResult.getUrl());
            context.setRequestMethod(bindingResult.getMethod());
            context.setRequestBody(bindingResult.getRequestBody());
            log.info("✅ [Intent Handler] 参数绑定完成");
            
            // 3. API 调用
            context.setStage(AgentContext.ProcessStage.API_CALLING);
            log.info("🚀 [Intent Handler] 阶段 3: API 调用");
            
            ApiExecutor.ExecutionResult executionResult = 
                    apiExecutor.execute(bindingResult.getUrl(), 
                                      bindingResult.getMethod(), 
                                      bindingResult.getRequestBody());
            
            if (!executionResult.isSuccess()) {
                context.setErrorMessage(executionResult.getErrorMessage());
                context.setResponseStatusCode(executionResult.getStatusCode());
                context.markCompleted(false);
                log.warn("⚠️  [Intent Handler] API 调用失败: {}", executionResult.getErrorMessage());
                return context;
            }
            
            context.setApiResponse(executionResult.getResponse());
            context.setResponseStatusCode(executionResult.getStatusCode());
            context.addMetadata("apiDuration", executionResult.getDuration());
            log.info("✅ [Intent Handler] API 调用完成，耗时: {}ms", executionResult.getDuration());
            
            // 标记成功
            context.markCompleted(true);
            log.info("✅ [Intent Handler] 意图处理完成");
            
            return context;
            
        } catch (Exception e) {
            log.error("❌ [Intent Handler] 意图处理失败: {}", e.getMessage(), e);
            context.setErrorMessage("意图处理失败: " + e.getMessage());
            context.markCompleted(false);
            return context;
        }
    }
    
    @Override
    public int getPriority() {
        return 0; // 默认处理器优先级最低
    }
}

