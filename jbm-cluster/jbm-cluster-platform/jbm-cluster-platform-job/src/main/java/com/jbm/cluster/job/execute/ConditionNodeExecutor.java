package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:41
 */
@Component
public class ConditionNodeExecutor implements NodeExecutor{
    @Autowired
    private DroolsRuleEngine droolsRuleEngine;

    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        // 条件节点的执行在流程引擎中处理
        // 这里只是传递数据
        return NodeExecutionResult.success(inputData);
    }

    @Override
    public String getSupportedType() {
        return "conditions";
    }
}
