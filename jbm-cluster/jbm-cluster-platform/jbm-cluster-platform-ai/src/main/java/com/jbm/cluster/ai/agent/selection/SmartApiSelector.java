package com.jbm.cluster.ai.agent.selection;

import cn.hutool.core.collection.CollUtil;
import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.agent.model.Intent;
import com.jbm.cluster.ai.model.ApiMetadata;
import com.jbm.cluster.ai.service.ApiMetadataCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能 API 选择器
 * 
 * 使用多维度匹配算法选择最合适的 API
 * 
 * @author wesley
 */
@Slf4j
@Component
public class SmartApiSelector implements ApiSelector {
    
    @Autowired
    private ApiMetadataCollector apiMetadataCollector;
    
    /**
     * 匹配分数阈值
     */
    @Value("${agent.selection.match-threshold:0.3}")
    private double matchThreshold;
    
    /**
     * 缓存的 API 定义列表
     */
    private List<ApiDefinition> cachedApiDefinitions = new ArrayList<>();
    
    /**
     * 上次更新时间
     */
    private long lastUpdateTime = 0;
    
    /**
     * 缓存有效期（毫秒）
     */
    private static final long CACHE_DURATION = 60000; // 1分钟
    
    @Override
    public ApiDefinition selectBestApi(Intent intent, List<ApiDefinition> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            log.warn("⚠️  [API Selection] 候选 API 列表为空");
            return null;
        }
        
        log.info("🎯 [API Selection] 开始选择 API，候选数量: {}", candidates.size());
        log.info("   意图: {}, 类型: {}, 参数: {}", 
                intent.getName(), intent.getType(), intent.getParams());
        log.info("   用户问题: {}", intent.getRawQuery());
        
        // 计算所有 API 的匹配分数（不过滤）
        List<ApiDefinition> allScoredApis = candidates.stream()
                .peek(api -> api.calculateMatchScore(intent))
                .sorted(Comparator.comparingDouble(ApiDefinition::getMatchScore).reversed()
                        .thenComparingInt(ApiDefinition::getPriority).reversed())
                .collect(Collectors.toList());
        
        // 显示前 10 个 API（即使分数低于阈值也显示）
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📊 [API Selection] 前 10 个候选 API（阈值: {}）:", matchThreshold);
        for (int i = 0; i < Math.min(10, allScoredApis.size()); i++) {
            ApiDefinition api = allScoredApis.get(i);
            String scoreStr = String.format("%.2f", api.getMatchScore());
            boolean qualified = api.getMatchScore() >= matchThreshold;
            
            log.info("   [{}] {} {} - {} {} | {}", 
                    i + 1, 
                    scoreStr,
                    qualified ? "✅" : "❌",
                    api.getMethod(), 
                    api.getPath(),
                    api.getSummary() != null ? api.getSummary() : "");
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 过滤出达到阈值的 API
        List<ApiDefinition> scoredApis = allScoredApis.stream()
                .filter(api -> api.getMatchScore() >= matchThreshold)
                .collect(Collectors.toList());
        
        if (scoredApis.isEmpty()) {
            log.warn("⚠️  [API Selection] 没有找到匹配分数 >= {} 的 API", matchThreshold);
            
            if (!allScoredApis.isEmpty()) {
                ApiDefinition topApi = allScoredApis.get(0);
                log.warn("   最高分 API: {} {}, 分数: {} (未达到阈值)", 
                        topApi.getMethod(), topApi.getPath(), 
                        String.format("%.2f", topApi.getMatchScore()));
                log.warn("   Summary: {}", topApi.getSummary());
            }
            
            log.warn("   建议：降低 match-threshold 到 {} 或优化用户问题", 
                    allScoredApis.isEmpty() ? "0.1" : String.format("%.2f", allScoredApis.get(0).getMatchScore()));
            return null;
        }
        
        ApiDefinition bestApi = scoredApis.get(0);
        log.info("✅ [API Selection] 最终选择: {} {}, 分数: {}", 
                bestApi.getMethod(), bestApi.getPath(), 
                String.format("%.2f", bestApi.getMatchScore()));
        log.info("   服务: {}", bestApi.getServiceName());
        log.info("   描述: {}", bestApi.getSummary());
        
        return bestApi;
    }
    
    @Override
    public List<ApiDefinition> getAllApis() {
        // 检查缓存是否有效
        long now = System.currentTimeMillis();
        if (!cachedApiDefinitions.isEmpty() && 
            (now - lastUpdateTime) < CACHE_DURATION) {
            return cachedApiDefinitions;
        }
        
        // 从 ApiMetadataCollector 获取最新数据
        List<ApiMetadata> allApis = apiMetadataCollector.getAllApis();
        
        if (CollUtil.isEmpty(allApis)) {
            log.warn("⚠️  [API Selection] ApiMetadataCollector 返回空列表");
            return cachedApiDefinitions; // 返回旧缓存
        }
        
        // 转换为 ApiDefinition
        cachedApiDefinitions = allApis.stream()
                .map(ApiDefinition::fromApiMetadata)
                .collect(Collectors.toList());
        
        lastUpdateTime = now;
        
        log.info("🔄 [API Selection] 更新 API 缓存，总数: {}", cachedApiDefinitions.size());
        
        return cachedApiDefinitions;
    }
    
    /**
     * 强制刷新缓存
     */
    public void refreshCache() {
        cachedApiDefinitions.clear();
        lastUpdateTime = 0;
        getAllApis();
    }
}

