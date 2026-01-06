package com.jbm.cluster.job.execute;

import lombok.Data;

import java.util.Map;

/**
 * @author scolin
 * @description 节点执行结果
 * @date 2025/10/22 11:34
 */
@Data
public class NodeExecutionResult {
    private boolean success;
    private boolean waitingForTrigger;
    private String triggerType;
    private String triggerKey;
    private Map<String, Object> outputData;
    private String errorMessage;
    private String message;

    public static NodeExecutionResult success(Map<String, Object> outputData) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setSuccess(true);
        result.setOutputData(outputData);
        return result;
    }

    public static NodeExecutionResult error(String errorMessage) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static NodeExecutionResult waiting(String triggerType, String triggerKey) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setSuccess(true);
        result.setWaitingForTrigger(true);
        result.setTriggerType(triggerType);
        result.setTriggerKey(triggerKey);
        return result;
    }
}
