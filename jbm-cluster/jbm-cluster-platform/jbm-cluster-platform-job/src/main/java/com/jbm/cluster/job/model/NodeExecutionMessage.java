package com.jbm.cluster.job.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author scolin
 * @description 节点执行消息
 * @date 2025/12/3 15:30
 */
@Data
public class NodeExecutionMessage {
    /**
     * 流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 流程实例名称
     */
    private String processInstanceName;
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 节点类型
     */
    private String nodeType;
    
    /**
     * 节点标签
     */
    private String nodeLabel;
    
    /**
     * 节点数据
     */
    private Map<String, Object> nodeData;
    
    /**
     * 输入参数
     */
    private Map<String, Object> inputParams;
    
    /**
     * 执行时间
     */
    private LocalDateTime executionTime;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 节点事件类型：ENTER-进入节点，EXIT-离开节点
     */
    private String eventType;
    
    /**
     * 是否存在结束节点
     */
    private Boolean hasEndNode;
}