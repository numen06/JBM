
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

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

        // 转换为RuleInstanceModel
        List<RuleInstanceModel> ruleInstanceModels = new ArrayList<>();
        for (ProcessInstance processInstance : processInstancePageData.getContents()) {
            RuleInstanceModel model = convertToRuleInstanceModel(processInstance);
            ruleInstanceModels.add(model);
        }

        // 返回分页数据
        return new DataPaging<>(ruleInstanceModels, processInstancePageData);
    }

    @Override
    public RuleInstanceModel getProcessInstanceById(String id) {
        ProcessInstance processInstance = super.selectById(id);
        if (processInstance == null) {
            return null;
        }
        return convertToRuleInstanceModel(processInstance);
    }

    /**
     * 将ProcessInstance转换为RuleInstanceModel
     */
    private RuleInstanceModel convertToRuleInstanceModel(ProcessInstance processInstance) {
        RuleInstanceModel model = new RuleInstanceModel();
        model.setId(processInstance.getId());
        model.setStatus(processInstance.getStatus());
        model.setInputParams(processInstance.getInputParams());
        model.setOutputParams(processInstance.getOutputParams());
        model.setCreatedAt(processInstance.getCreatedAt() != null ? processInstance.getCreatedAt().toString() : null);
        model.setUpdatedAt(processInstance.getUpdatedAt() != null ? processInstance.getUpdatedAt().toString() : null);

        // 查询对应的RuleDefinition
        RuleDefinition ruleDefinition = ruleDefinitionService.selectById(processInstance.getRuleDefinitionId());
        if (ruleDefinition != null) {
            model.setRuleId(ruleDefinition.getId());
            model.setRuleName(ruleDefinition.getRuleName());
            model.setRuleCode(ruleDefinition.getRuleCode());
            model.setRuleContent(ruleDefinition.getRuleContent());
        }

        // 查询对应的NodeExecution列表
        NodeExecution nodeExecutionQuery = new NodeExecution();
        nodeExecutionQuery.setProcessInstanceId(processInstance.getId());
        List<NodeExecution> nodeExecutions = nodeExecutionService.selectEntitys(nodeExecutionQuery);
        model.setNodeExecutions(nodeExecutions);

        return model;
    }
}
