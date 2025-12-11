package com.jbm.cluster.api.model.job.rule;

import lombok.Data;

import java.util.Map;

/**
 * @author scolin
 * @description 流程执行响应
 * @date 2025/10/22 11:12
 */
@Data
public class ExecuteProcessResponse {
    private String processInstanceId;
    private String status;
    private Map<String, Object> outputParams;
    private String currentNodeId;
    private boolean waitingForTrigger;
    private String message;
}
