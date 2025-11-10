package com.jbm.cluster.ai.agent.nlu;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.jbm.cluster.ai.agent.model.Intent;
import com.jbm.cluster.ai.config.DashScopeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 意图识别器
 * 
 * 使用通义千问模型识别用户意图和提取参数
 * 
 * @author wesley
 */
@Slf4j
@Component
public class AiIntentRecognizer implements IntentRecognizer {
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    @Autowired
    private com.jbm.cluster.ai.service.ApiMetadataCollector apiMetadataCollector;
    
    @Autowired
    private com.jbm.cluster.ai.agent.config.AgentProperties agentProperties;
    
    /**
     * 意图识别的 System Prompt 模板
     */
    private static final String NLU_SYSTEM_PROMPT_TEMPLATE = """
            你是一个专业的自然语言理解（NLU）系统，负责分析用户问题并识别意图。
            
            系统能力范围：
            {API_OVERVIEW}
            
            任务：分析用户问题，识别意图并提取参数
            
            输出格式（必须是严格的 JSON）：
            {
              "intent": "意图名称",
              "type": "意图类型",
              "confidence": 0.95,
              "params": {
                "参数名": "参数值"
              }
            }
            
            意图类型包括：
            - QUERY: 查询类（查询、获取、列出、显示）
            - CREATE: 创建类（创建、新建、添加）
            - UPDATE: 更新类（更新、修改、编辑）
            - DELETE: 删除类（删除、移除、清除）
            - OTHER: 其他（对话、问候等不需要调用API的请求）
            
            意图名称规则：
            - 使用小写英文和下划线
            - 格式：动作_对象（如 query_user_info, list_online_users）
            - 参考上面的系统能力，尽可能具体和准确
            - 如果是对话型（问候、介绍等），使用 OTHER 类型
            
            参数提取规则：
            - 提取所有可识别的参数
            - 参数名要与 API 参数匹配（参考系统能力）
            - 参数值尽可能具体
            - 时间范围转换为秒数（如"5分钟"转为"300"）
            - ID类参数保持原格式
            
            示例：
            
            用户："查询用户ID为123的信息"
            输出：{
              "intent": "query_user_info",
              "type": "QUERY",
              "confidence": 0.95,
              "params": {"userId": "123"}
            }
            
            用户："最近5分钟内在线的用户有哪些"
            输出：{
              "intent": "list_online_users",
              "type": "QUERY",
              "confidence": 0.9,
              "params": {"timeRange": "300"}
            }
            
            用户："你好"
            输出：{
              "intent": "greeting",
              "type": "OTHER",
              "confidence": 1.0,
              "params": {}
            }
            
            重要：只输出 JSON，不要有任何其他文字。
            """;
    
