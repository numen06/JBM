package com.jbm.cluster.job.service.rule;

import com.jbm.cluster.api.entitys.job.rule.ProcessInstance;
import com.jbm.cluster.api.entitys.job.rule.RuleDefinition;
import com.jbm.framework.masterdata.service.IMasterDataService;

import com.jbm.cluster.api.model.job.rule.RuleInstanceModel;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;

import java.util.List;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:22
 */
public interface ProcessInstanceService extends IMasterDataService<ProcessInstance> {
    /**
     * 分页查询流程实例（包含关联的规则定义和节点执行信息）
     *
     * @param ruleDefinitionId 规则定义ID，可为空表示查询所有
     * @param status           流程状态，可为空表示查询所有
     * @param pageForm         分页参数
     * @return 分页数据
     */
    DataPaging<RuleInstanceModel> pageQueryProcessInstances(Long ruleDefinitionId, String status, PageForm pageForm);

    /**
     * 根据ID查询流程实例（包含关联的规则定义和节点执行信息）
     *
     * @param id 流程实例ID
     * @return 流程实例详情
     */
    RuleInstanceModel getProcessInstanceById(String id);
}
