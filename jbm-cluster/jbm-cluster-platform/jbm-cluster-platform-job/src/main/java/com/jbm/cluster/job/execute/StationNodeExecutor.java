package com.jbm.cluster.job.execute;

import com.jbm.cluster.api.model.job.rule.NodeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author scolin
 * @description 站点节点执行器（业务节点，暂时不做处理，直接完成到下一步）
 * @date 2025/11/25
 */
@Component
@Slf4j
public class StationNodeExecutor implements NodeExecutor {
    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        log.info("执行站点节点: {}, 节点数据: {}", node.getId(), node.getData());
        // 站点节点属于业务节点，暂时不做处理，直接传递数据到下一步
        return NodeExecutionResult.success(inputData);
    }

    @Override
    public String getSupportedType() {
        return "station";
    }
}
