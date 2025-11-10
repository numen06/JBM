package com.jbm.cluster.ai.agent.dialogue;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.config.DashScopeConfig;
import com.jbm.cluster.ai.model.ApiParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 参数提取器
 * 
 * 使用 AI 从用户的自然语言回复中提取参数值
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ParameterExtractor {
    
    @Autowired
    private DashScopeConfig dashScopeConfig;
    
    /**
     * 提取单个参数的 System Prompt 模板
     */
    private static final String EXTRACTION_SYSTEM_PROMPT = """
            你是一个专业的参数提取器，负责从用户的自然语言回复中提取参数值。
            
            任务：从用户的回复中提取指定参数的值
            
            输出格式（必须是严格的 JSON）：
            {
              "success": true,
              "value": "提取的值",
              "confidence": 0.95
            }
            
            或者无法提取时：
            {
              "success": false,
              "reason": "无法识别参数值"
            }
            
            提取规则：
            1. 仔细理解参数的含义和类型
            2. 从用户回复中识别相关信息
            3. 进行必要的格式转换（如"明天" → 日期，"是的" → true）
            4. 如果用户明确表示不知道或拒绝，返回 success=false
            5. 保持提取值的原始类型（数字、布尔值、字符串等）
            
            示例1：
            参数：userId (用户ID, 类型: integer)
            用户回复："123"
            输出：{"success": true, "value": 123, "confidence": 1.0}
            
            示例2：
            参数：email (邮箱地址, 类型: string)
            用户回复："我的邮箱是 test@example.com"
            输出：{"success": true, "value": "test@example.com", "confidence": 0.95}
            
            示例3：
            参数：startDate (开始日期, 类型: string)
            用户回复："明天"
            输出：{"success": true, "value": "2024-01-15", "confidence": 0.9}
            
            示例4：
            参数：isActive (是否激活, 类型: boolean)
            用户回复："是的"
            输出：{"success": true, "value": true, "confidence": 1.0}
            
            示例5：
            参数：count (数量, 类型: integer)
            用户回复："不知道"
            输出：{"success": false, "reason": "用户表示不知道"}
            
            重要：只输出 JSON，不要有任何其他文字。
            """;
    
    /**
     * 从用户回复中提取参数值
     * 
     * @param api API 定义
     * @param parameterName 参数名
     * @param userReply 用户回复
     * @return 提取结果
     */
    public ExtractionResult extractParameter(ApiDefinition api, String parameterName, String userReply) {
        if (StrUtil.isEmpty(userReply)) {
            return ExtractionResult.failure("用户回复为空");
        }
        
        log.info("🔍 [参数提取] 开始提取参数: {}", parameterName);
        log.info("   用户回复: {}", userReply);
        
        try {
            // 构建参数信息
            String parameterInfo = buildParameterInfo(api, parameterName);
            
            // 构建用户消息
            String userMessage = String.format(
                    "参数信息：\n%s\n\n用户回复：\n%s",
                    parameterInfo,
                    userReply
            );
            
            // 调用 AI 提取
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(EXTRACTION_SYSTEM_PROMPT)
                    .build());
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build());
            
            GenerationParam param = GenerationParam.builder()
                    .apiKey(dashScopeConfig.getApiKey())
                    .model(dashScopeConfig.getModel())
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(0.1f)  // 低温度，确保稳定输出
                    .maxTokens(300)
                    .build();
            
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            
            // 解析响应
            if (result != null && result.getOutput() != null && 
                result.getOutput().getChoices() != null && 
                !result.getOutput().getChoices().isEmpty()) {
                
                String aiResponse = result.getOutput().getChoices().get(0).getMessage().getContent();
                log.info("🤖 [参数提取] AI 响应: {}", aiResponse);
                
                // 解析 JSON
                ExtractionResult extractionResult = parseExtractionResult(aiResponse, parameterName);
                
                if (extractionResult.isSuccess()) {
                    log.info("✅ [参数提取] 提取成功: {} = {}", 
                            parameterName, extractionResult.getValue());
                } else {
                    log.warn("⚠️  [参数提取] 提取失败: {}", extractionResult.getReason());
                }
                
                return extractionResult;
            }
            
        } catch (Exception e) {
            log.error("❌ [参数提取] 提取异常: {}", e.getMessage(), e);
            return ExtractionResult.failure("提取失败: " + e.getMessage());
        }
        
        return ExtractionResult.failure("AI 未返回有效结果");
    }
    
    /**
     * 构建参数信息描述
     */
    private String buildParameterInfo(ApiDefinition api, String parameterName) {
        if (api == null || api.getParameters() == null) {
            return String.format("参数名: %s", parameterName);
        }
        
        ApiParameter param = api.getParameters().stream()
                .filter(p -> p.getName().equals(parameterName))
                .findFirst()
                .orElse(null);
        
        if (param == null) {
            return String.format("参数名: %s", parameterName);
        }
        
        StringBuilder info = new StringBuilder();
        info.append("参数名: ").append(param.getName()).append("\n");
        
        if (StrUtil.isNotEmpty(param.getDescription())) {
            info.append("说明: ").append(param.getDescription()).append("\n");
        }
        
        if (StrUtil.isNotEmpty(param.getType())) {
            info.append("类型: ").append(param.getType()).append("\n");
        }
        
        if (param.getExample() != null) {
            info.append("示例: ").append(param.getExample()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * 解析提取结果
     */
    private ExtractionResult parseExtractionResult(String jsonStr, String parameterName) {
        try {
            // 提取 JSON
            String cleanJson = extractJson(jsonStr);
            JSONObject json = JSONUtil.parseObj(cleanJson);
            
            boolean success = json.getBool("success", false);
            
            if (success) {
                Object value = json.get("value");
                double confidence = json.getDouble("confidence", 0.5);
                
                return ExtractionResult.success(value, confidence);
            } else {
                String reason = json.getStr("reason", "未知原因");
                return ExtractionResult.failure(reason);
            }
            
        } catch (Exception e) {
            log.warn("⚠️  [参数提取] JSON 解析失败: {}", e.getMessage());
            
            // 尝试直接使用用户回复作为值
            return tryDirectExtraction(jsonStr, parameterName);
        }
    }
    
    /**
     * 从文本中提取 JSON
     */
    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return text;
    }
    
    /**
     * 尝试直接提取（当 AI 返回格式不正确时）
     */
    private ExtractionResult tryDirectExtraction(String text, String parameterName) {
        // 去除可能的引号
        String cleaned = text.trim().replaceAll("^[\"']|[\"']$", "");
        
        // 尝试解析为数字
        try {
            if (cleaned.matches("-?\\d+")) {
                return ExtractionResult.success(Integer.parseInt(cleaned), 0.5);
            }
            if (cleaned.matches("-?\\d+\\.\\d+")) {
                return ExtractionResult.success(Double.parseDouble(cleaned), 0.5);
            }
        } catch (Exception ignored) {
        }
        
        // 尝试解析为布尔值
        if ("true".equalsIgnoreCase(cleaned) || "是".equals(cleaned) || "yes".equalsIgnoreCase(cleaned)) {
            return ExtractionResult.success(true, 0.6);
        }
        if ("false".equalsIgnoreCase(cleaned) || "否".equals(cleaned) || "no".equalsIgnoreCase(cleaned)) {
            return ExtractionResult.success(false, 0.6);
        }
        
        // 默认作为字符串
        if (StrUtil.isNotEmpty(cleaned)) {
            return ExtractionResult.success(cleaned, 0.3);
        }
        
        return ExtractionResult.failure("无法从回复中提取有效值");
    }
    
    /**
     * 批量提取多个参数（如果用户一次提供了多个参数）
     * 
     * @param api API 定义
     * @param missingParameters 缺失的参数列表
     * @param userReply 用户回复
     * @return 提取的参数映射
     */
    public Map<String, Object> extractMultipleParameters(ApiDefinition api, 
                                                          List<String> missingParameters, 
                                                          String userReply) {
        Map<String, Object> extracted = new HashMap<>();
        
        if (missingParameters == null || missingParameters.isEmpty()) {
            return extracted;
        }
        
        log.info("🔍 [参数提取] 尝试批量提取 {} 个参数", missingParameters.size());
        
        // 逐个尝试提取
        for (String paramName : missingParameters) {
            ExtractionResult result = extractParameter(api, paramName, userReply);
            
            if (result.isSuccess() && result.getConfidence() > 0.5) {
                extracted.put(paramName, result.getValue());
                log.info("  ✅ {} = {}", paramName, result.getValue());
            }
        }
        
        log.info("📊 [参数提取] 批量提取完成: 成功 {} / {}", 
                extracted.size(), missingParameters.size());
        
        return extracted;
    }
    
    /**
     * 提取结果
     */
    public static class ExtractionResult {
        private boolean success;
        private Object value;
        private double confidence;
        private String reason;
        
        private ExtractionResult(boolean success, Object value, double confidence, String reason) {
            this.success = success;
            this.value = value;
            this.confidence = confidence;
            this.reason = reason;
        }
        
        public static ExtractionResult success(Object value, double confidence) {
            return new ExtractionResult(true, value, confidence, null);
        }
        
        public static ExtractionResult failure(String reason) {
            return new ExtractionResult(false, null, 0.0, reason);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Object getValue() {
            return value;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public String getReason() {
            return reason;
        }
    }
}

