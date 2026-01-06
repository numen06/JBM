package com.jbm.cluster.job.business.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.entitys.job.rule.ProcessTrigger;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.cluster.api.constants.job.ProcessStatusEnum;
import com.jbm.cluster.api.model.job.rule.*;
import com.jbm.cluster.job.execute.*;
import com.jbm.cluster.job.model.NodeExecutionMessage;
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
import java.util.*;
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
     * 创建流程实例（传入流程json）
     */
    public ProcessInstance createProcessByJson(ExecuteProcessByJsonRequest request) {
        try {
            Assert.notBlank(request.getRuleContent(), () -> new ServiceException("流程定义JSON不能为空"));
            //Assert.notNull(request.getInputParams(), () -> new ServiceException("输入参数不能为空"));

            // 根据输入参数创建流程实例
            ProcessInstance processInstance = new ProcessInstance();
            processInstance.setId(request.getProcessInstanceId() != null ? request.getProcessInstanceId()
                    : UUID.randomUUID().toString());
            processInstance.setRuleDefinitionId(null); // 直接执行流程不需要引用定义ID
            // 设置ruleName和ruleContent
            processInstance.setRuleName(request.getRuleName());
            processInstance.setRuleContent(request.getRuleContent());
            processInstance.setStatus(ProcessStatusEnum.CREATED.getCode());
            processInstance.setInputParams(JsonUtils.toJson(request.getInputParams()));
            processInstance.setCreatedAt(LocalDateTime.now());
            return processInstanceService.saveEntity(processInstance);
        } catch (Exception e) {
            throw new ServiceException("生成流程实例失败: " + e.getMessage());
        }
    }


    /**
     * 直接执行流程JSON（不使用本地规则定义）
     */
    public ExecuteProcessResponse executeProcessByJson(ExecuteProcessByJsonRequest request) {
        try {
            Assert.notNull(request.getInputParams(), () -> new ServiceException("processInstanceId不能为空"));
            Assert.notNull(request.getInputParams(), () -> new ServiceException("inputParams不能为空"));

            ProcessInstance processInstance = processInstanceService.selectById(request.getProcessInstanceId());
            Assert.notNull(processInstance, () -> new ServiceException("流程实例不存在"));

            // 解析流程数据
            FlowData flowData = parseFlowData(processInstance.getRuleContent());

            // 开始执行
            processInstance.setStatus(ProcessStatusEnum.RUNNING.getCode());
            return executeFromStart(processInstance, flowData, request.getInputParams());

        } catch (Exception e) {
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
        // 检查是使用本地规则定义还是直接使用流程实例中的JSON内容
        String ruleContent;
        if (processInstance.getRuleDefinitionId() != null) {
            // 使用本地规则定义
            ruleContent = ruleDefinitionService.selectById(processInstance.getRuleDefinitionId()).getRuleContent();
        } else {
            // 使用流程实例中直接存储的JSON内容
            ruleContent = processInstance.getRuleContent();
        }
        FlowData flowData = parseFlowData(ruleContent);

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

        // 1. 恢复之前的输入参数作为基础上下文
        Map<String, Object> executionData = new HashMap<>();
        if (currentExecution.getInputData() != null) {
            Map<String, Object> previousInput = JsonUtils.fromJson(currentExecution.getInputData(), Map.class);
            if (previousInput != null) {
                executionData.putAll(previousInput);
            }
        }

        // 2. 合并当前的触发参数到上下文中
        Map<String, Object> triggerDataMap = JsonUtils.fromJson(triggerData, Map.class);
        if (triggerDataMap != null) {
            executionData.putAll(triggerDataMap);
        }

        if(Objects.equals(currentExecution.getStatus(), ProcessStatusEnum.WAITING.getCode())){
            // 获取节点执行器
            NodeExecutor executor = nodeExecutorFactory.getExecutor(currentNode.getType());

            executionData.put("currentNodeStatus", currentExecution.getStatus());
            // 自动查询并注入下一个站点信息
            injectNextSiteInfo(flowData, currentNode, executionData);

            NodeExecutionResult result = executor.execute(currentNode, executionData);

            // 3. 执行成功则更新上下文为执行后的输出
            if (result.isSuccess()) {
                executionData = result.getOutputData();
            }

            // 执行完移除辅助字段，避免污染后续节点
            executionData.remove("currentNodeStatus");
            executionData.remove("nextSite");
            executionData.remove("nextSiteList");
        }
        // 更新节点执行记录
        currentExecution.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        currentExecution.setOutputData(JsonUtils.toJson(executionData));
        currentExecution.setCompletedAt(LocalDateTime.now());
        nodeExecutionService.saveEntity(currentExecution);
        // 继续执行后续节点，传入完整的上下文
        return executeNextNodes(processInstance, flowData, currentNode, executionData);
    }

    /**
     * 在触发数据中自动注入下一个站点的信息
     * 支持两种情况：
     * 1. 普通节点：注入nextSite（下一个站点）
     * 2. 条件分支：注入nextSiteList（所有可能的下一站点）
     */
    private void injectNextSiteInfo(FlowData flowData, NodeData currentNode, Map<String, Object> triggerDataMap) {
        List<EdgeData> outgoingEdges = flowData.getEdges().stream()
                .filter(edge -> currentNode.getId().equals(edge.getSource()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            return;  // 没有后续节点
        }

        if ("conditions".equals(currentNode.getType())) {
            // 条件分支或普通节点都有多个后继节点時，注入所有可能的下一站点列表
            List<Map<String, Object>> nextSiteList = new ArrayList<>();
            for (EdgeData edge : outgoingEdges) {
                NodeData nextNode = findNodeById(flowData, edge.getTarget());
                if ("station".equals(nextNode.getType()) && nextNode.getData() != null) {
                    Map<String, Object> siteInfo = extractSiteInfo(nextNode);
                    if (siteInfo != null) {
                        nextSiteList.add(siteInfo);
                    }
                }
            }
            if (!nextSiteList.isEmpty()) {
                triggerDataMap.put("nextSiteList", nextSiteList);
            }
        } else {
            // 普通节点：如果有多个后继节点，注入所有站点；否则注入单个站点
            if (outgoingEdges.size() > 1) {
                // 有多个后继节点，注入 nextSiteList
                List<Map<String, Object>> nextSiteList = new ArrayList<>();
                for (EdgeData edge : outgoingEdges) {
                    NodeData nextNode = findNodeById(flowData, edge.getTarget());
                    if ("station".equals(nextNode.getType()) && nextNode.getData() != null) {
                        Map<String, Object> siteInfo = extractSiteInfo(nextNode);
                        if (siteInfo != null) {
                            nextSiteList.add(siteInfo);
                        }
                    }
                }
                if (!nextSiteList.isEmpty()) {
                    triggerDataMap.put("nextSiteList", nextSiteList);
                }
            } else {
                // 只有一个后继节点，注入 nextSite
                EdgeData nextEdge = outgoingEdges.get(0);
                NodeData nextNode = findNodeById(flowData, nextEdge.getTarget());
                if ("station".equals(nextNode.getType()) && nextNode.getData() != null) {
                    Map<String, Object> nextSiteInfo = extractSiteInfo(nextNode);
                    if (nextSiteInfo != null) {
                        triggerDataMap.put("nextSite", nextSiteInfo);
                    }
                }
            }
        }
    }

    /**
     * 从节点中提取站点信息（动态获取所有字段）
     * 直接复制site对象中的所有字段到siteInfo中，避免硬编码字段列表
     */
    private Map<String, Object> extractSiteInfo(NodeData nodeData) {
        try {
            Map<String, Object> nodeData_map = (Map<String, Object>) nodeData.getData();
            if (nodeData_map != null && nodeData_map.containsKey("site")) {
                Map<String, Object> site = (Map<String, Object>) nodeData_map.get("site");
                if (site != null) {
                    // 动态复制site中的所有字段，实现自动适配新增字段
                    Map<String, Object> siteInfo = new HashMap<>(site);
                    return siteInfo;
                }
            }
        } catch (Exception e) {
            log.warn("提取站点信息失败: {}", e.getMessage());
        }
        return null;
    }

    private ExecuteProcessResponse executeNode(ProcessInstance processInstance,
            FlowData flowData,
            NodeData currentNode,
            Map<String, Object> inputData) {
        // ... existing code ...
        // 先检查该节点是否已有执行记录，避免重复创建
        NodeExecution nodeExecution = nodeExecutionService.findByProcessInstanceIdAndNodeId(
                processInstance.getId(), currentNode.getId());
        
        // 如果该节点已经有执行记录，说明是重复执行，直接返回而不是创建新记录
        if (nodeExecution != null) {
            log.warn("节点 {} 已有执行记录，避免重复执行", currentNode.getId());
            // 已经执行过了，不再重复执行
            return executeNextNodes(processInstance, flowData, currentNode, inputData);
        }
        
        // 创建新的节点执行记录
        nodeExecution = createNodeExecution(processInstance, currentNode, inputData);
        
        // 发送MQTT消息 - 进入节点
        sendNodeExecutionMessage(processInstance, currentNode, inputData, "RUNNING", "ENTER");

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
                processInstanceService.saveOrUpdate(processInstance);
                
                // 发送节点等待触发消息 - 离开节点
                sendNodeExecutionMessage(processInstance, currentNode, inputData, "WAITING", "EXIT");

                return createResponse(processInstance, currentNode, result, true);
            }

            if (result.isSuccess()) {
                // 发送节点执行成功消息 - 离开节点
                sendNodeExecutionMessage(processInstance, currentNode, result.getOutputData(), "COMPLETED", "EXIT");
                
                // 执行后续节点
                return executeNextNodes(processInstance, flowData, currentNode, result.getOutputData());
            } else {
                // 执行失败
                processInstance.setStatus(ProcessStatusEnum.FAILED.getCode());
                processInstanceService.saveEntity(processInstance);
                
                // 发送节点执行失败消息 - 离开节点
                sendNodeExecutionMessage(processInstance, currentNode, inputData, "FAILED", "EXIT");
                
                return createResponse(processInstance, currentNode, result, false);
            }

        } catch (Exception e) {
            // 处理执行异常
            handleNodeExecutionError(nodeExecution, e);
            processInstance.setStatus(ProcessStatusEnum.FAILED.getCode());
            processInstanceService.saveEntity(processInstance);
            
            // 发送节点执行异常消息 - 离开节点
            sendNodeExecutionMessage(processInstance, currentNode, inputData, "ERROR", "EXIT");
            
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
            // 普通节点：如果有多个后继节点，需要由后续条件节点来选择
            // nextSiteId 是 siteCoordinateId，无法与节点ID关联，所以简化为取第一条边
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
    
    /**
     * 选择下一条边
     * 如果只有一条边，直接返回；
     * 如果有多条边，根据 outputData 中的 nextSiteId 来选择对应的站点边
     */
    private EdgeData selectNextEdge(List<EdgeData> outgoingEdges, Map<String, Object> outputData) {
        if (outgoingEdges.size() <= 1) {
            return outgoingEdges.get(0);
        }
        
        // 如果有多条边，尝试根据 nextSiteId 来选择
        Object nextSiteId = outputData.get("nextSiteId");
        if (nextSiteId == null) {
            // 如果没有 nextSiteId，返回第一条边
            return outgoingEdges.get(0);
        }
        
        String targetSiteId = nextSiteId.toString();
        // 众上会先提前设置 flowData 的引用，但这里没有。根据目标节点的站点ID来匹配是最佳方案
        return outgoingEdges.get(0);
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

        if(result.isSuccess()){
            if(result.isWaitingForTrigger()){
                execution.setStatus(ProcessStatusEnum.WAITING.getCode());
            }else {
                execution.setStatus(ProcessStatusEnum.COMPLETED.getCode());
            }
        }else {
            execution.setStatus(ProcessStatusEnum.FAILED.getCode());
        }

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

    public FlowData parseFlowData(String flowDataJson) {
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

    /**
     * 发送节点执行消息
     *
     * @param processInstance 流程实例
     * @param nodeData        节点数据
     * @param inputData       输入数据
     * @param status          状态
     * @param eventType       事件类型 ENTER-进入节点，EXIT-离开节点
     */
    private void sendNodeExecutionMessage(ProcessInstance processInstance, NodeData nodeData, Map<String, Object> inputData, String status, String eventType) {
        try {
            NodeExecutionMessage message = new NodeExecutionMessage();
            message.setProcessInstanceId(processInstance.getId());
            message.setProcessInstanceName(processInstance.getRuleName());
            message.setNodeId(nodeData.getId());
            message.setNodeType(nodeData.getType());
            message.setNodeLabel(nodeData.getLabel());
            message.setNodeData(nodeData.getData());
            message.setInputParams(inputData);
            message.setExecutionTime(LocalDateTime.now());
            message.setStatus(status);
            message.setEventType(eventType);

            //String messageJson = JsonUtils.toJson(message);
            String topic = "process/node/execution/";
            
            // 发送MQTT消息
            mqttService.publish(topic, message);
        } catch (Exception e) {
            log.error("发送节点执行消息失败", e);
        }
    }
}
