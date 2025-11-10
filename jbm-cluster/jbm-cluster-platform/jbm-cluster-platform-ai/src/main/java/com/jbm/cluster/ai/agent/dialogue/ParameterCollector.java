package com.jbm.cluster.ai.agent.dialogue;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.model.ApiParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数收集器
 * 
 * 分析 API 定义，识别缺失的必填参数，并生成自然语言提问
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ParameterCollector {
    
    /**
     * 参数类型对应的中文描述
     */
    private static final Map<String, String> TYPE_DESCRIPTIONS = Map.of(
            "string", "文本",
            "integer", "整数",
            "number", "数字",
            "boolean", "布尔值（true/false）",
            "array", "数组",
            "object", "对象"
    );
    
    /**
     * 分析并识别缺失的参数
     * 
     * @param api API 定义
     * @param currentParams 当前已有的参数
     * @return 缺失的必填参数列表
     */
    public List<String> identifyMissingParameters(ApiDefinition api, Map<String, Object> currentParams) {
        if (api == null || api.getParameters() == null || api.getParameters().isEmpty()) {
            log.debug("📋 [参数收集] API 无参数定义");
            return Collections.emptyList();
        }
        
        List<String> missingParams = new ArrayList<>();
        Map<String, Object> params = currentParams != null ? currentParams : Collections.emptyMap();
        
        for (ApiParameter param : api.getParameters()) {
            // 只检查必填参数
            if (param.isRequired()) {
                Object value = params.get(param.getName());
                
                // 检查参数是否缺失或为空
                if (value == null || (value instanceof String && StrUtil.isEmpty((String) value))) {
                    missingParams.add(param.getName());
                    log.debug("  ⚠️  缺失必填参数: {}", param.getName());
                }
            }
        }
        
        log.info("📋 [参数收集] 识别到 {} 个缺失的必填参数: {}", 
                missingParams.size(), missingParams);
        
        return missingParams;
    }
    
    /**
     * 为指定参数生成提问
     * 
     * @param api API 定义
     * @param parameterName 参数名称
     * @return 自然语言提问
     */
    public String generateQuestion(ApiDefinition api, String parameterName) {
        if (api == null || StrUtil.isEmpty(parameterName)) {
            return "请提供参数值";
        }
        
        // 查找参数定义
        ApiParameter param = findParameter(api, parameterName);
        
        if (param == null) {
            // 没有参数定义，使用默认提问
            return generateDefaultQuestion(parameterName);
        }
        
        // 根据参数定义生成提问
        return generateQuestionFromParameter(param);
    }
    
    /**
     * 根据参数定义生成提问
     */
    private String generateQuestionFromParameter(ApiParameter param) {
        StringBuilder question = new StringBuilder();
        
        // 如果有描述，优先使用描述
        if (StrUtil.isNotEmpty(param.getDescription())) {
            question.append("请提供").append(param.getDescription());
        } else {
            // 使用参数名生成友好提问
            String friendlyName = convertToFriendlyName(param.getName());
            question.append("请提供").append(friendlyName);
        }
        
        // 添加类型提示
        if (StrUtil.isNotEmpty(param.getType())) {
            String typeDesc = TYPE_DESCRIPTIONS.getOrDefault(param.getType().toLowerCase(), param.getType());
            question.append("（类型：").append(typeDesc).append("）");
        }
        
        // 添加示例（如果有）
        if (param.getExample() != null) {
            question.append("\n示例：").append(param.getExample());
        }
        
        return question.toString();
    }
    
    /**
     * 生成默认提问（当没有参数定义时）
     */
    private String generateDefaultQuestion(String parameterName) {
        String friendlyName = convertToFriendlyName(parameterName);
        return "请提供" + friendlyName;
    }
    
    /**
     * 将参数名转换为友好的中文描述
     * 
     * 例如：userId -> 用户ID, userName -> 用户名称
     */
    private String convertToFriendlyName(String parameterName) {
        if (StrUtil.isEmpty(parameterName)) {
            return "参数";
        }
        
        // 常见参数名映射
        Map<String, String> commonMappings = Map.ofEntries(
                Map.entry("id", "ID"),
                Map.entry("userId", "用户ID"),
                Map.entry("userName", "用户名"),
                Map.entry("name", "名称"),
                Map.entry("email", "邮箱地址"),
                Map.entry("phone", "手机号码"),
                Map.entry("mobile", "手机号码"),
                Map.entry("address", "地址"),
                Map.entry("date", "日期"),
                Map.entry("startDate", "开始日期"),
                Map.entry("endDate", "结束日期"),
                Map.entry("startTime", "开始时间"),
                Map.entry("endTime", "结束时间"),
                Map.entry("pageSize", "每页数量"),
                Map.entry("pageNum", "页码"),
                Map.entry("status", "状态"),
                Map.entry("type", "类型"),
                Map.entry("title", "标题"),
                Map.entry("content", "内容"),
                Map.entry("description", "描述"),
                Map.entry("remark", "备注")
        );
        
        // 检查是否有直接映射
        if (commonMappings.containsKey(parameterName)) {
            return commonMappings.get(parameterName);
        }
        
        // 尝试驼峰拆分
        String friendly = splitCamelCase(parameterName);
        
        return friendly;
    }
    
    /**
     * 拆分驼峰命名
     * 
     * 例如：userName -> 用户 Name, orderId -> 订单 Id
     */
    private String splitCamelCase(String text) {
        if (StrUtil.isEmpty(text)) {
            return text;
        }
        
        // 在大写字母前插入空格
        String spaced = text.replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // 转换为首字母大写
        return Arrays.stream(spaced.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
    
    /**
     * 查找参数定义
     */
    private ApiParameter findParameter(ApiDefinition api, String parameterName) {
        if (api.getParameters() == null) {
            return null;
        }
        
        return api.getParameters().stream()
                .filter(p -> p.getName().equals(parameterName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 验证参数值
     * 
     * @param api API 定义
     * @param parameterName 参数名
     * @param value 参数值
     * @return 验证结果
     */
    public ValidationResult validateParameter(ApiDefinition api, String parameterName, Object value) {
        ApiParameter param = findParameter(api, parameterName);
        
        if (param == null) {
            // 没有参数定义，默认通过
            return ValidationResult.success();
        }
        
        // 检查空值
        if (value == null || (value instanceof String && StrUtil.isEmpty((String) value))) {
            if (param.isRequired()) {
                return ValidationResult.failure("参数不能为空");
            }
            return ValidationResult.success();
        }
        
        // 类型验证
        String type = param.getType();
        if (StrUtil.isNotEmpty(type)) {
            if (!validateType(value, type)) {
                return ValidationResult.failure(
                        String.format("参数类型不匹配，期望 %s", TYPE_DESCRIPTIONS.getOrDefault(type, type))
                );
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证类型
     */
    private boolean validateType(Object value, String expectedType) {
        String lowerType = expectedType.toLowerCase();
        
        switch (lowerType) {
            case "string":
                return value instanceof String;
            case "integer":
            case "int":
                return value instanceof Integer || value instanceof Long || 
                       (value instanceof String && ((String) value).matches("-?\\d+"));
            case "number":
            case "double":
            case "float":
                return value instanceof Number || 
                       (value instanceof String && ((String) value).matches("-?\\d+(\\.\\d+)?"));
            case "boolean":
            case "bool":
                return value instanceof Boolean || 
                       (value instanceof String && ("true".equalsIgnoreCase((String) value) || 
                                                    "false".equalsIgnoreCase((String) value)));
            case "array":
                return value instanceof List || value instanceof Object[];
            case "object":
                return value instanceof Map;
            default:
                // 未知类型，默认通过
                return true;
        }
    }
    
    /**
     * 批量生成多个参数的提问
     * 
     * @param api API 定义
     * @param missingParameters 缺失的参数列表
     * @return 参数名到提问的映射
     */
    public Map<String, String> generateQuestionsForParameters(ApiDefinition api, List<String> missingParameters) {
        if (missingParameters == null || missingParameters.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, String> questions = new LinkedHashMap<>();
        
        for (String paramName : missingParameters) {
            String question = generateQuestion(api, paramName);
            questions.put(paramName, question);
        }
        
        return questions;
    }
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

