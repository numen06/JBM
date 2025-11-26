package com.jbm.cluster.job.business.impl;

import cn.hutool.core.lang.Assert;
import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.entitys.job.rule.ProcessTrigger;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.cluster.api.constants.job.ProcessStatusEnum;
import com.jbm.cluster.api.model.job.rule.*;
import com.jbm.cluster.job.execute.*;
import com.jbm.cluster.job.service.rule.NodeExecutionService;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.cluster.job.service.rule.ProcessTriggerService;
import com.jbm.cluster.job.service.rule.RuleDefinitionService;
import com.jbm.cluster.job.util.JsonUtils;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author scolin
 * @description 流程引擎执行
 * @date 2025/10/22 11:17
 */
@Component
@Slf4j
public class ProcessExecutionEngine {
    @Resource
    private RuleDefinitionService ruleDefinitionService;

    @Resource
    private ProcessInstanceService processInstanceService;

    @Resource
    private NodeExecutionService nodeExecutionService;

    @Resource
    private ProcessTriggerService processTriggerService;

    @Resource
    private NodeExecutorFactory nodeExecutorFactory;

    @Resource
    private DroolsRuleEngine droolsRuleEngine;

    @Resource
    private MQTTService mqttService;

    private final Map<String, ProcessInstance> runningInstances = new ConcurrentHashMap<>();

    /**
     * 执行流程
     */
    public ExecuteProcessResponse executeProcess(ExecuteProcessRequest request) {
        try {
            RuleDefinition ruleDefinition = ruleDefinitionService.selectById(request.getRuleDefinitionId());
            Assert.notNull(ruleDefinition, () -> new ServiceException("流程定义不存在"));

            // 创建流程实例
            ProcessInstance processInstance = createProcessInstance(ruleDefinition, request);

            // 解析流程数据
            FlowData flowData = parseFlowData(ruleDefinition.getRuleContent());

            // 开始执行
            return executeFromStart(processInstance, flowData, request.getInputParams());

        } catch (Exception e) {
            log.error("执行流程失败", e);
            throw new ServiceException("流程执行失败: " + e.getMessage());
        }
    }

    /**
     * 直接执行流程JSON（不使用本地规则定义）
     */
    public ExecuteProcessResponse executeProcessByJson(ExecuteProcessByJsonRequest request) {
        try {
            Assert.notBlank(request.getRuleContent(), () -> new ServiceException("流程定义JSON不能为空"));
            Assert.notNull(request.getInputParams(), () -> new ServiceException("输入参数不能为空"));

            // 根据输入参数创建流程实例
            ProcessInstance processInstance = new ProcessInstance();
            processInstance.setId(request.getProcessInstanceId() != null ? request.getProcessInstanceId()
                    : UUID.randomUUID().toString());
            processInstance.setRuleDefinitionId(null); // 直接执行流程不需要引用定义ID
            // 设置ruleName和ruleContent
            processInstance.setRuleName(request.getRuleName());
            processInstance.setRuleContent(request.getRuleContent());
            processInstance.setStatus(ProcessStatusEnum.RUNNING.getCode());
            processInstance.setInputParams(JsonUtils.toJson(request.getInputParams()));
            processInstance.setCreatedAt(LocalDateTime.now());
            processInstanceService.saveEntity(processInstance);

            // 解析流程数据
            FlowData flowData = parseFlowData(request.getRuleContent());

            // 开始执行
            return executeFromStart(processInstance, flowData, request.getInputParams());

        } catch (Exception e) {
            log.error("直接执行流程JSON失败", e);
            throw new ServiceException("流程执行失败: " + e.getMessage());
        }
    }

    /**
     * 继续执行等待触发的流程
     */
    public ExecuteProcessResponse continueProcess(ExecuteProcessRequest request) {
        ProcessInstance processInstance = processInstanceService.selectById(request.getProcessInstanceId());
        Assert.notNull(processInstance, () -> new ServiceException("流程实例不存在"));

        if (!ProcessStatusEnum.WAITING.getCode().equals(processInstance.getStatus())) {
            throw new ServiceException("流程实例不在等待状态");
        }

        // 更新触发器
        ProcessTrigger trigger = processTriggerService.findByProcessInstanceIdAndNodeId(
                request.getProcessInstanceId(), request.getTriggerNodeId());

        if (trigger == null) {
            throw new ServiceException("未找到对应的触发器");
        }

        trigger.setStatus(ProcessStatusEnum.TRIGGERED.getCode());
        trigger.setTriggerData(request.getTriggerData());
        trigger.setTriggeredAt(LocalDateTime.now());
        processTriggerService.saveEntity(trigger);

        // 继续执行
        FlowData flowData = parseFlowData(
                ruleDefinitionService.selectById(processInstance.getRuleDefinitionId()).getRuleContent());

        return continueExecution(processInstance, flowData, request.getTriggerNodeId(), request.getTriggerData());
    }

