package com.jbm.cluster.ai.agent.nlu;

import com.jbm.cluster.ai.agent.model.Intent;

/**
 * 意图识别器接口
 * 
 * 负责从用户问题中识别意图和提取参数
 * 
 * @author wesley
 */
public interface IntentRecognizer {
    
    /**
     * 识别用户意图
     * 
     * @param userQuery 用户问题
     * @return 识别出的意图
     */
    Intent recognize(String userQuery);
    
    /**
     * 批量识别意图
     * 
     * @param userQueries 用户问题列表
     * @return 识别出的意图列表
     */
    default java.util.List<Intent> recognizeBatch(java.util.List<String> userQueries) {
        return userQueries.stream()
                .map(this::recognize)
                .collect(java.util.stream.Collectors.toList());
    }
}

