package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author scolin
 * @description 开始节点执行器
 * @date 2025/10/22 17:53
 */
@Component
public class StartNodeExecutor implements NodeExecutor {
    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        // 开始节点只是传递输入参数
        return NodeExecutionResult.success(inputData);
    }

    @Override
    public String getSupportedType() {
        return "start";
    }
}
