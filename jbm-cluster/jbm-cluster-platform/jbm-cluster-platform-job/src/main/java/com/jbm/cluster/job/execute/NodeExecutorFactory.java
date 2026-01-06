package com.jbm.cluster.job.execute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 13:16
 */
@Component
public class NodeExecutorFactory {
    private final Map<String, NodeExecutor> executorMap = new HashMap<>();

    @Autowired
    public NodeExecutorFactory(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            executorMap.put(executor.getSupportedType(), executor);
        }
    }

    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor == null) {
            throw new RuntimeException("不支持的节点类型: " + nodeType);
        }
        return executor;
    }
}
