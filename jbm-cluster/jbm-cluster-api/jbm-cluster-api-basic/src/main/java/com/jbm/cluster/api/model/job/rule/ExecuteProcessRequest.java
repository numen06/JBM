package com.jbm.cluster.api.model.job.rule;

import lombok.Data;

import java.util.Map;

/**
 * @author scolin
 * @description 流程执行请求
 * @date 2025/10/22 11:11
 */
@Data
public class ExecuteProcessRequest {
    private String ruleDefinitionId;
    private String processInstanceId;
    private Map<String, Object> inputParams;
    private String triggerNodeId; // 用于继续执行的触发节点ID
    private String triggerData; // 触发数据
}
