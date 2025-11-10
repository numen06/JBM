package com.jbm.cluster.ai.agent.dialogue;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.model.ApiParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 上下文参数推断器
 * 
 * 从对话历史、默认值、上下文信息中自动推断参数
 * 
 * @author wesley
 */
@Slf4j
@Component
public class ContextParameterInferrer {
    
    /**
     * 常用参数的默认值映射
     */
    private static final Map<String, Object> DEFAULT_VALUES = Map.ofEntries(
            Map.entry("pageSize", 10),
            Map.entry("pageNum", 1),
            Map.entry("page", 1),
            Map.entry("size", 10),
            Map.entry("limit", 10),
            Map.entry("offset", 0),
            Map.entry("current", 1)
    );
    
    /**
     * 推断参数
     * 
     * @param api API 定义
     * @param currentParams 当前已有参数
     * @param context 上下文信息（可以包含用户信息、历史等）
     * @return 推断出的参数映射
     */
    public Map<String, Object> inferParameters(ApiDefinition api, 
                                                Map<String, Object> currentParams,
                                                Map<String, Object> context) {
        Map<String, Object> inferred = new HashMap<>();
        
        if (api == null || api.getParameters() == null) {
            return inferred;
        }
        
        log.debug("🔮 [参数推断] 开始推断参数");
        
        for (ApiParameter param : api.getParameters()) {
            String paramName = param.getName();
            
            // 如果参数已存在，跳过
            if (currentParams != null && currentParams.containsKey(paramName)) {
                continue;
            }
            
            // 尝试推断
            Object inferredValue = inferParameter(param, context);
            
            if (inferredValue != null) {
                inferred.put(paramName, inferredValue);
                log.info("✅ [参数推断] 推断参数: {} = {}", paramName, inferredValue);
            }
        }
        
        if (!inferred.isEmpty()) {
            log.info("📊 [参数推断] 推断了 {} 个参数", inferred.size());
        }
        
        return inferred;
    }
    
    /**
     * 推断单个参数
     */
    private Object inferParameter(ApiParameter param, Map<String, Object> context) {
        String paramName = param.getName();
        String paramType = param.getType();
        
        // 1. 从默认值映射中查找
        if (DEFAULT_VALUES.containsKey(paramName)) {
            return DEFAULT_VALUES.get(paramName);
        }
        
        // 2. 根据参数名推断时间相关参数
        if (isTimeParameter(paramName)) {
            return inferTimeParameter(paramName, paramType);
        }
        
        // 3. 根据参数名推断用户相关参数
        if (isUserParameter(paramName) && context != null) {
            return inferUserParameter(paramName, context);
        }
        
        // 4. 根据参数描述推断
        if (StrUtil.isNotEmpty(param.getDescription())) {
            return inferFromDescription(param.getDescription(), paramType);
        }
        
        return null;
    }
    
    /**
     * 判断是否为时间参数
     */
    private boolean isTimeParameter(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("time") || 
               lower.contains("date") || 
               lower.equals("startTime") || 
               lower.equals("endTime") ||
               lower.equals("startDate") || 
               lower.equals("endDate") ||
               lower.equals("createTime") ||
               lower.equals("updateTime");
    }
    
    /**
     * 推断时间参数
     */
    private Object inferTimeParameter(String paramName, String paramType) {
        String lower = paramName.toLowerCase();
        
        // 当前时间
        if (lower.contains("current") || lower.equals("now")) {
            return formatTime(LocalDateTime.now(), paramType);
        }
        
        // 开始时间（默认今天开始）
        if (lower.contains("start")) {
            return formatTime(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0), paramType);
        }
        
        // 结束时间（默认今天结束）
        if (lower.contains("end")) {
            return formatTime(LocalDateTime.now().withHour(23).withMinute(59).withSecond(59), paramType);
        }
        
        // 默认当前时间
        return formatTime(LocalDateTime.now(), paramType);
    }
    
    /**
     * 格式化时间
     */
    private Object formatTime(LocalDateTime time, String paramType) {
        if (paramType == null) {
            return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        String lower = paramType.toLowerCase();
        
        // 时间戳
        if (lower.contains("long") || lower.contains("timestamp")) {
            return DateUtil.parse(time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).getTime();
        }
        
        // 日期字符串
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * 判断是否为用户相关参数
     */
    private boolean isUserParameter(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.equals("userid") || 
               lower.equals("user_id") || 
               lower.equals("currentuserid") ||
               lower.equals("operatorid") ||
               lower.equals("creatorid");
    }
    
    /**
     * 推断用户参数
     */
    private Object inferUserParameter(String paramName, Map<String, Object> context) {
        // 从上下文中获取当前用户ID
        if (context.containsKey("userId")) {
            return context.get("userId");
        }
        if (context.containsKey("currentUserId")) {
            return context.get("currentUserId");
        }
        if (context.containsKey("user")) {
            Object user = context.get("user");
            if (user instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userMap = (Map<String, Object>) user;
                if (userMap.containsKey("id")) {
                    return userMap.get("id");
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从描述中推断
     */
    private Object inferFromDescription(String description, String paramType) {
        String lower = description.toLowerCase();
        
        // 分页相关
        if (lower.contains("页码") || lower.contains("page number")) {
            return 1;
        }
        if (lower.contains("每页") || lower.contains("page size")) {
            return 10;
        }
        
        // 状态相关
        if (lower.contains("状态") && lower.contains("默认")) {
            return 1; // 默认正常状态
        }
        
        return null;
    }
    
    /**
     * 推断分页参数
     * 
     * @param currentParams 当前参数
     * @return 补充的分页参数
     */
    public Map<String, Object> inferPaginationParameters(Map<String, Object> currentParams) {
        Map<String, Object> pagination = new HashMap<>();
        
        // 常见的分页参数名
        String[] pageSizeNames = {"pageSize", "size", "limit", "perPage"};
        String[] pageNumNames = {"pageNum", "page", "current", "offset"};
        
        // 检查是否已有分页参数
        boolean hasPageSize = false;
        boolean hasPageNum = false;
        
        if (currentParams != null) {
            for (String name : pageSizeNames) {
                if (currentParams.containsKey(name)) {
                    hasPageSize = true;
                    break;
                }
            }
            for (String name : pageNumNames) {
                if (currentParams.containsKey(name)) {
                    hasPageNum = true;
                    break;
                }
            }
        }
        
        // 补充缺失的分页参数
        if (!hasPageSize) {
            pagination.put("pageSize", 10);
        }
        if (!hasPageNum) {
            pagination.put("pageNum", 1);
        }
        
        return pagination;
    }
}

