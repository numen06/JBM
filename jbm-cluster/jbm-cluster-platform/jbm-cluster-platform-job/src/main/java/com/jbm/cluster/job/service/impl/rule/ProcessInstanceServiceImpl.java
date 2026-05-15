
package com.jbm.cluster.job.service.impl.rule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.entitys.job.rule.NodeExecution;
import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.cluster.api.model.job.rule.RuleInstanceModel;
import com.jbm.cluster.job.mapper.ProcessInstanceMapper;
import com.jbm.cluster.job.service.rule.NodeExecutionService;
import com.jbm.cluster.job.service.rule.ProcessInstanceService;
import com.jbm.cluster.job.service.rule.RuleDefinitionService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.masterdata.usage.PageParams;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 15:56
 */
@Slf4j
@Service
public class ProcessInstanceServiceImpl extends MasterDataServiceImpl<ProcessInstance>
        implements ProcessInstanceService {
    @Autowired
    private RuleDefinitionService ruleDefinitionService;

    @Autowired
    private NodeExecutionService nodeExecutionService;

    @Override
    public DataPaging<RuleInstanceModel> pageQueryProcessInstances(Long ruleDefinitionId, String status,
            PageForm pageForm) {
        // 构建查询条件
        ProcessInstance queryEntity = new ProcessInstance();
        if (ruleDefinitionId != null) {
            queryEntity.setRuleDefinitionId(ruleDefinitionId);
        }
        if (status != null && !status.isEmpty()) {
            queryEntity.setStatus(status);
        }

        // 使用ServiceUtils进行分页查询，避免框架的selectEntitys(CriteriaQueryWrapper)方法的bug
        CriteriaQueryWrapper<ProcessInstance> criteriaQueryWrapper = ServiceUtils.toCriteriaQueryWrapper(queryEntity,
                pageForm);
        PageParams pageParams = criteriaQueryWrapper.getPageParams();
        IPage<ProcessInstance> pageData = this.baseMapper.selectPage(
                (com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProcessInstance>) pageParams,
                criteriaQueryWrapper);
        DataPaging<ProcessInstance> processInstancePageData = ServiceUtils.pageToDataPaging(pageData);

        // 转换为RuleInstanceModel（使用批量查询优化）
        List<ProcessInstance> processInstances = processInstancePageData.getContents();
        List<RuleInstanceModel> ruleInstanceModels = convertToRuleInstanceModels(processInstances);

        // 返回分页数据
        return new DataPaging<>(ruleInstanceModels, processInstancePageData);
    }

    /**
     * 批量将ProcessInstance转换为RuleInstanceModel，避免N+1查询
     */
    private List<RuleInstanceModel> convertToRuleInstanceModels(List<ProcessInstance> processInstances) {
        if (processInstances == null || processInstances.isEmpty()) {
            return new ArrayList<>();
        }

        // 第一步：批量获取所有的RuleDefinitionId
        List<Long> ruleDefinitionIds = new ArrayList<>();
        for (ProcessInstance pi : processInstances) {
            if (pi.getRuleDefinitionId() != null) {
                ruleDefinitionIds.add(pi.getRuleDefinitionId());
            }
        }

        // 第二步：批量查询RuleDefinition，构建id->RuleDefinition的Map
        Map<Long, RuleDefinition> ruleDefinitionMap = new HashMap<>();
        if (!ruleDefinitionIds.isEmpty()) {
            List<RuleDefinition> ruleDefinitions = ruleDefinitionService.selectAllEntitys();
            for (RuleDefinition rd : ruleDefinitions) {
                ruleDefinitionMap.put(rd.getId(), rd);
            }
        }

        // 第三步：批量获取所有ProcessInstanceId
        List<String> processInstanceIds = new ArrayList<>();
        for (ProcessInstance pi : processInstances) {
            processInstanceIds.add(pi.getInstanceId());
        }

        // 第四步：批量查询NodeExecution，构建processInstanceId->NodeExecution列表的Map
        Map<String, List<NodeExecution>> nodeExecutionMap = new HashMap<>();
        if (!processInstanceIds.isEmpty()) {
            List<NodeExecution> allNodeExecutions = nodeExecutionService.selectAllEntitys();
            for (NodeExecution ne : allNodeExecutions) {
                nodeExecutionMap.computeIfAbsent(ne.getProcessInstanceId(), k -> new ArrayList<>()).add(ne);
            }
        }

        // 第五步：遍历ProcessInstance，使用Map中的数据进行映射
        List<RuleInstanceModel> ruleInstanceModels = new ArrayList<>();
        for (ProcessInstance processInstance : processInstances) {
            RuleInstanceModel model = new RuleInstanceModel();
            model.setId(processInstance.getInstanceId());
            model.setStatus(processInstance.getStatus());
            model.setInputParams(processInstance.getInputParams());
            model.setOutputParams(processInstance.getOutputParams());
            model.setCreatedAt(
                    processInstance.getCreatedAt() != null ? processInstance.getCreatedAt().toString() : null);
            model.setUpdatedAt(
                    processInstance.getUpdatedAt() != null ? processInstance.getUpdatedAt().toString() : null);

            // 从Map中获取RuleDefinition
            RuleDefinition ruleDefinition = ruleDefinitionMap.get(processInstance.getRuleDefinitionId());

            // 从Map中获取NodeExecution列表
            List<NodeExecution> nodeExecutions = nodeExecutionMap.getOrDefault(processInstance.getInstanceId(),
                    new ArrayList<>());

            if (ruleDefinition != null) {
                model.setRuleId(ruleDefinition.getId());
                model.setRuleName(ruleDefinition.getRuleName());
                model.setRuleCode(ruleDefinition.getRuleCode());
                // 增强 ruleContent，将 nodeExecutions 的 status 信息拼接进去
                String enhancedRuleContent = enhanceRuleContent(ruleDefinition.getRuleContent(), nodeExecutions);
                model.setRuleContent(enhancedRuleContent);
            } else {
                // ruleDefinition 为 null 时，不使用本地规则，直接从 ProcessInstance 中取值
                model.setRuleName(processInstance.getRuleName());
                // 增强 ruleContent
                String enhancedRuleContent = enhanceRuleContent(processInstance.getRuleContent(), nodeExecutions);
                model.setRuleContent(enhancedRuleContent);
            }

            model.setNodeExecutions(nodeExecutions);
            ruleInstanceModels.add(model);
        }

        return ruleInstanceModels;
    }

    @Override
    public RuleInstanceModel getProcessInstanceById(String id) {
        ProcessInstance processInstance = super.selectById(id);
        if (processInstance == null) {
            return null;
        }
        // 使用批量转换方法
        return convertToRuleInstanceModels(java.util.Arrays.asList(processInstance)).stream().findFirst().orElse(null);
    }

    /**
     * 增强ruleContent，将nodeExecutions的信息拼接到ruleContent中
     * 1.
     * 绑定到nodes中，根据nodeId匹配，拼接status、inputData、outputData、startedAt、completedAt、errorMessage
     * 2. 绑定到edges中，根据target匹配nodeId，拼接status
     */
    private String enhanceRuleContent(String ruleContent, List<NodeExecution> nodeExecutions) {
        if (ruleContent == null || nodeExecutions == null || nodeExecutions.isEmpty()) {
            return ruleContent;
        }

        try {
            // 解析ruleContent JSON
            JSONObject ruleContentObj = JSONUtil.parseObj(ruleContent);

            // 构建 nodeId -> NodeExecution 的 Map
            Map<String, NodeExecution> nodeIdExecutionMap = new HashMap<>();
            for (NodeExecution ne : nodeExecutions) {
                nodeIdExecutionMap.put(ne.getNodeId(), ne);
            }

            // 处理 nodes 数组
            JSONArray nodesArray = ruleContentObj.getJSONArray("nodes");
            if (nodesArray != null && !nodesArray.isEmpty()) {
                for (int i = 0; i < nodesArray.size(); i++) {
                    JSONObject node = nodesArray.getJSONObject(i);
                    String nodeId = node.getStr("id");
                    NodeExecution nodeExecution = nodeIdExecutionMap.get(nodeId);

                    // 获取或创建node的data对象
                    JSONObject nodeData = null;
                    if (node.get("data") != null) {
                        nodeData = JSONUtil.parseObj(node.get("data"));
                    } else {
                        nodeData = new JSONObject();
                    }

                    // 如果找到对应的执行记录，拼接所有字段
                    if (nodeExecution != null) {
                        nodeData.set("status", nodeExecution.getStatus());
                        nodeData.set("inputData", nodeExecution.getInputData());
                        nodeData.set("outputData", nodeExecution.getOutputData());
                        nodeData.set("startedAt",
                                nodeExecution.getStartedAt() != null ? nodeExecution.getStartedAt().toString() : null);
                        nodeData.set("completedAt",
                                nodeExecution.getCompletedAt() != null ? nodeExecution.getCompletedAt().toString()
                                        : null);
                        nodeData.set("errorMessage", nodeExecution.getErrorMessage());
                    } else {
                        // 如果没有找到执行记录，设置这些字段为空
                        nodeData.set("status", null);
                        nodeData.set("inputData", null);
                        nodeData.set("outputData", null);
                        nodeData.set("startedAt", null);
                        nodeData.set("completedAt", null);
                        nodeData.set("errorMessage", null);
                    }
                    node.set("data", nodeData);
                }
            }

            // 处理 edges 数组
            JSONArray edgesArray = ruleContentObj.getJSONArray("edges");
            if (edgesArray != null && !edgesArray.isEmpty()) {
                for (int i = 0; i < edgesArray.size(); i++) {
                    JSONObject edge = edgesArray.getJSONObject(i);
                    String target = edge.getStr("target");
                    NodeExecution nodeExecution = nodeIdExecutionMap.get(target);
                    JSONObject data = edge.getJSONObject("data");
                    // 拼接status字段
                    if (nodeExecution != null) {
                        data.set("status", nodeExecution.getStatus());
                    } else {
                        data.set("status", null);
                    }
                }
            }

            // 转换回字符串
            return ruleContentObj.toString();
        } catch (Exception e) {
            log.error("增强ruleContent失败", e);
            return ruleContent;
        }
    }
}