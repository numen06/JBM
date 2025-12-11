package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author scolin
 * @description 结束节点执行器
 * @date 2025/10/22 13:15
 */
@Component
public class EndNodeExecutor implements NodeExecutor {
    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        return NodeExecutionResult.success(inputData);
    }

    @Override
    public String getSupportedType() {
        return "end";
    }
}
