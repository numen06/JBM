package com.jbm.cluster.ai.agent.model;

import com.jbm.cluster.ai.model.ApiMetadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * API 定义
 * 
 * 扩展 ApiMetadata，添加意图匹配相关字段
 * 
 * @author wesley
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ApiDefinition extends ApiMetadata {
    
    /**
     * 关联的意图模式（从 tags 和 description 动态生成）
     * 例如：["query_user", "get_user_info"]
     */
    private List<String> intentPatterns = new ArrayList<>();
    
    /**
     * 优先级（默认为 0）
     * 数值越大优先级越高，用于在多个匹配 API 中选择
     */
    private int priority = 0;
    
    /**
     * 与用户意图的匹配分数（运行时计算）
     * 范围 0-1，越接近 1 表示匹配度越高
     */
    private double matchScore = 0.0;
    
    /**
     * API 版本
     * 从路径中提取，如 /v1/, /v2/
     */
    private String version = "v1";
    
    /**
     * 是否已废弃
     */
    private boolean deprecated = false;
    
    /**
     * 从 ApiMetadata 创建 ApiDefinition
     */
    public static ApiDefinition fromApiMetadata(ApiMetadata metadata) {
        ApiDefinition definition = new ApiDefinition();
        
        // 复制基础字段
        definition.setServiceName(metadata.getServiceName());
        definition.setPath(metadata.getPath());
        definition.setMethod(metadata.getMethod());
        definition.setSummary(metadata.getSummary());
        definition.setDescription(metadata.getDescription());
        definition.setTags(metadata.getTags());
        definition.setParameters(metadata.getParameters());
        definition.setResponseType(metadata.getResponseType());
        definition.setRequiresAuth(metadata.isRequiresAuth());
        
        // 从路径提取版本
        definition.extractVersionFromPath();
        
        // 从 tags 和 description 生成意图模式
        definition.generateIntentPatterns();
        
        return definition;
    }
    
    /**
     * 从路径提取版本号
     */
    private void extractVersionFromPath() {
        if (getPath() != null && getPath().contains("/v")) {
            String path = getPath();
            int vIndex = path.indexOf("/v");
            if (vIndex >= 0 && vIndex + 3 < path.length()) {
                String versionPart = path.substring(vIndex + 1, vIndex + 3);
                if (versionPart.matches("v\\d+")) {
                    this.version = versionPart;
                }
            }
        }
    }
    
    /**
     * 从 tags 和 description 生成意图模式
     */
    private void generateIntentPatterns() {
        this.intentPatterns = new ArrayList<>();
        
        // 从 tags 生成
        if (getTags() != null) {
            for (String tag : getTags()) {
                String pattern = tag.toLowerCase()
                        .replaceAll("\\s+", "_")
                        .replaceAll("[^a-z0-9_]", "");
                if (!pattern.isEmpty()) {
                    intentPatterns.add(pattern);
                }
            }
        }
        
        // 从 summary 生成
        if (getSummary() != null) {
            String pattern = getSummary().toLowerCase()
                    .replaceAll("\\s+", "_")
                    .replaceAll("[^a-z0-9_]", "");
            if (!pattern.isEmpty() && pattern.length() <= 50) {
                intentPatterns.add(pattern);
            }
        }
    }
    
    /**
     * 计算与意图的匹配分数（基于 Summary 优先）
     * 
     * API 文档的 summary 字段已经非常清楚，应该作为主要匹配依据
     */
    public double calculateMatchScore(Intent intent) {
        double score = 0.0;
        
        String rawQuery = intent.getRawQuery() != null ? intent.getRawQuery().toLowerCase() : "";
        String apiSummary = getSummary() != null ? getSummary().toLowerCase() : "";
        String apiPath = getPath() != null ? getPath().toLowerCase() : "";
        
        // 清理 summary 中的特殊字符（【】等）
        String cleanSummary = apiSummary.replaceAll("[【】\\[\\]]", "");
        String cleanQuery = rawQuery.replaceAll("[【】\\[\\]]", "");
        
        // 1. Summary 完全匹配（权重 80%）- 最重要！
        if (cleanSummary.equals(cleanQuery)) {
            score += 0.8; // 完全匹配
            log.debug("   ✨ Summary 完全匹配: {}", getSummary());
        } else if (cleanSummary.contains(cleanQuery) || cleanQuery.contains(cleanSummary)) {
            score += 0.7; // 包含关系
            log.debug("   ✨ Summary 包含匹配: {}", getSummary());
        } else {
            // 计算关键词覆盖率
            String[] queryKeywords = cleanQuery.split("[\\s，。的]+");
            int summaryMatches = 0;
            int validKeywords = 0;
            
            for (String keyword : queryKeywords) {
                if (keyword.length() <= 1) continue;
                validKeywords++;
                
                if (cleanSummary.contains(keyword)) {
                    summaryMatches++;
                }
            }
            
            if (validKeywords > 0) {
                double summaryScore = (double) summaryMatches / validKeywords;
                score += summaryScore * 0.8;
                
                if (summaryMatches > 0) {
                    log.debug("   📝 Summary 关键词匹配: {}/{} - {}", 
                            summaryMatches, validKeywords, getSummary());
                }
            }
        }
        
        // 2. Tags 匹配（权重 10%）
        if (getTags() != null && !getTags().isEmpty()) {
            for (String tag : getTags()) {
                String cleanTag = tag.toLowerCase().replaceAll("[【】\\[\\]]", "");
                
                String[] queryKeywords = cleanQuery.split("[\\s，。的]+");
                for (String keyword : queryKeywords) {
                    if (keyword.length() > 1 && cleanTag.contains(keyword)) {
                        score += 0.1;
                        log.debug("   🏷️  Tag 匹配: {}", tag);
                        break;
                    }
                }
            }
        }
        
        // 3. 参数匹配（权重 10%）
        if (intent.getParams() != null && !intent.getParams().isEmpty() && 
            getParameters() != null && !getParameters().isEmpty()) {
            
            int paramMatches = 0;
            for (String paramKey : intent.getParams().keySet()) {
                for (var apiParam : getParameters()) {
                    if (apiParam.getName().equalsIgnoreCase(paramKey)) {
                        paramMatches++;
                        break;
                    }
                }
            }
            
            if (!intent.getParams().isEmpty()) {
                double paramScore = (double) paramMatches / intent.getParams().size();
                score += paramScore * 0.1;
            }
        } else if ((intent.getParams() == null || intent.getParams().isEmpty())) {
            // 无参数查询，优先选择无必填参数的 API
            boolean hasRequiredParams = getParameters() != null && 
                    getParameters().stream().anyMatch(p -> p.isRequired());
            if (!hasRequiredParams) {
                score += 0.05;
            }
        }
        
        // 设置匹配分数
        this.matchScore = Math.min(1.0, score);
        
        return this.matchScore;
    }
}