    @Override
    public Intent recognize(String userQuery) {
        if (StrUtil.isEmpty(userQuery)) {
            return createDefaultIntent(userQuery);
        }
        
        try {
            log.info("🔍 [NLU] 开始意图识别: {}", userQuery);
            
            // 构建 API 概述（给 AI 提供系统能力信息）
            String apiOverview = buildApiOverview();
            
            // 将 API 概述注入到 System Prompt
            String systemPrompt = NLU_SYSTEM_PROMPT_TEMPLATE.replace("{API_OVERVIEW}", apiOverview);
            
            // 构建消息
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build());
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userQuery)
                    .build());
            
            // 构建生成参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(dashScopeConfig.getApiKey())
                    .model(dashScopeConfig.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(0.1f)  // 极低温度，确保输出稳定
                    .maxTokens(500)     // 意图识别不需要太长
                    .build();
            
            // 调用 AI 模型
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 解析响应
            if (result != null && result.getOutput() != null && 
                result.getOutput().getChoices() != null && 
                !result.getOutput().getChoices().isEmpty()) {
                
                String aiResponse = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.info("🤖 [NLU] AI 响应: {}", aiResponse);
                
                // 解析 JSON
                Intent intent = parseIntentFromJson(aiResponse, userQuery);
                log.info("✅ [NLU] 意图识别完成: intent={}, confidence={}", 
                        intent.getName(), intent.getConfidence());
                
                return intent;
            }
            
        } catch (Exception e) {
            log.error("❌ [NLU] 意图识别失败: {}", e.getMessage(), e);
        }
        
        // 失败时返回默认意图
        return createDefaultIntent(userQuery);
    }
    
    /**
     * 从 JSON 字符串解析意图
     */
    private Intent parseIntentFromJson(String jsonStr, String rawQuery) {
        try {
            // 尝试提取 JSON（可能包含其他文字）
            String cleanJson = extractJson(jsonStr);
            JSONObject json = JSONUtil.parseObj(cleanJson);
            
            Intent intent = new Intent();
            intent.setRawQuery(rawQuery);
            intent.setName(json.getStr("intent", "unknown"));
            intent.setConfidence(json.getDouble("confidence", 0.5));
            
            // 解析意图类型
            String typeStr = json.getStr("type", "OTHER");
            try {
                intent.setType(Intent.IntentType.valueOf(typeStr));
            } catch (Exception e) {
                intent.setType(Intent.IntentType.OTHER);
            }
            
            // 解析参数
            JSONObject paramsJson = json.getJSONObject("params");
            if (paramsJson != null) {
                Map<String, Object> params = new HashMap<>();
                for (String key : paramsJson.keySet()) {
                    params.put(key, paramsJson.get(key));
                }
                intent.setParams(params);
            }
            
            return intent;
            
        } catch (Exception e) {
            log.warn("⚠️  [NLU] JSON 解析失败，使用默认意图: {}", e.getMessage());
            return createDefaultIntent(rawQuery);
        }
    }
    
    /**
     * 从文本中提取 JSON
     */
    private String extractJson(String text) {
        // 查找 { 和 }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return text;
    }
    
    /**
     * 创建默认意图
     */
    private Intent createDefaultIntent(String rawQuery) {
        Intent intent = new Intent();
        intent.setRawQuery(rawQuery);
        intent.setName("general_query");
        intent.setType(Intent.IntentType.OTHER);
        intent.setConfidence(0.3);
        intent.setParams(new HashMap<>());
        
        log.warn("⚠️  [NLU] 使用默认意图");
        
        return intent;
    }
    
    /**
     * 构建 API 概述信息
     * 
     * 按服务分组，提供简洁的 API 能力描述
     */
    private String buildApiOverview() {
        try {
            List<com.jbm.cluster.ai.model.ApiMetadata> allApis = apiMetadataCollector.getAllApis();
            
            if (allApis == null || allApis.isEmpty()) {
                return "系统暂无可用 API";
            }
            
            // 按服务分组
            Map<String, List<com.jbm.cluster.ai.model.ApiMetadata>> groupedByService = new java.util.LinkedHashMap<>();
            
            for (com.jbm.cluster.ai.model.ApiMetadata api : allApis) {
                groupedByService.computeIfAbsent(api.getServiceName(), k -> new ArrayList<>()).add(api);
            }
            
            // 构建概述（限制长度，避免超过 token 限制）
            StringBuilder overview = new StringBuilder();
            overview.append("\n");
            
            int totalServices = 0;
            int maxServices = agentProperties.getNlu().getMaxServices(); // 从配置读取
            int maxApisPerService = agentProperties.getNlu().getMaxApisPerService(); // 从配置读取
            
            for (Map.Entry<String, List<com.jbm.cluster.ai.model.ApiMetadata>> entry : groupedByService.entrySet()) {
                if (totalServices >= maxServices) {
                    int remainingServices = groupedByService.size() - totalServices;
                    overview.append(String.format("\n... 还有 %d 个服务未列出\n", remainingServices));
                    break;
                }
                
                String serviceName = entry.getKey();
                List<com.jbm.cluster.ai.model.ApiMetadata> apis = entry.getValue();
                
                overview.append(String.format("\n【%s】(%d个接口)\n", serviceName, apis.size()));
                
                // 提取主要标签/功能
                java.util.Set<String> tags = new java.util.LinkedHashSet<>();
                for (int i = 0; i < Math.min(maxApisPerService, apis.size()); i++) {
                    com.jbm.cluster.ai.model.ApiMetadata api = apis.get(i);
                    overview.append(String.format("  • %s %s - %s\n", 
                            api.getMethod(), 
                            api.getPath(), 
                            api.getSummary() != null ? api.getSummary() : ""));
                    
                    if (api.getTags() != null) {
                        tags.addAll(api.getTags());
                    }
                }
                
                if (apis.size() > maxApisPerService) {
                    overview.append(String.format("  ... 还有 %d 个接口\n", apis.size() - maxApisPerService));
                }
                
                totalServices++;
            }
            
            overview.append(String.format("\n总计：%d 个服务，%d 个接口\n", 
                    groupedByService.size(), allApis.size()));
            
            String result = overview.toString();
            log.debug("📋 [NLU] API 概述长度: {} 字符", result.length());
            
            return result;
            
        } catch (Exception e) {
            log.warn("⚠️  [NLU] 构建 API 概述失败: {}", e.getMessage());
            return "系统 API 信息暂时不可用";
        }
    }
}

