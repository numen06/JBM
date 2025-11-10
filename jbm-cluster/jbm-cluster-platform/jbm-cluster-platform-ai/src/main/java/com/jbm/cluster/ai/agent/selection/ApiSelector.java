package com.jbm.cluster.ai.agent.selection;

import com.jbm.cluster.ai.agent.model.ApiDefinition;
import com.jbm.cluster.ai.agent.model.Intent;

import java.util.List;

/**
 * API 选择器接口
 * 
 * 根据意图从候选 API 列表中选择最合适的一个
 * 
 * @author wesley
 */
public interface ApiSelector {
    
    /**
     * 选择最佳 API
     * 
     * @param intent 用户意图
     * @param candidates 候选 API 列表
     * @return 最匹配的 API，如果没有合适的返回 null
     */
    ApiDefinition selectBestApi(Intent intent, List<ApiDefinition> candidates);
    
    /**
     * 获取所有候选 API
     * 
     * @return 所有可用的 API
     */
    List<ApiDefinition> getAllApis();
}