    private ExecuteProcessResponse executeFromStart(ProcessInstance processInstance,
            FlowData flowData,
            Map<String, Object> inputParams) {
        // 找到开始节点
        NodeData startNode = flowData.getNodes().stream()
                .filter(node -> "start".equals(node.getType()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到开始节点"));

        return executeNode(processInstance, flowData, startNode, inputParams);
    }

    private ExecuteProcessResponse continueExecution(ProcessInstance processInstance,
            FlowData flowData,
            String currentNodeId,
            String triggerData) {
        NodeData currentNode = findNodeById(flowData, currentNodeId);
        NodeExecution currentExecution = nodeExecutionService.findByProcessInstanceIdAndNodeId(
                processInstance.getId(), currentNodeId);

        // 更新节点执行记录
        currentExecution.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        currentExecution.setOutputData(JsonUtils.toJson(triggerData));
        currentExecution.setCompletedAt(LocalDateTime.now());
        nodeExecutionService.saveEntity(currentExecution);

        // triggerData 转map
        Map<String, Object> triggerDataMap = JsonUtils.fromJson(triggerData, Map.class);

        // 继续执行后续节点
        return executeNextNodes(processInstance, flowData, currentNode, triggerDataMap);
    }

    private ExecuteProcessResponse executeNode(ProcessInstance processInstance,
            FlowData flowData,
            NodeData currentNode,
            Map<String, Object> inputData) {
        // 创建节点执行记录
        NodeExecution nodeExecution = createNodeExecution(processInstance, currentNode, inputData);

        try {
            // 获取节点执行器
            NodeExecutor executor = nodeExecutorFactory.getExecutor(currentNode.getType());

            // 执行节点
            NodeExecutionResult result = executor.execute(currentNode, inputData);

            // 更新执行记录
            updateNodeExecution(nodeExecution, result);

            if (result.isWaitingForTrigger()) {
                // 节点需要等待触发
                createProcessTrigger(processInstance, currentNode, result);
                processInstance.setStatus(ProcessStatusEnum.WAITING.getCode());
                processInstanceService.save(processInstance);

                return createResponse(processInstance, currentNode, result, true);
            }

            if (result.isSuccess()) {
                // 执行后续节点
                return executeNextNodes(processInstance, flowData, currentNode, result.getOutputData());
            } else {
                // 执行失败
                processInstance.setStatus(ProcessStatusEnum.FAILED.getCode());
                processInstanceService.saveEntity(processInstance);
                return createResponse(processInstance, currentNode, result, false);
            }

        } catch (Exception e) {
            // 处理执行异常
            handleNodeExecutionError(nodeExecution, e);
            processInstance.setStatus(ProcessStatusEnum.FAILED.getCode());
            processInstanceService.saveEntity(processInstance);
            throw new ServiceException("节点执行失败: " + e.getMessage(), e);
        }
    }

    private ExecuteProcessResponse executeNextNodes(ProcessInstance processInstance,
            FlowData flowData,
            NodeData currentNode,
            Map<String, Object> outputData) {
        // 找到当前节点的所有出边
        List<EdgeData> outgoingEdges = flowData.getEdges().stream()
                .filter(edge -> currentNode.getId().equals(edge.getSource()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            // 没有后续节点，流程结束
            processInstance.setStatus(ProcessStatusEnum.COMPLETED.getCode());
            processInstance.setOutputParams(JsonUtils.toJson(outputData));
            processInstanceService.saveEntity(processInstance);

            return createResponse(processInstance, currentNode,
                    NodeExecutionResult.success(outputData), false);
        }

        // 根据节点类型决定下一步
        if ("conditions".equals(currentNode.getType())) {
            return executeConditionNode(processInstance, flowData, currentNode, outputData, outgoingEdges);
        } else {
            // 普通节点，执行第一个后续节点
            EdgeData nextEdge = outgoingEdges.get(0);
            NodeData nextNode = findNodeById(flowData, nextEdge.getTarget());

            if ("end".equals(nextNode.getType())) {
                // 到达结束节点
                processInstance.setStatus(ProcessStatusEnum.COMPLETED.getCode());
                processInstance.setOutputParams(JsonUtils.toJson(outputData));
                processInstanceService.saveEntity(processInstance);
            }

            return executeNode(processInstance, flowData, nextNode, outputData);
        }
    }

    private ExecuteProcessResponse executeConditionNode(ProcessInstance processInstance,
            FlowData flowData,
            NodeData conditionNode,
            Map<String, Object> inputData,
            List<EdgeData> outgoingEdges) {
        // 使用Drools规则引擎判断分支
        String ruleName = droolsRuleEngine.evaluateCondition(conditionNode, inputData);

        // 找到对应的边
        EdgeData selectedEdge = outgoingEdges.stream()
                .filter(edge -> {
                    String sourceHandle = edge.getSourceHandle();
                    return sourceHandle != null && sourceHandle.contains(ruleName);
                })
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到匹配的分支"));

        // 执行选中的分支
        NodeData nextNode = findNodeById(flowData, selectedEdge.getTarget());
        return executeNode(processInstance, flowData, nextNode, inputData);
    }

    // 辅助方法
    private ProcessInstance createProcessInstance(RuleDefinition ruleDefinition, ExecuteProcessRequest request) {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(UUID.randomUUID().toString());
        instance.setRuleDefinitionId(ruleDefinition.getId());
        // 设置规则名称和内容
        instance.setRuleName(ruleDefinition.getRuleName());
        instance.setRuleContent(ruleDefinition.getRuleContent());
        instance.setStatus(ProcessStatusEnum.RUNNING.getCode());
        instance.setInputParams(JsonUtils.toJson(request.getInputParams()));
        instance.setCreatedAt(LocalDateTime.now());
        return processInstanceService.saveEntity(instance);
    }

    private NodeExecution createNodeExecution(ProcessInstance processInstance,
            NodeData node,
            Map<String, Object> inputData) {
        NodeExecution execution = new NodeExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setProcessInstanceId(processInstance.getId());
        execution.setNodeId(node.getId());
        execution.setNodeType(node.getType());
        execution.setStatus(ProcessStatusEnum.RUNNING.getCode());
        execution.setInputData(JsonUtils.toJson(inputData));
        execution.setStartedAt(LocalDateTime.now());
        return nodeExecutionService.saveEntity(execution);
    }

    private void updateNodeExecution(NodeExecution execution, NodeExecutionResult result) {
        execution.setStatus(
                result.isSuccess() ? ProcessStatusEnum.COMPLETED.getCode() : ProcessStatusEnum.FAILED.getCode());
        execution.setOutputData(JsonUtils.toJson(result.getOutputData()));
        execution.setErrorMessage(result.getErrorMessage());
        execution.setCompletedAt(LocalDateTime.now());
        nodeExecutionService.saveEntity(execution);
    }

    private void createProcessTrigger(ProcessInstance processInstance,
            NodeData node,
            NodeExecutionResult result) {
        ProcessTrigger trigger = new ProcessTrigger();
        trigger.setId(UUID.randomUUID().toString());
        trigger.setProcessInstanceId(processInstance.getId());
        trigger.setNodeId(node.getId());
        trigger.setTriggerType(result.getTriggerType());
        trigger.setTriggerKey(result.getTriggerKey());
        trigger.setStatus(ProcessStatusEnum.WAITING.getCode());
        trigger.setCreatedAt(LocalDateTime.now());
        processTriggerService.saveEntity(trigger);
    }

    private NodeData findNodeById(FlowData flowData, String nodeId) {
        return flowData.getNodes().stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到节点: " + nodeId));
    }

    private FlowData parseFlowData(String flowDataJson) {
        return JsonUtils.fromJson(flowDataJson, FlowData.class);
    }

    private ExecuteProcessResponse createResponse(ProcessInstance processInstance,
            NodeData currentNode,
            NodeExecutionResult result,
            boolean waiting) {
        ExecuteProcessResponse response = new ExecuteProcessResponse();
        response.setProcessInstanceId(processInstance.getId());
        response.setStatus(processInstance.getStatus());
        response.setCurrentNodeId(currentNode.getId());
        response.setWaitingForTrigger(waiting);
        response.setMessage(result.getMessage());

        if (processInstance.getOutputParams() != null) {
            response.setOutputParams(JsonUtils.fromJson(processInstance.getOutputParams(), Map.class));
        } else {
            response.setOutputParams(result.getOutputData());
        }

        return response;
    }

    private void handleNodeExecutionError(NodeExecution execution, Exception e) {
        execution.setStatus(ProcessStatusEnum.FAILED.getCode());
        execution.setErrorMessage(e.getMessage());
        execution.setCompletedAt(LocalDateTime.now());
        nodeExecutionService.saveEntity(execution);
    }
}
