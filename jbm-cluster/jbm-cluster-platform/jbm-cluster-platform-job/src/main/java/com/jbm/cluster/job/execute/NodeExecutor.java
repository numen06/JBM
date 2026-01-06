package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;

import java.util.Map;

/**
 * @author scolin
 * @description 节点执行器接口
 * @date 2025/10/22 11:35
 */
public interface NodeExecutor {
    NodeExecutionResult execute(NodeData node, Map<String, Object> inputData);
    String getSupportedType();
}
